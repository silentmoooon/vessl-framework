package org.vessl.bean;

import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.lang3.StringUtils;
import org.vessl.bean.config.ConfigManager;
import org.vessl.bean.config.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class BeanStore {
    /**
     * 按bean名保存,优先按类型加载,如果有多个则按名称(名称是唯一),如果没有名称中,则在类型中选一个
     */
    private static Table<Class<?>, String, PackageObject> beanWithNameMap = HashBasedTable.create();
    /**
     * 按类型保存 r父类,c类本身 v对象
     */
    private static Table<Class<?>, Class<?>, PackageObject> beanWithClassMap = HashBasedTable.create();

    /**
     * 用来判断注解上还有哪些注解
     */
    private static Multimap<Class<?>, Class<?>> annotationClassMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Annotation> annotationObjectMap = ArrayListMultimap.create();
    private static List<Method> beanMethodList = new ArrayList<>();

    //----- 代理相关
    //需要被代理的类,及有代理注解的方法命令
    private static Multimap<Class<?>, Method> pendingClassMap = ArrayListMultimap.create();
    //需要被代理的类,及其方法上的注解集合
    private static Multimap<Class<?>, Class<? extends Annotation>> pendingClassAnnotationMap = HashMultimap.create();

    /**
     * 缓存aop处理
     */
    private static List<Class<?>> executeHandlerList = new  ArrayList<>();
    //----- 代理相关

    private static Multimap<Class<?>, Field> pendingInjectMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Field> pendingSetValueMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Method> pendingInitMap = ArrayListMultimap.create();
    private static Multimap<Class<?>, Method> pendingDestroyMap = ArrayListMultimap.create();

    public static <T> T getBean(Class<T> tClass) {
        PackageObject packageObject = beanWithClassMap.get(tClass, tClass);
        if (packageObject != null) {
            return (T) packageObject.getObject();
        }
        Map<Class<?>, PackageObject> row = beanWithClassMap.row(tClass);
        for (PackageObject value : row.values()) {
            return (T) value.getObject();
        }
        return null;
    }

    public static <T> T getBean(Class<T> tClass, String name) {
        PackageObject o = beanWithNameMap.get(tClass, name);
        if (o == null) {
            return null;
        }
        return (T) o.getObject();
    }


    static void addExecuteHandle(Class<?> clazz) {
        if(!executeHandlerList.contains(clazz)) {
            executeHandlerList.add(clazz);
        }
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
                bean = getBean(fieldType, value);

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
            Object targetClass = getBean(aClass);
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
            Class<?> fieldType = field.getType();
            Object targetClass = getBean(aClass);

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
        Map<Class<?>, PackageObject> row = beanWithClassMap.row(clazz);
        for (PackageObject value : row.values()) {
            map.put(value.getBeanName(), (T) value.getObject());
        }

        return map;

    }

    private static <T> List<T> getFileValueWithList(Class<?> clazz) {
        List<T> list = new ArrayList<>();
        Map<Class<?>, PackageObject> row = beanWithClassMap.row(clazz);
        for (PackageObject value : row.values()) {
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
        executeHandlerList.sort(BeanOrder::order);
        //缓存代理类型和其对象
        Map<Class<?>, ClassExecuteHandler> executeHandleMap = new HashMap<>();
        //缓存代理注解和代理类的关系
        Multimap<Class<? extends Annotation>, Class<?>> pendingAnnotationMap = ArrayListMultimap.create();
        for (Class<?> clazz : executeHandlerList) {

            try {
                ClassExecuteHandler classExecuteHandler = (ClassExecuteHandler) clazz.getDeclaredConstructor().newInstance();
                executeHandleMap.put(clazz, classExecuteHandler);
                Class<? extends Annotation>[] annotationClasses = classExecuteHandler.targetAnnotation();
                for (Class<? extends Annotation> aClass : annotationClasses) {
                    pendingAnnotationMap.put(aClass, clazz);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            }
        }
        for (Class<?> aClass : pendingClassMap.keySet()) {
            Collection<Class<? extends Annotation>> classes = pendingClassAnnotationMap.get(aClass);
            Set<Class<?>> set = new HashSet<>();
            List<ClassExecuteHandler> classExecuteHandlers = new ArrayList<>();
            for (Class<? extends Annotation> annotationClass : classes) {
                Collection<Class<?>> classes1 = pendingAnnotationMap.get(annotationClass);
                set.addAll(classes1);
            }
            for (Class<?> aClass1 : set) {
                classExecuteHandlers.add(executeHandleMap.get(aClass1));
            }
            Object bean = getBean(aClass);
            BeanClassProxy beanClassProxy = new BeanClassProxy(classExecuteHandlers, bean, pendingClassMap.get(aClass));
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(aClass);
            enhancer.setCallback(beanClassProxy);
            Object o = enhancer.create();
            addWithClassObject(aClass, o);
        }

        pendingClassAnnotationMap.clear();
        executeHandlerList.clear();
        pendingClassMap.clear();
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
        //如果方法上有注解,需要先缓存起来,看是否有代理类
        for (Method method : clazz.getMethods()) {
            if (Object.class.equals(method.getDeclaringClass())) {
                continue;
            }
            Annotation[] methodAnnotations = method.getAnnotations();
            boolean isAdd = false;
            for (Annotation methodAnnotation : methodAnnotations) {
                if (methodAnnotation.annotationType().getName().startsWith("java")) {
                    continue;
                }
                if (methodAnnotation.annotationType() == Bean.class) {
                    beanMethodList.add(method);
                    continue;
                }
                if (methodAnnotation.annotationType() == Init.class) {
                    pendingInitMap.put(clazz, method);
                    continue;
                }
                if (methodAnnotation.annotationType() == Destroy.class) {
                    pendingDestroyMap.put(clazz, method);
                    continue;
                }
                pendingClassAnnotationMap.put(clazz, methodAnnotation.annotationType());
                isAdd = true;
            }
            if (isAdd) {
                pendingClassMap.put(clazz, method);
            }

        }
    }


    static void addBeanWithClass(String beanName, Class<?> clazz) {

        if (StringUtils.isEmpty(beanName)) {
            beanName = StringUtils.uncapitalize(clazz.getSimpleName());
        }
        try {
            Object o = clazz.getDeclaredConstructor().newInstance();
            addBean(clazz, o, beanName);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            //TODO
        }


    }

    public static void addWithClassObject(Class<?> clazz, Object obj) {
        String beanName = StringUtils.uncapitalize(clazz.getSimpleName());
        addBean(clazz, obj, beanName);

    }

    private static void addBean(Class<?> clazz, Object obj, String beanName) {
        PackageObject packageObject = new PackageObject(beanName, obj);
        beanWithNameMap.put(clazz, beanName, packageObject);
        beanWithClassMap.put(clazz, clazz, packageObject);
        List<Class<?>> superClass = getSuperClass(clazz);
        superClass.addAll(getSuperInterface(clazz));
        for (Class<?> aClass : superClass) {
            beanWithClassMap.put(aClass, clazz, packageObject);
            beanWithNameMap.put(aClass, beanName, packageObject);
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
