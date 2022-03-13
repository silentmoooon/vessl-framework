package org.vessl.bean;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class BeanClassProxy implements MethodInterceptor, InvocationHandler {


    private Object target;
    private MethodAnnotation methodAnnotation;
    private HashMultimap<Class<? extends Annotation>, ClassExecuteHandler> executeHandlerHashMultimap;
    private final LinkedListMultimap<String, ClassExecuteHandler> methodProxyChain = LinkedListMultimap.create();


    protected BeanClassProxy(HashMultimap<Class<? extends Annotation>, ClassExecuteHandler> executeHandlerHashMultimap, Object target, MethodAnnotation methodAnnotation) {
        this.executeHandlerHashMultimap = executeHandlerHashMultimap;
        this.target = target;
        this.methodAnnotation = methodAnnotation;
        assignProxy();
    }

    private void assignProxy() {

        for (Method method : methodAnnotation.methods()) {
            String signature = ReflectUtils.getSignature(method).toString();
            for (Class<? extends Annotation> aClass : methodAnnotation.get(method)) {
                Set<ClassExecuteHandler> classExecuteHandlers = executeHandlerHashMultimap.get(aClass);
                methodProxyChain.putAll(signature, classExecuteHandlers);
            }
            methodProxyChain.get(signature).sort(BeanOrder::order);
        }


    }


    @Override
    public Object intercept(Object object, Method method, Object[] objects, MethodProxy proxy) throws Throwable {

        return execute(object, method, objects);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return execute(proxy, method, args);

    }

    private Object execute(Object proxy, Method method, Object[] objects) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(proxy, objects);
        }
        Signature signature = ReflectUtils.getSignature(method);
        if (methodProxyChain.containsKey(signature.toString())) {
            List<ClassExecuteHandler> classExecuteHandlers = methodProxyChain.get(signature.toString());
            if (classExecuteHandlers.size() == 0) {
                return method.invoke(target, objects);
            }
            int executeIndex = 0;
            ClassExecuteHandler classExecuteHandler = classExecuteHandlers.get(executeIndex);

            classExecuteHandler.beforeHandle(signature, objects);
            Object handle = null;
            try {
                handle = classExecuteHandler.handle(new MethodExecutor(classExecuteHandlers, executeIndex++, signature, method, target, objects));
                classExecuteHandler.afterHandle(signature);
                classExecuteHandler.afterReturn(signature, objects, handle);
                return handle;
            } catch (Throwable e) {
                classExecuteHandler.afterException(signature, objects, e);
                throw e;
            }

        } else {
            return method.invoke(target, objects);
        }


    }


}

