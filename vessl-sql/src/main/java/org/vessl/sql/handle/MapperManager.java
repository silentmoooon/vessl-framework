package org.vessl.sql.handle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.lang3.StringUtils;
import org.vessl.core.bean.BeanStore;
import org.vessl.sql.annotation.Sql;
import org.vessl.sql.bean.*;
import org.vessl.sql.constant.MethodReturnMode;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperManager {

    private static DataSource singleDataSource;
    private static boolean isSingleDataSource = true;
    private static Map<String, DataSource> dataSourceMap = new HashMap<>();
    /**
     * 根据mapper类缓存的mapper动态代理对象
     */
    private static final Map<Class<?>, Object> mapperProxyMap = new HashMap<>();

    private static final Map<Class<?>, String> mapperDatasourceMap = new HashMap<>();

    /**
     * 缓存类中方法定义
     */
    private static final Multimap<Class<?>, MethodDesc> classMethodMap = ArrayListMultimap.create();

    public static boolean isSingle() {
        return isSingleDataSource;
    }

    static DataSource getDataSource() {
        if (singleDataSource == null) {
            throw new RuntimeException("datasource is null");
        }
        return singleDataSource;
    }

    static DataSource getDataSource(Class<?> clazz) {
        if(isSingleDataSource){
            return singleDataSource;
        }
        DataSource dataSource = dataSourceMap.get(mapperDatasourceMap.get(clazz));
        if (dataSource == null) {
            throw new RuntimeException("datasource is null");
        }
        return dataSource;
    }

    static DataSource getDataSource(String datasourceName) {
        if(isSingleDataSource){
            return singleDataSource;
        }
        DataSource dataSource = dataSourceMap.get(datasourceName);
        if (dataSource == null) {
            throw new RuntimeException("datasource is null");
        }
        return dataSource;
    }


    static <T> void addMapper(Class<T> type) {

        Method[] methods = type.getMethods();
        for (Method method : methods) {
            Sql sqlAnnotation = method.getAnnotation(Sql.class);

            if (sqlAnnotation != null && StringUtils.isNoneEmpty(sqlAnnotation.value())) {
                SqlMethodBean methodBean = new SqlMethodBean();
                methodBean.setName(method.getName());
                methodBean.setType(sqlAnnotation.type().toString());
                methodBean.setSqlType(sqlAnnotation.type());
                methodBean.setSql(sqlAnnotation.value());
                MethodReturnMode methodReturnMode = getMethodReturnMode(method);
                methodBean.setReturnMode(methodReturnMode);
                methodBean.setReturnType(method.getGenericReturnType());
                classMethodMap.put(type, new MethodDesc(method, methodBean));

            }
        }

    }

    static void initMapperBean() {
        for (Class<?> aClass : classMethodMap.keySet()) {
            MapperProxy mapperProxy = new MapperProxy(aClass, classMethodMap.get(aClass).stream().toList());

            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(aClass);
            enhancer.setCallback(mapperProxy);
            Object o = enhancer.create();
            mapperProxyMap.put(aClass, o);

        }
        mapperProxyMap.forEach(BeanStore::addBean);
        classMethodMap.clear();

    }

    static void initDatasource(HikariConfig config) {
        singleDataSource = new HikariDataSource(config);

    }

    static void initDatasource(List<BaseSqlConfig> sqlConfigs) {
        isSingleDataSource = false;
        for (BaseSqlConfig sqlConfig : sqlConfigs) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(sqlConfig.getJdbcUrl());
            hikariConfig.setUsername(sqlConfig.getUsername());
            hikariConfig.setPassword(sqlConfig.getPassword());
            hikariConfig.addDataSourceProperty("cachePrepStmts", sqlConfig.getCachePrepStmts());
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", sqlConfig.getPrepStmtCacheSize());
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", sqlConfig.getPrepStmtCacheSqlLimit());
            DataSource dataSource = new HikariDataSource(hikariConfig);
            dataSourceMap.put(sqlConfig.getDatasourceName(), dataSource);

            for (Class<?> aClass : mapperProxyMap.keySet()) {
                if (aClass.getPackageName().startsWith(sqlConfig.getBasePackage())) {

                    mapperDatasourceMap.put(aClass, sqlConfig.getDatasourceName());
                }

            }

        }

    }

    static <T> T getMapper(Class<T> type) {
        return (T) mapperProxyMap.get(type);
    }

    public static Map<Class<?>, Object> getAllMapperObjects() {
        return mapperProxyMap;
    }

    static void addMapper(SqlClassBean bean) {
        Class<?> clazz = null;
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(bean.getName());
        } catch (ClassNotFoundException e) {
            return;
        }

        Map<String, SqlMethodBean> methodBeanMap = new HashMap<>();
        for (SqlMethodBean method : bean.getMethods()) {
            methodBeanMap.put(method.getName(), method);
        }
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getAnnotation(Sql.class) == null && methodBeanMap.containsKey(method.getName())) {
                SqlMethodBean methodBean = methodBeanMap.get(method.getName());
                MethodReturnMode methodReturnMode = getMethodReturnMode(method);
                methodBean.setReturnMode(methodReturnMode);
                methodBean.setReturnType(method.getGenericReturnType());
                classMethodMap.put(clazz, new MethodDesc(method, methodBean));
            }
        }

    }

    private static MethodReturnMode getMethodReturnMode(Method method) {
        Class<?> returnType = method.getReturnType();
        if (Collection.class.isAssignableFrom(returnType)) {
            return MethodReturnMode.LIST;
        }
        if (void.class.equals(returnType)) {
            return MethodReturnMode.VOID;
        }
        return MethodReturnMode.SINGLE;
    }

    public static void main(String[] args) {
        System.out.println(MapperManager.class.getPackageName());
    }

}
