package org.vessl.bean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.lang3.StringUtils;
import org.vessl.bean.async.Async;
import org.vessl.bean.config.ConfigManager;
import org.vessl.bean.config.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BeanStore {
    /**
     * 按bean名保存,优先按类型加载,如果有多个则按名称(名称是唯一),如果没有名称中,则在类型中选一个
     */
    private static Map<String, PackageObject> beanWithNameMap = new HashMap<>();
    private static Map<String, PackageObject> beanWithNameOriginMap = new HashMap<>();
    /**
     * 按类型保存 r父类,c类本身 v对象
     */
    private static ArrayListMultimap<Class<?>, PackageObject> beanWithClassMap = ArrayListMultimap.create();
    private static ArrayListMultimap<Class<?>, PackageObject> beanWithClassOriginMap = ArrayListMultimap.create();

    /**
     * 用来判断注解上还有哪些注解
     */
    private static Multimap<Class<?>, Class<?>> annotationClassMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Annotation> annotationObjectMap = ArrayListMultimap.create();
    private static List<Method> beanMethodList = new ArrayList<>();


    private static List<MethodAnnotation> annotationMethodList = new ArrayList<>();

    /**
     * 缓存aop处理
     */
    private static List<Class<?>> executeHandlerList = new ArrayList<>();
    //----- 代理相关

    private static Multimap<Class<?>, Field> pendingInjectMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Field> pendingSetValueMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Method> pendingInitMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Method> pendingDestroyMap = ArrayListMultimap.create();

    public static <T> T getBean(Class<T> tClass) {
       /* List<PackageObject> packageObjects = beanWithClassProxyMap.get(tClass);
        if (packageObjects.size() > 0) {
            return (T) packageObjects.get(packageObjects.size() - 1).getObject();
        }*/
        List<PackageObject>  packageObjects = beanWithClassMap.get(tClass);
        if (packageObjects.size() > 0) {
            return (T) packageObjects.get(packageObjects.size() - 1).getObject();
        }

        return null;
    }
    static <T> T getOriginBean(Class<T> tClass) {
       /* List<PackageObject> packageObjects = beanWithClassProxyMap.get(tClass);
        if (packageObjects.size() > 0) {
            return (T) packageObjects.get(packageObjects.size() - 1).getObject();
        }*/
        List<PackageObject>  packageObjects = beanWithClassMap.get(tClass);
        if (packageObjects.size() == 0) {
            return null;
        }
        PackageObject packageObject = packageObjects.get(packageObjects.size() - 1);
        if (packageObject.isProxy) {
            return (T)packageObject.getTarget();
        }
        return (T)packageObject.getObject();


    }

    public static <T> T getBean(String name) {
       /* PackageObject packageObject = beanWithNameProxyMap.get(name);
        if (packageObject != null) {
            return (T)packageObject.getObject();
        }*/
        PackageObject   packageObject = beanWithNameMap.get(name);
        if (packageObject != null) {
            return (T)packageObject.getObject();
        }
        return null;
    }

    public static <T> T getOriginBean(String name) {
       /* PackageObject packageObject = beanWithNameProxyMap.get(name);
        if (packageObject != null) {
            return (T)packageObject.getObject();
        }*/
        PackageObject   packageObject = beanWithNameMap.get(name);
        if (packageObject == null) {
            return null;
        }
        if (packageObject.isProxy) {
            return (T)packageObject.getTarget();
        }
        return (T)packageObject.getObject();

    }


    static void addExecuteHandle(Class<?> clazz) {
        if (!executeHandlerList.contains(clazz)) {
            executeHandlerList.add(clazz);
        }
        addBeanWithClass(null, clazz);

    }

    static void inject() {
        for (Map.Entry<Class<?>, Field> entry : pendingInjectMap.entries()) {
            Class<?> aClass = entry.getKey();
            Field field = entry.getValue();
            String value = field.getAnnotation(Inject.class).value();
            boolean required = field.getAnnotation(Inject.class).required();
            Class<?> fieldType = field.getType();
            Object bean = null;
            if (StringUtils.isNotEmpty(value)) {
                bean = getBean(value);

            } else {
                bean = getBean(fieldType);
            }
            //单独处理map list
            if (Map.class.isAssignableFrom(fieldType)) {
                ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                Type actualTypeArgument = genericType.getActualTypeArguments()[1];
                Class<?> clazz = TypeToken.of(actualTypeArgument).getRawType();
                bean = getFileValueWithMap(clazz);
            } else if (List.class.isAssignableFrom(fieldType)) {
                ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                Type actualTypeArgument = genericType.getActualTypeArguments()[0];
                Class<?> clazz = TypeToken.of(actualTypeArgument).getRawType();
                bean = getFileValueWithList(clazz);
            }

            if (bean == null) {
                if (required) {
                    throw new RuntimeException("the required field not exist:" + fieldType);
                }
                continue;
            }
            Object targetClass = getOriginBean(aClass);
            boolean flag = field.canAccess(targetClass);
            field.setAccessible(true);
            try {
                field.set(targetClass, bean);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                if (required) {
                    field.setAccessible(flag);
                    throw new RuntimeException("the required field not exist:" + fieldType);
                }
            }
            field.setAccessible(flag);
        }

    }

    static void setFiledValue() {
        for (Map.Entry<Class<?>, Field> entry : pendingSetValueMap.entries()) {
            Class<?> aClass = entry.getKey();
            Field field = entry.getValue();
            String value = field.getAnnotation(Value.class).value();
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            Object filedValue = null;
            filedValue = getValueFromConfig(value);
            if (filedValue == null) {
                continue;
            }
            Object targetClass = getOriginBean(aClass);

            boolean flag = field.canAccess(targetClass);
            field.setAccessible(true);
            try {
                field.set(targetClass, filedValue);
            } catch (IllegalAccessException e) {
                //TODO
            }
            field.setAccessible(flag);
        }

    }

    private static <T> Map<String, T> getFileValueWithMap(Class<?> clazz) {
        Map<String, T> map = new HashMap<>();
        List<PackageObject> rows = beanWithClassMap.get(clazz);
        for (PackageObject value : rows) {
            map.put(value.getBeanName(), (T) value.getObject());
        }

        return map;

    }

    private static <T> List<T> getFileValueWithList(Class<?> clazz) {
        List<T> list = new ArrayList<>();
        List<PackageObject> rows = beanWithClassMap.get(clazz);
        for (PackageObject value : rows) {
            list.add((T) value.getObject());
        }

        return list;

    }


    static void setFiledValue(Field field, Object object) {
        String value = field.getAnnotation(Value.class).value();
        if (StringUtils.isEmpty(value)) {
            return;
        }
        Object filedValue = null;
        filedValue = getValueFromConfig(value);
        if (filedValue == null) {
            return;
        }
        Class<?> type = field.getType();

        boolean flag = field.canAccess(object);
        field.setAccessible(true);
        try {
            field.set(object, filedValue);
        } catch (IllegalAccessException e) {
            //TODO
        }
        field.setAccessible(flag);

    }

    private static Object getValueFromConfig(String value) {
        Object filedValue;
        if (!"${}".equals(value) && value.contains("${") && value.contains("}")) {
            String key = value.substring(value.indexOf("${") + 2, value.indexOf("}"));
            String defaultValue = null;
            if (key.contains(":")) {
                String[] split = key.split(":");
                key = split[0];
                if (split.length > 1) {
                    defaultValue = split[1];
                } else {
                    defaultValue = "";
                }
            }
            filedValue = ConfigManager.get(key);
            if (filedValue == null) {
                filedValue = defaultValue;
            }
        } else {
            filedValue = value;
        }
        return filedValue;
    }

    static void invokeInit() throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<Class<?>, Method> entry : pendingInitMap.entries()) {
            Object targetClass = getBean(entry.getKey());
            boolean flag = entry.getValue().canAccess(targetClass);
            entry.getValue().setAccessible(true);
            entry.getValue().invoke(targetClass);
            entry.getValue().setAccessible(flag);
        }
    }

    static void invokeDestroy() throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<Class<?>, Method> entry : pendingDestroyMap.entries()) {
            Object targetClass = getBean(entry.getKey());
            boolean flag = entry.getValue().canAccess(targetClass);
            entry.getValue().setAccessible(true);
            entry.getValue().invoke(targetClass);
            entry.getValue().setAccessible(flag);
        }
    }

    static void initBeanMethod() {
        Map<String, Object> objectMap = new HashMap<>();
        for (Method method : beanMethodList) {
            Bean annotation = method.getAnnotation(Bean.class);
            String beanName = annotation.value();
            if (StringUtils.isEmpty(beanName)) {
                beanName = method.getName();
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                Object bean = getBean(parameterType);
                args[i] = bean;
            }
            String s = ReflectUtils.getSignature(method).toString();
            Object o = objectMap.computeIfAbsent(s, s1 -> {
                try {
                    return method.getDeclaringClass().getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    return null;
                    //TODO
                }
            });
            if (o != null) {
                objectMap.put(ReflectUtils.getSignature(method).toString(), o);
                try {
                    Object result = method.invoke(o, args);
                    isNeedInject(result.getClass());
                    isNeedProxy(result.getClass());
                    addBean(result.getClass(), result, beanName);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    //TODO
                }
            }

        }

    }

    static void executeHandle() {

        //缓存代理注解和代理类的关系
        HashMultimap<Class<? extends Annotation>, ClassExecuteHandler> pendingAnnotationMap = HashMultimap.create();
        for (Class<?> clazz : executeHandlerList) {
            ClassExecuteHandler classExecuteHandler = (ClassExecuteHandler) getBean(clazz);
            Class<? extends Annotation>[] annotationClasses = classExecuteHandler.targetAnnotation();
            for (Class<? extends Annotation> aClass : annotationClasses) {
                pendingAnnotationMap.put(aClass, classExecuteHandler);
            }

        }
        for (MethodAnnotation methodAnnotation : annotationMethodList) {
            List<Class<? extends Annotation>> needRemove = new ArrayList<>();
            HashMultimap<Class<? extends Annotation>, ClassExecuteHandler> classExecuteHandlers = HashMultimap.create();
            for (Class<? extends Annotation> annotationClass : methodAnnotation.annotations()) {
                Set<ClassExecuteHandler> tmpClassHandlers = pendingAnnotationMap.get(annotationClass);
                if (tmpClassHandlers.isEmpty()) {
                    needRemove.add(annotationClass);
                    continue;
                }
                classExecuteHandlers.putAll(annotationClass, tmpClassHandlers);

            }
            //去掉没有代理的注解
            methodAnnotation.removeAnnotation(needRemove);

            if (classExecuteHandlers.isEmpty()) {
                continue;
            }
            Object bean = getBean(methodAnnotation.getClazz());
            BeanClassProxy beanClassProxy = new BeanClassProxy(classExecuteHandlers, bean, methodAnnotation);
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(methodAnnotation.getClazz());
            enhancer.setCallback(beanClassProxy);
            Object o = enhancer.create();
            addProxyBean(methodAnnotation.getClazz(), o, methodAnnotation.getClazz().getName());
        }

        annotationMethodList.clear();
        executeHandlerList.clear();
    }

    static void addAnnotationMap(Class<?> annotationClass) {
        for (Annotation annotation : annotationClass.getAnnotations()) {
            if (!annotation.annotationType().getCanonicalName().startsWith("java.")) {
                annotationClassMap.put(annotationClass, annotation.annotationType());
                annotationObjectMap.put(annotationClass, annotation);
            }
        }

    }

    static void add(String beanName, Class<?> clazz) {
        isNeedInject(clazz);
        isNeedSetValue(clazz);
        isNeedProxy(clazz);
        if (clazz.isInterface()) {
            return;
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return;
        }
        addBeanWithClass(beanName, clazz);
    }

    private static void isNeedInject(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Object.class.equals(field.getDeclaringClass())) {
                continue;
            }
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getName().startsWith("java")) {
                    continue;
                }
                if (annotation.annotationType() == Inject.class) {
                    pendingInjectMap.put(clazz, field);
                }
            }
        }
    }

    private static void isNeedSetValue(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Object.class.equals(field.getDeclaringClass())) {
                continue;
            }
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getName().startsWith("java")) {
                    continue;
                }
                if (annotation.annotationType() == Value.class) {
                    pendingSetValueMap.put(clazz, field);
                }
            }
        }
    }

    private static void isNeedProxy(Class<?> clazz) {
        if (ClassExecuteHandler.class.isAssignableFrom(clazz)) {
            return;

        }
        //如果方法上有注解,需要先缓存起来,看是否有代理类
        for (Method method : clazz.getMethods()) {
            if (Object.class.equals(method.getDeclaringClass())) {
                continue;
            }
            Annotation[] methodAnnotations = method.getAnnotations();
            MethodAnnotation mAnnotation = new MethodAnnotation(clazz);
            for (Annotation methodAnnotation : methodAnnotations) {
                Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
                if (annotationType.getName().startsWith("java")) {
                    continue;
                }
                if (annotationType == Bean.class) {
                    beanMethodList.add(method);
                    continue;
                }
                if (annotationType == Init.class) {
                    pendingInitMap.put(clazz, method);
                    continue;
                }
                if (annotationType == Destroy.class) {
                    pendingDestroyMap.put(clazz, method);
                    continue;
                }
                if (annotationType == Async.class) {
                    if (getBean(ThreadPoolExecutor.class) == null) {
                        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2,
                                60L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>(100));
                        addWithClassObject(ThreadPoolExecutor.class, threadPoolExecutor);
                    }
                }
                mAnnotation.add(method, annotationType);

            }
            if (!mAnnotation.isEmpty()) {
                annotationMethodList.add(mAnnotation);
            }

        }
    }


    static void addBeanWithClass(String beanName, Class<?> clazz) {

        if (StringUtils.isEmpty(beanName)) {
            beanName = clazz.getName();
        }
        try {
            Object o = clazz.getDeclaredConstructor().newInstance();
            addBean(clazz, o, beanName);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            //TODO
        }


    }

    public static void addWithClassObject(Class<?> clazz, Object obj) {
        String beanName = clazz.getName();
        addBean(clazz, obj, beanName);

    }

    private static void addBean(Class<?> clazz, Object obj, String beanName) {
        PackageObject packageObject = new PackageObject(beanName, obj);
        beanWithNameMap.put(beanName, packageObject);
        beanWithClassMap.put(clazz, packageObject);
        List<Class<?>> superClass = getSuperClass(clazz);
        superClass.addAll(getSuperInterface(clazz));
        for (Class<?> aClass : superClass) {
            beanWithClassMap.put(aClass, packageObject);
        }
    }

    private static void addProxyBean(Class<?> clazz, Object obj, String beanName) {
        PackageObject object = beanWithNameMap.get(beanName);
        PackageObject packageObject = new PackageObject(beanName, obj);
        if(object!=null) {
            packageObject.setProxy(true);
            packageObject.setTarget(object.getObject());
        }
        beanWithNameMap.put(beanName, packageObject);
        beanWithClassMap.put(clazz, packageObject);
        List<Class<?>> superClass = getSuperClass(clazz);
        superClass.addAll(getSuperInterface(clazz));
        for (Class<?> aClass : superClass) {
            beanWithClassMap.put(aClass, packageObject);
        }
    }

    /**
     * 获取这个类的所有父类
     *
     * @param clazz
     * @return
     */
    private static List<Class<?>> getSuperClass(Class<?> clazz) {
        List<Class<?>> clazzs = new ArrayList<>();
        Class<?> suCl = clazz.getSuperclass();
        while (suCl != null && suCl != Object.class) {
            clazzs.add(suCl);
            suCl = suCl.getSuperclass();
        }
        return clazzs;
    }

    /**
     * 获取这个类的所有父接口
     *
     * @param clazz
     * @return
     */
    private static List<Class<?>> getSuperInterface(Class<?> clazz) {
        List<Class<?>> clazzs = new ArrayList<>();
        Class<?>[] suCl = clazz.getInterfaces();
        for (Class<?> aClass : suCl) {
            List<Class<?>> superClass = getSuperClass(aClass);
            clazzs.addAll(superClass);
        }
        return clazzs;
    }

}
