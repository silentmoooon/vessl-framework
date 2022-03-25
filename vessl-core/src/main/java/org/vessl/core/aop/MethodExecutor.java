package org.vessl.core.aop;

import lombok.AllArgsConstructor;
import net.sf.cglib.core.Signature;

import java.lang.reflect.Method;
import java.util.List;

@AllArgsConstructor
public class MethodExecutor {
    private List<ExecuteInterceptor> executeInterceptors;
    private int executeIndex;
    private Signature signature;
    private Method method;
    private Object target;
    private Object[] args;


    public Object invoke() throws Throwable {
        if (executeInterceptors.size() <= executeIndex) {
            return method.invoke(target, args);
        }
        ExecuteInterceptor executeInterceptor = executeInterceptors.get(executeIndex);
        executeIndex++;
        executeInterceptor.beforeHandle(signature, args);
        Object handle = null;
        try {
            handle = executeInterceptor.handle(this);
            executeInterceptor.afterHandle(signature);
            executeInterceptor.afterReturn(signature, args, handle);
            return handle;
        } catch (Throwable e) {
            executeInterceptor.afterException(signature, args, e);
            throw e;
        }

    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
