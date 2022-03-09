package org.vessl.web.handle;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class MethodHandle {
    Method method;
    Object object;

    public MethodHandle(Method method, Object object) {
        this.method=method;
        this.object = object;
    }
    List<String> pathParamName;
    Pattern pathRegex;
    boolean regex;
}
