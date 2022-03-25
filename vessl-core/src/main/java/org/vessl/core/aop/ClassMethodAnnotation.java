package org.vessl.core.aop;

import com.google.common.collect.ArrayListMultimap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class ClassMethodAnnotation {
    private Class<?> clazz;
    private ArrayListMultimap<Method ,Class<? extends Annotation> > methodMap = ArrayListMultimap.create();
    private ArrayListMultimap<Class<? extends Annotation>,Method > annotationMap = ArrayListMultimap.create();

    public ClassMethodAnnotation(Class<?> clazz) {
        this.clazz=clazz;
    }

    public void add(Method method, Class<? extends Annotation> clazz) {
        methodMap.put(method, clazz);
        annotationMap.put(clazz, method);
    }

    public void removeAnnotation(List<Class<? extends Annotation>> classes){
        for (Class<? extends Annotation> aClass : classes) {
            annotationMap.removeAll(aClass);
        }

    }

    public boolean isEmpty() {

        return methodMap.isEmpty();
    }

    public Set<Method> methods() {
        return methodMap.keySet();
    }

    public Set<Class<? extends Annotation>> annotations() {
        return annotationMap.keySet();
    }

    public List<Class<? extends Annotation>> get(Method method) {
        return methodMap.get(method);
    }
    public Class<?> getClazz() {
        return clazz;
    }

    public void clear() {
        annotationMap.clear();
        methodMap.clear();
    }
}
