package org.vessl.sql.handle;

import lombok.AllArgsConstructor;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.vessl.sql.constant.MethodReturnMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SqlResult implements SqlProcessStep {

    private ResultSet resultSet;
    private MethodReturnMode returnMode;
    private Type type;


    @Override
    public Object execute(MapperInvoker mapperInvoker,Object[] args) throws Exception {
        return execute(resultSet, returnMode, type);
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
    static Object execute(ResultSet resultSet, MethodReturnMode returnMode, Type type) throws NoSuchMethodException, SQLException, InvocationTargetException, InstantiationException, IllegalAccessException {
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
        if (returnMode.equals(MethodReturnMode.SINGLE)) {
            return list.get(0);
        }

        return list;
    }


    private static String toCamel(String name) {
        if (!name.contains("_")) {
            return name;
        }
        if (name.indexOf("_") == name.length() - 1) {
            return name.substring(0, name.length() - 1);
        }
        String[] s = name.split("_");

        return s[0].toLowerCase() + StringUtils.capitalize(s[1].toLowerCase());
    }

    private static boolean isPrimitive(Class clz) {
        try {
            return clz.isPrimitive() || ((Class) clz.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

}
