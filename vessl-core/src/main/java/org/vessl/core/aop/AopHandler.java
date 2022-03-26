package org.vessl.core.aop;

import com.google.common.collect.HashMultimap;
import net.sf.cglib.proxy.Enhancer;
import org.vessl.core.bean.BeanStore;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author xiecan
 */
public class AopHandler {


    /**
     * 被注解的方法,可能会有AOP类拦截
     */
    private final List<ClassMethodAnnotation> annotationMethodList = new ArrayList<>();

    /**
     * 缓存aop拦截器类
     */
    private final List<Class<?>> executeInterceptorList = new ArrayList<>();

    public void addExecuteInterceptor(Class<?> clazz) {
        if (!executeInterceptorList.contains(clazz)) {
            executeInterceptorList.add(clazz);
        }
    }

    public  void addClassAnnotationMethod(ClassMethodAnnotation classMethodAnnotation){
        annotationMethodList.add(classMethodAnnotation);
    }

    /**
     * 设置proxy
     */
    public  void initAop() {

        //缓存代理注解和代理类的关系
        HashMultimap<Class<? extends Annotation>, ExecuteInterceptor> pendingAnnotationMap = HashMultimap.create();
        for (Class<?> clazz : executeInterceptorList) {
            ExecuteInterceptor executeInterceptor = (ExecuteInterceptor) BeanStore.getBean(clazz);
            if (executeInterceptor == null) {
                continue;
            }
            Class<? extends Annotation>[] annotationClasses = executeInterceptor.targetAnnotation();
            for (Class<? extends Annotation> aClass : annotationClasses) {
                pendingAnnotationMap.put(aClass, executeInterceptor);
            }

        }
        for (ClassMethodAnnotation classMethodAnnotation : annotationMethodList) {
            List<Class<? extends Annotation>> needRemove = new ArrayList<>();
            HashMultimap<Class<? extends Annotation>, ExecuteInterceptor> classExecuteHandlers = HashMultimap.create();
            for (Class<? extends Annotation> annotationClass : classMethodAnnotation.annotations()) {
                Set<ExecuteInterceptor> tmpClassHandlers = pendingAnnotationMap.get(annotationClass);
                if (tmpClassHandlers.isEmpty()) {
                    needRemove.add(annotationClass);
                    continue;
                }
                classExecuteHandlers.putAll(annotationClass, tmpClassHandlers);

            }
            //去掉没有代理的注解
            classMethodAnnotation.removeAnnotation(needRemove);

            if (classExecuteHandlers.isEmpty()) {
                continue;
            }
            Object bean = BeanStore.getBean(classMethodAnnotation.getClazz());
            BeanClassProxy beanClassProxy = new BeanClassProxy(classExecuteHandlers, bean, classMethodAnnotation);
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(classMethodAnnotation.getClazz());
            enhancer.setCallback(beanClassProxy);
            Object o = enhancer.create();
            BeanStore.addProxyBean(classMethodAnnotation.getClazz(), o);
        }

        annotationMethodList.clear();
        executeInterceptorList.clear();
    }
}
