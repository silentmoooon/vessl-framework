package org.vessl.sql.handle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.vessl.sql.annotation.Name;
import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.constant.MethodReturnMode;
import org.vessl.sql.constant.SqlType;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class MapperMethodExecutor {

    private Method method;
    private SqlMethodBean sqlMethodBean;

    /**
     * 方法参数列表
     */
    private List<Parameter> methodParameters = new ArrayList<>();
    /**
     * 方法参数名
     */
    private List<String> methodParameterNames = new ArrayList<>();
    /**
     * 包括参数名的原始SQL
     */
    private String baseSql;
    /**
     * 用来执行的只包含占位符的sql
     */
    private String execSql;

    /**
     * sql中的参数名及对应的index
     */
    private Multimap<String, Integer> paramIndexMap = ArrayListMultimap.create();


    protected MapperMethodExecutor(Method method, SqlMethodBean sqlMethodBean) {
        this.method = method;
        this.sqlMethodBean = sqlMethodBean;
        this.baseSql = sqlMethodBean.getSql();
        this.execSql = baseSql.replaceAll("\\{[\\w\\.]*\\}", "?");
        getMethodParameter();
        getParamIndex();
    }

    public String getSql() {
        return baseSql;
    }

    protected SqlMethodBean getSqlMethodBean() {
        return sqlMethodBean;
    }

    /**
     * 获取参数列表及对应的参数名,优先取 {@link Name}注解参数名,如果没注解,需要在编译参数中增加 -parameters
     */
    protected void getMethodParameter() {

        for (Parameter parameter : method.getParameters()) {
            methodParameters.add(parameter);
            String paramName = parameter.getName();
            Name annotation = parameter.getAnnotation(Name.class);
            if (annotation != null && StringUtils.isNotEmpty(annotation.value())) {
                paramName = annotation.value();
            }
            methodParameterNames.add(paramName);
        }

    }


    /**
     * 实际调用jdbc方法
     * 优先从threadLocal中取连接,若取到,则认为启用了手动提交事务,执行后不关闭连接
     *
     * @param args
     * @return
     * @throws Throwable
     */
    Object invoke(Object[] args) throws Throwable {
        try {
            boolean isTrans = true;
            Connection connection = SqlSession.connectionThreadLocal.get();
            if (connection == null) {
                isTrans = false;
                connection = MapperManager.getDataSource().getConnection();
            }
            try {
                PreparedStatement statement = connection.prepareStatement(execSql);
                resolveParameter(statement, args);

                if (sqlMethodBean.getType().equals(SqlType.SELECT.toString())) {
                    ResultSet resultSet = statement.executeQuery();
                    Type returnType = method.getGenericReturnType();
                    List list = packData(resultSet, returnType);
                    if (sqlMethodBean.getReturnMode().equals(MethodReturnMode.SINGLE)) {
                        return list.get(0);
                    }
                    if (sqlMethodBean.getReturnMode().equals(MethodReturnMode.LIST)) {
                        return list;
                    }
                } else {
                    return statement.executeUpdate();

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!isTrans) {
                    try {

                        connection.close();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static boolean isPrimitive(Class clz) {
        try {
            return clz.isPrimitive() || ((Class) clz.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 为PreparedStatement参数赋值
     *
     * @param statement
     * @param args
     * @throws Exception
     */
    private void resolveParameter(PreparedStatement statement, Object[] args) throws Exception {
        Map<String, Object> argsMap = new HashMap<>();

        List<String> notMapperFiledList = new ArrayList<>();

        for (int i = 0; i < methodParameters.size(); i++) {
            String paramName = methodParameterNames.get(i);
            argsMap.put(paramName, args[i]);
        }
        for (String paramName : paramIndexMap.keySet()) {
            if (paramName.indexOf(".") > 0 && !paramName.endsWith(".")) {
                String[] split = paramName.split("\\.");
                if (split.length != 2) {
                    notMapperFiledList.add(paramName);
                    continue;
                }
                String objectName = split[0];
                String filedName = split[1];

                if (!argsMap.containsKey(objectName)) {
                    notMapperFiledList.add(paramName);
                    continue;
                }
                Object o = argsMap.get(objectName);
                Object value = PropertyUtils.getProperty(o, filedName);
               /* Field field = o.getClass().getDeclaredField(filedName);
                boolean flag = field.canAccess(o);
                field.setAccessible(true);
                Object result = field.get(o);*/
                Collection<Integer> indexes = paramIndexMap.get(paramName);
                for (Integer index : indexes) {
                    statement.setObject(index, value);
                }
                //field.setAccessible(flag);
            } else {
                if (!argsMap.containsKey(paramName)) {
                    notMapperFiledList.add(paramName);
                    continue;
                }
                Collection<Integer> indexes = paramIndexMap.get(paramName);
                for (Integer index : indexes) {
                    statement.setObject(index, argsMap.get(paramName));
                }
            }
        }


        if (notMapperFiledList.size() > 0) {

            throw new Exception("parameter error,the parameters: " + StringUtils.join(notMapperFiledList) + " can not mapping");
        }


    }

    /**
     * 组装返回参数
     *
     * @param resultSet
     * @param type
     * @return
     * @throws NoSuchMethodException
     * @throws SQLException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private List packData(ResultSet resultSet, Type type) throws NoSuchMethodException, SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = null;
        if (type instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();

            clazz = (Class<?>) actualTypeArguments[0];
        } else {
            clazz = (Class<?>) type;
        }
        ResultSetMetaData rsmd = resultSet.getMetaData();
        //获取结果集的元素个数
        int colCount = rsmd.getColumnCount();
        //返回结果的列表集合
        List list = new ArrayList();

        if (isPrimitive(clazz)) {
            while (resultSet.next()) {
                Object object = resultSet.getObject(1);

                list.add(object);
            }
        } else {
            while (resultSet.next()) {

                Object obj = clazz.getDeclaredConstructor().newInstance();
                //将每一个字段取出进行赋值
                for (int i = 1; i <= colCount; i++) {
                    Object value = resultSet.getObject(i);

                    if (value instanceof Timestamp timestamp) {
                        PropertyUtils.setProperty(obj, toCamel(rsmd.getColumnName(i)), timestamp.toLocalDateTime());
                    } else {
                        PropertyUtils.setProperty(obj, toCamel(rsmd.getColumnName(i)), value);
                    }

                }
                list.add(obj);
            }


        }
        return list;
    }

    /**
     * 获取sql中的参数名及索引
     */
    private void getParamIndex() {
        int fromIndex = 0;
        int index = 0;
        int count = 1;
        while ((index = baseSql.indexOf("{", fromIndex)) >= 0) {
            String name = baseSql.substring(index + 1, baseSql.indexOf("}", index));
            paramIndexMap.put(name, count);
            fromIndex = index + 1;
            count++;
        }
    }

    private String toCamel(String name) {
        if (!name.contains("_")) {
            return name;
        }
        if (name.indexOf("_") == name.length() - 1) {
            return name.substring(0, name.length() - 1);
        }
        String[] s = name.split("_");

        return s[0].toLowerCase() + StringUtils.capitalize(s[1].toLowerCase());
    }

}

