package org.vessl.sql.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
public class MethodDesc {
    private Method method;
    private SqlMethodBean sqlMethodBean;
}
