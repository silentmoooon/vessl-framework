package org.vessl.sql.handle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.lang3.StringUtils;
import org.vessl.core.bean.BeanStore;
import org.vessl.sql.annotation.Sql;
import org.vessl.sql.bean.MethodDesc;
import org.vessl.sql.bean.SqlClassBean;
import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.constant.MethodReturnMode;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapperManager {

    private static DataSource dataSource;
    /**
     * 根据mapper类缓存的mapper动态代理对象
     */
    private static Map<Class<?>, Object> mapperProxyMap = new HashMap<>();

    /**
     * 缓存类中方法定义
     */
    private static Multimap<Class<?>, MethodDesc> classMethodMap = ArrayListMultimap.create();

    static DataSource getDataSource() {
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
                methodBean.setSql(sqlAnnotation.value());
                MethodReturnMode methodReturnMode = getMethodReturnMode(method);
                methodBean.setReturnMode(methodReturnMode);
                classMethodMap.put(type, new MethodDesc(method,methodBean));

            }
        }

    }


    static void initMapperProxyAndDatasource(HikariConfig config){
        dataSource = new HikariDataSource(config);
        for (Class<?> aClass : classMethodMap.keySet()) {
            MapperProxy mapperProxy = new MapperProxy(aClass,classMethodMap.get(aClass).stream().toList());
           /* if(aClass.isInterface()) {
                Object o = Proxy.newProxyInstance(aClass.getClassLoader(), new Class[]{aClass}, mapperProxy);
                mapperProxyMap.put(aClass, o);
            }else{
*/
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(aClass);
                enhancer.setCallback(mapperProxy);
                Object o = enhancer.create();
                mapperProxyMap.put(aClass, o);
           // }
        }
        classMethodMap.clear();
        mapperProxyMap.forEach(BeanStore::addWithClassObject);
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
                classMethodMap.put(clazz, new MethodDesc(method,methodBean));
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


}
