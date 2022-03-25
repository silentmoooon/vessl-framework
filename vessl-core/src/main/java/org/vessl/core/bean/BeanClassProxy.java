package org.vessl.core.bean;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.vessl.core.aop.ExecuteInterceptor;
import org.vessl.core.aop.ClassMethodAnnotation;
import org.vessl.core.aop.MethodExecutor;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class BeanClassProxy implements MethodInterceptor, InvocationHandler {


    private Object target;
    private ClassMethodAnnotation classMethodAnnotation;
    private HashMultimap<Class<? extends Annotation>, ExecuteInterceptor> executeHandlerHashMultimap;
    private final LinkedListMultimap<String, ExecuteInterceptor> methodProxyChain = LinkedListMultimap.create();


    public BeanClassProxy(HashMultimap<Class<? extends Annotation>, ExecuteInterceptor> executeHandlerHashMultimap, Object target, ClassMethodAnnotation classMethodAnnotation) {
        this.executeHandlerHashMultimap = executeHandlerHashMultimap;
        this.target = target;
        this.classMethodAnnotation = classMethodAnnotation;
        assignProxy();
    }

    private void assignProxy() {

        for (Method method : classMethodAnnotation.methods()) {
            String signature = ReflectUtils.getSignature(method).toString();
            for (Class<? extends Annotation> aClass : classMethodAnnotation.get(method)) {
                Set<ExecuteInterceptor> executeInterceptors = executeHandlerHashMultimap.get(aClass);
                methodProxyChain.putAll(signature, executeInterceptors);
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
            List<ExecuteInterceptor> executeInterceptors = methodProxyChain.get(signature.toString());
            if (executeInterceptors.size() == 0) {
                return method.invoke(target, objects);
            }
            int executeIndex = 0;
            ExecuteInterceptor executeInterceptor = executeInterceptors.get(executeIndex);

            executeInterceptor.beforeHandle(signature, objects);
            Object handle = null;
            try {
                handle = executeInterceptor.handle(new MethodExecutor(executeInterceptors, executeIndex++, signature, method, target, objects));
                executeInterceptor.afterHandle(signature);
                executeInterceptor.afterReturn(signature, objects, handle);
                return handle;
            } catch (Throwable e) {
                executeInterceptor.afterException(signature, objects, e);
                throw e;
            }

        } else {
            return method.invoke(target, objects);
        }


    }


}

