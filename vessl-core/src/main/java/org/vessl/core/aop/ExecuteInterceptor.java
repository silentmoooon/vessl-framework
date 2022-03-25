package org.vessl.core.aop;

import net.sf.cglib.core.Signature;

import java.lang.annotation.Annotation;

public interface ExecuteInterceptor {
    Class<? extends Annotation>[] targetAnnotation();

    void beforeHandle(Signature signature,Object[] args);
    default Object handle(MethodExecutor methodExecutor) throws Throwable{
        return methodExecutor.invoke();
    }
    void afterHandle(Signature signature);

    void afterReturn(Signature signature,Object[] args,Object result);
    void afterException(Signature signature,Object[] args,Throwable e);
}
