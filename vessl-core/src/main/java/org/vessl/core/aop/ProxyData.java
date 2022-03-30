package org.vessl.core.aop;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.sf.cglib.core.Signature;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
public class ProxyData {
    private Signature signature;
    private Method method;
    private Object target;
    private Object[] args;
}
