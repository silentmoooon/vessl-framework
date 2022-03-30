package org.vessl.core.async;

import org.vessl.core.aop.Aop;
import org.vessl.core.aop.ExecuteInterceptor;
import org.vessl.core.aop.ProxyData;
import org.vessl.core.aop.ProxyExecutor;
import org.vessl.core.bean.Inject;
import org.vessl.core.bean.Order;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Aop
@Order(Integer.MIN_VALUE)
public class AsyncExecuteInterceptor implements ExecuteInterceptor {

    @Inject
    ExecutorService threadPoolExecutor;

    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Async.class};
    }

    @Override
    public void beforeHandle(ProxyData proxyData) {

    }

    @Override
    public void afterHandle(ProxyData proxyData) {

    }

    @Override
    public Object handle(ProxyExecutor proxyExecutor) throws Throwable {

        Callable<Object> task = () -> {

            try {
                return proxyExecutor.invoke();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }


        };

        Class<?> returnType = proxyExecutor.getMethod().getReturnType();
        if (Void.class.isAssignableFrom(returnType)) {
            threadPoolExecutor.submit(task);
        } else if (CompletableFuture.class.isAssignableFrom(returnType)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }, threadPoolExecutor);
        } else if (Future.class.isAssignableFrom(returnType)) {
            return threadPoolExecutor.submit(task);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }, threadPoolExecutor).get();
        }

        return null;
    }

    @Override
    public void afterReturn(ProxyData proxyData, Object result) {

    }

    @Override
    public void afterException(ProxyData proxyData, Throwable e) {

    }
}
