package org.vessl.core.bean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import net.sf.cglib.core.ReflectUtils;
import org.apache.commons.lang3.StringUtils;
import org.vessl.core.bean.config.ConfigManager;
import org.vessl.core.bean.config.Value;

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
    /**
     * 按类型保存 r父类,c类本身 v对象
     */
    private static ArrayListMultimap<Class<?>, PackageObject> beanWithClassMap = ArrayListMultimap.create();


    /**
     * 被@Bean注解的方法
     */
    private static List<Method> beanMethodList = new ArrayList<>();


    /**
     * 依赖注入字段
     */
    private static Multimap<Class<?>, Field> pendingInjectMap = ArrayListMultimap.create();
    /**
     * 配置注入字段
     */
    private static Multimap<Class<?>, Field> pendingSetValueMap = ArrayListMultimap.create();
    /**
     * init方法
     */
    private static Multimap<Class<?>, Method> pendingInitMap = ArrayListMultimap.create();
    /**
     * destroy方法
     */
    private static Multimap<Class<?>, Method> pendingDestroyMap = ArrayListMultimap.create();


    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2,
                60L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(100));
        addBean(ThreadPoolExecutor.class, threadPoolExecutor);
    }


    public static <T> T getBean(Class<T> tClass) {

        List<PackageObject> packageObjects = beanWithClassMap.get(tClass);
        if (packageObjects.size() > 0) {
            return (T) packageObjects.get(packageObjects.size() - 1).getObject();
        }

        return null;
    }

    static <T> T getOriginBean(Class<T> tClass) {

        List<PackageObject> packageObjects = beanWithClassMap.get(tClass);
        if (packageObjects.size() == 0) {
            return null;
        }
        PackageObject packageObject = packageObjects.get(packageObjects.size() - 1);
        if (packageObject.isProxy) {
            return (T) packageObject.getTarget();
        }
        return (T) packageObject.getObject();


    }

    public static <T> T getBean(String name) {

        PackageObject packageObject = beanWithNameMap.get(name);
        if (packageObject != null) {
            return (T) packageObject.getObject();
        }
        return null;
    }


    /**
     * 依赖注入
     */
    void inject() {
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

    //为@value注解的字段赋值
    void setConfigValue() {
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

    private <T> Map<String, T> getFileValueWithMap(Class<?> clazz) {
        Map<String, T> map = new HashMap<>();
        List<PackageObject> rows = beanWithClassMap.get(clazz);
        for (PackageObject value : rows) {
            map.put(value.getBeanName(), (T) value.getObject());
        }

        return map;

    }

    private <T> List<T> getFileValueWithList(Class<?> clazz) {
        List<T> list = new ArrayList<>();
        List<PackageObject> rows = beanWithClassMap.get(clazz);
        for (PackageObject value : rows) {
            list.add((T) value.getObject());
        }

        return list;

    }


    public void setConfigValue(Field field, Object object) {
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

    private Object getValueFromConfig(String value) {
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

    /**
     * 调用@Init注解的方法
     *
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    void invokeInit() throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<Class<?>, Method> entry : pendingInitMap.entries()) {
            Object targetClass = getBean(entry.getKey());
            boolean flag = entry.getValue().canAccess(targetClass);
            entry.getValue().setAccessible(true);
            entry.getValue().invoke(targetClass);
            entry.getValue().setAccessible(flag);
        }
    }

    public void invokeDestroy() throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<Class<?>, Method> entry : pendingDestroyMap.entries()) {
            Object targetClass = getBean(entry.getKey());
            boolean flag = entry.getValue().canAccess(targetClass);
            entry.getValue().setAccessible(true);
            entry.getValue().invoke(targetClass);
            entry.getValue().setAccessible(flag);
        }
    }

    /**
     * 从@Bean注解的方法加载类
     */
    void scanWithBeanMethod() {
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
                    addBean(result.getClass(), result, beanName);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    //TODO
                }
            }

        }

    }

    public void addBeanMethod(Method method) {
        beanMethodList.add(method);
    }

    public void addPendingInit(Class<?> clazz, Method method) {
        pendingInitMap.put(clazz, method);
    }

    public void addPendingDestroy(Class<?> clazz, Method method) {
        pendingDestroyMap.put(clazz, method);
    }

    public void addPendingSetValue(Class<?> clazz, Field field) {
        pendingSetValueMap.put(clazz, field);
    }

    public void addPendingInject(Class<?> clazz, Field field) {
        pendingInjectMap.put(clazz, field);
    }


    public void addBean(String beanName, Class<?> clazz) {

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


    public static void addBean(Class<?> clazz, Object obj) {
        String beanName = clazz.getName();
        addBean(clazz, obj, beanName);

    }

    private static void addBean(Class<?> clazz, Object obj, String beanName) {
        PackageObject packageObject = new PackageObject(beanName, obj);
        beanWithNameMap.put(beanName, packageObject);
        beanWithClassMap.put(clazz, packageObject);
        List<Class<?>> superClass = getSuperClassAndInterface(clazz);
        if (clazz.equals(ThreadPoolExecutor.class)) {
            for (Class<?> aClass : superClass) {
                System.out.println(aClass.getName());
            }
        }
        for (Class<?> aClass : superClass) {
            beanWithClassMap.put(aClass, packageObject);
        }
    }

    public static void addProxyBean(Class<?> clazz, Object obj, String beanName) {
        PackageObject object = beanWithNameMap.get(beanName);
        PackageObject packageObject = new PackageObject(beanName, obj);
        if (object != null) {
            packageObject.setProxy(true);
            packageObject.setTarget(object.getObject());
        }
        beanWithNameMap.put(beanName, packageObject);
        beanWithClassMap.put(clazz, packageObject);
        List<Class<?>> superClass = getSuperClassAndInterface(clazz);
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
    private static List<Class<?>> getSuperClassAndInterface(Class<?> clazz) {
        List<Class<?>> clazzs = new ArrayList<>();
        Class<?> suCl = clazz.getSuperclass();
        if (suCl != null && suCl != Object.class) {
            clazzs.add(suCl);
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        clazzs.addAll(Arrays.asList(interfaces));

        clazzs.stream().toList().forEach(aClass -> {
            clazzs.addAll(getSuperClassAndInterface(aClass));
        });

        return clazzs;
    }


}
