package org.vessl.bean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

public class BeanClassProxy implements MethodInterceptor, InvocationHandler {


    private Object target;
    private Collection<Method> methods;
    private List<ClassExecuteHandler> classExecuteHandlers;
    private final Map<String, Method> methodMap = new HashMap<>();
    private final LinkedListMultimap<String, ClassExecuteHandler> methodProxyChain = LinkedListMultimap.create();


    private final Multimap<Class<? extends Annotation>, ClassExecuteHandler> annotationClass = ArrayListMultimap.create();

    protected BeanClassProxy(List<ClassExecuteHandler> classExecuteHandlers, Object target, Collection<Method> methods) {
        this.classExecuteHandlers = classExecuteHandlers;
        this.target=target;
        this.methods = methods;
        assignProxy();
    }

    private void assignProxy() {
        for (ClassExecuteHandler classExecuteHandler : classExecuteHandlers) {
            Class<? extends Annotation>[] classes = classExecuteHandler.targetAnnotation();
            for (Class<? extends Annotation> aClass : classes) {
                annotationClass.put(aClass, classExecuteHandler);
            }
        }
        for (Method method : methods) {
            String signature = ReflectUtils.getSignature(method).toString();
            methodMap.put(signature, method);
            Set<Class<? extends ClassExecuteHandler>> handlers = new HashSet<>();
            for (Annotation annotation : method.getAnnotations()) {
                for (ClassExecuteHandler classExecuteHandler : annotationClass.get(annotation.annotationType())) {
                    if (!handlers.contains(classExecuteHandler.getClass())) {
                        handlers.add(classExecuteHandler.getClass());
                        methodProxyChain.put(signature, classExecuteHandler);
                    }
                }
            }
        }
        for (String s : methodProxyChain.keySet()) {
            List<ClassExecuteHandler> classExecuteHandlers = methodProxyChain.get(s);
            classExecuteHandlers.sort(BeanOrder::order);

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
            //复制一份
            List<ClassExecuteHandler> tempClassExecuteHandlers = new ArrayList<>(classExecuteHandlers);

            ClassExecuteHandler classExecuteHandler = tempClassExecuteHandlers.remove(0);

            classExecuteHandler.beforeHandle(signature, objects);
            Object handle = null;
            try {
                handle = classExecuteHandler.handle(new MethodExecutor(tempClassExecuteHandlers, signature, method, target, objects));
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

