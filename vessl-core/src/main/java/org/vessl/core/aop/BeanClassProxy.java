package org.vessl.core.aop;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.vessl.core.bean.BeanOrder;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * @author xiecan
 */
public class BeanClassProxy implements MethodInterceptor, InvocationHandler {


    private final Object target;
    private final HashMultimap<Class<? extends Annotation>, ExecuteInterceptor> executeHandlerHashMultimap;
    private final LinkedListMultimap<String, ExecuteInterceptor> methodProxyChain = LinkedListMultimap.create();


    public BeanClassProxy(HashMultimap<Class<? extends Annotation>, ExecuteInterceptor> executeHandlerHashMultimap, Object target, ClassMethodAnnotation classMethodAnnotation) {
        this.executeHandlerHashMultimap = executeHandlerHashMultimap;
        this.target = target;
        assignProxy(classMethodAnnotation);
    }

    private void assignProxy(ClassMethodAnnotation classMethodAnnotation) {

        for (Method method : classMethodAnnotation.methods()) {
            String signature = ReflectUtils.getSignature(method).toString();
            for (Class<? extends Annotation> aClass : classMethodAnnotation.get(method)) {
                Set<ExecuteInterceptor> executeInterceptors = executeHandlerHashMultimap.get(aClass);
                methodProxyChain.putAll(signature, executeInterceptors);
            }
            methodProxyChain.get(signature).sort(BeanOrder::order);
        }

        classMethodAnnotation.clear();
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
            ProxyData proxyData = new ProxyData(signature, method, target, objects);

            return new ProxyExecutor(executeInterceptors,proxyData ).invoke();



        } else {
            return method.invoke(target, objects);
        }


    }


}

