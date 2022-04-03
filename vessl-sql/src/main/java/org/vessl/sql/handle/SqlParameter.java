package org.vessl.sql.handle;

import com.google.common.collect.Multimap;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.vessl.sql.bean.SqlMethodBean;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SqlParameter implements SqlProcessStep {


    @Override
    public Object execute(SqlMethodBean sqlMethodBean, Object[] args) throws Exception {
        execute((PreparedStatement) args[0], (Map<String, Object>) args[1], (Multimap<String, Integer>) args[2]);
        return null;
    }


    /**
     * 为PreparedStatement参数赋值
     *
     * @param statement
     * @throws Exception
     */
    static void execute(PreparedStatement statement, Map<String, Object> argsMap, Multimap<String, Integer> paramIndexMap) throws Exception {
        List<String> notMapperFiledList = new ArrayList<>();

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

                Collection<Integer> indexes = paramIndexMap.get(paramName);
                for (Integer index : indexes) {
                    statement.setObject(index, value);
                }

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


}

