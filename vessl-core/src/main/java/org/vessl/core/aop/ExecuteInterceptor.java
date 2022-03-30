package org.vessl.core.aop;

import java.lang.annotation.Annotation;

public interface ExecuteInterceptor {
    Class<? extends Annotation>[] targetAnnotation();

    void beforeHandle(ProxyData proxyData);
    default Object handle(ProxyExecutor proxyExecutor) throws Throwable{
        return proxyExecutor.invoke();
    }
    void afterHandle(ProxyData proxyData);

    void afterReturn(ProxyData proxyData,Object result);
    void afterException(ProxyData proxyData,Throwable e);
}
