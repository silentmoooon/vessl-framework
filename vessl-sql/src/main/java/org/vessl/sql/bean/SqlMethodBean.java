package org.vessl.sql.bean;

import lombok.Data;
import lombok.ToString;
import org.vessl.sql.constant.MethodReturnMode;
import org.vessl.sql.constant.SqlType;

import java.lang.reflect.Type;

@Data
@ToString
public class SqlMethodBean {
    private String name;
    private String type;
    private String sql;
    private SqlType sqlType;
    private MethodReturnMode returnMode;
    private Type returnType;
}
