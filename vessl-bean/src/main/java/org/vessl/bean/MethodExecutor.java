package org.vessl.bean;

import lombok.AllArgsConstructor;
import net.sf.cglib.core.Signature;

import java.lang.reflect.Method;
import java.util.List;

@AllArgsConstructor
public class MethodExecutor {
    private List<ClassExecuteHandler> classExecuteHandlers;
    private Signature signature;
    private Method method;
    private Object target;
    private Object[] args;


    public Object invoke() throws Throwable {
        if (classExecuteHandlers.size() == 0) {
            return method.invoke(target, args);
        }
        ClassExecuteHandler classExecuteHandler = classExecuteHandlers.remove(0);
        classExecuteHandler.beforeHandle(signature, args);
        Object handle = null;
        try {
            //handle = classExecuteHandler.handle(new MethodExecutor(classExecuteHandlers, signature, method, target, args));
            handle = classExecuteHandler.handle(this);
            classExecuteHandler.afterHandle(signature);
            classExecuteHandler.afterReturn(signature, args, handle);
            return handle;
        } catch (Throwable e) {
            classExecuteHandler.afterException(signature, args, e);
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
