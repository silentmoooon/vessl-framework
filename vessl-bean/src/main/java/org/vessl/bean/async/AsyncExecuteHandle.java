package org.vessl.bean.async;

import net.sf.cglib.core.Signature;
import org.vessl.bean.ClassExecuteHandler;
import org.vessl.bean.Inject;
import org.vessl.bean.MethodExecutor;
import org.vessl.bean.Order;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Order(Integer.MIN_VALUE)
public class AsyncExecuteHandle implements ClassExecuteHandler {

    @Inject
    ExecutorService threadPoolExecutor;

    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Async.class};
    }

    @Override
    public void beforeHandle(Signature signature, Object[] args) {

    }

    @Override
    public void afterHandle(Signature signature) {

    }

    @Override
    public Object handle(MethodExecutor methodExecutor) throws Throwable {

        Callable<Object> task = () -> {

            try {
                return methodExecutor.invoke();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }


        };

        Class<?> returnType = methodExecutor.getMethod().getReturnType();
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
    public void afterReturn(Signature signature, Object[] args, Object result) {

    }

    @Override
    public void afterException(Signature signature, Object[] args, Throwable e) {

    }
}
