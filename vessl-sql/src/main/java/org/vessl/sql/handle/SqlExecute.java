package org.vessl.sql.handle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.vessl.sql.annotation.Name;
import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.constant.MethodReturnMode;
import org.vessl.sql.constant.SqlType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlExecutor {

    private Method method;
    private SqlMethodBean sqlMethodBean;


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


    protected SqlExecutor(Method method, SqlMethodBean sqlMethodBean) {
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
    Object execute(Object[] args) throws Throwable {
        try {
            Map<String, Object> argsMap = new HashMap<>();
            for (int i = 0; i < methodParameterNames.size(); i++) {
                String paramName = methodParameterNames.get(i);
                argsMap.put(paramName, args[i]);
            }
            boolean isTrans = true;
            Connection connection = SqlSession.connectionThreadLocal.get();
            if (connection == null) {
                isTrans = false;
                connection = MapperManager.getDataSource().getConnection();
            }
            try {
                PreparedStatement statement = connection.prepareStatement(execSql);
                packageParameter(statement, argsMap, paramIndexMap);

                if (sqlMethodBean.getSqlType() == SqlType.SELECT) {
                    ResultSet resultSet = statement.executeQuery();
                    Type returnType = method.getGenericReturnType();
                    return parseResult(resultSet, sqlMethodBean.getReturnMode(), returnType);
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


    /**
     * 为PreparedStatement参数赋值
     *
     * @param statement
     * @param args
     * @throws Exception
     */
    private void packageParameter(PreparedStatement statement, Map<String, Object> argsMap, Multimap<String, Integer> paramIndexMap) throws Exception {
        SqlParameter.execute(statement, argsMap, paramIndexMap);
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
    private Object parseResult(ResultSet resultSet, MethodReturnMode methodReturnMode, Type type) throws NoSuchMethodException, SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return SqlResult.execute(resultSet, methodReturnMode, type);
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


}

