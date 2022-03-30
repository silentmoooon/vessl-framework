package org.vessl.core.aop;

import net.sf.cglib.core.Signature;

import java.lang.reflect.Method;
import java.util.List;

public class ProxyExecutor {
    private List<ExecuteInterceptor> executeInterceptors;
    private int executeIndex;
    private ProxyData proxyData;

    public ProxyExecutor(List<ExecuteInterceptor> executeInterceptors, ProxyData proxyData) {
        this.executeInterceptors = executeInterceptors;
        this.proxyData = proxyData;
    }


    public Object invoke() throws Throwable {
        if (executeInterceptors.size() <= executeIndex) {
            return proxyData.getMethod().invoke(proxyData.getTarget(), proxyData.getArgs());
        }
        ExecuteInterceptor executeInterceptor = executeInterceptors.get(executeIndex);
        executeIndex++;
        executeInterceptor.beforeHandle(proxyData);
        Object handle;
        try {
            handle = executeInterceptor.handle(this);
            executeInterceptor.afterHandle(proxyData);
            executeInterceptor.afterReturn(proxyData, handle);
            return handle;
        } catch (Throwable e) {
            executeInterceptor.afterException(proxyData, e);
            throw e;
        }

    }

    public Signature getSignature() {
        return proxyData.getSignature();
    }


    public Method getMethod() {
        return proxyData.getMethod();
    }


    public Object getTarget() {
        return proxyData.getTarget();
    }


    public Object[] getArgs() {
        return proxyData.getArgs();
    }

}
