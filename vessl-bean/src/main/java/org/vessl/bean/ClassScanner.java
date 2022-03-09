package org.vessl.bean;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.vessl.bean.config.Value;
import org.vessl.bean.config.YamlScanHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 扫描包下路径
 * 包括本地文件和jar包文件
 *
 * @author ljb
 */
public class ClassScanner {

    private final FileScanner fileScanner = new FileScanner();
    public static final int DEFAULT_ORDER = 1025;


    /**
     * 缓存扫描到的类(扫描处理类除外)
     */
    private static ArrayListMultimap<Class<? extends Annotation>, Class<?>> annotationClassMap = ArrayListMultimap.create();

    /**
     * 缓存扫描处理类
     */
    private static List<Class<?>> classScanHandleList = new ArrayList<>();

    /**
     * 缓存扫描处理类
     */
    private static Map<Class<?>, ClassScanHandler> classScanHandleObjectMap = new HashMap<>();


    /**
     * 缓存文件扫描处理类
     */
    private static List<Class<?>> fileScanHandlerList = new ArrayList<>();

    private static List<Class<?>> baseFileScanHandlerList = Arrays.asList(BaseClassScanHandler.class, YamlScanHandler.class);


    static void addHandle(Class<?> clazz){

        if(ClassScanHandler.class.isAssignableFrom(clazz)) {
            if (!classScanHandleList.contains(clazz)) {
                classScanHandleList.add(clazz);
            }

        } else if (FileScanHandler.class.isAssignableFrom(clazz)) {
            if(!fileScanHandlerList.contains(clazz)) {
                fileScanHandlerList.add(clazz);
            }
        } else if (ClassExecuteHandler.class.isAssignableFrom(clazz)) {
            BeanStore.addExecuteHandle(clazz);

        }
    }
    //----- 代理相关


    public void scan() throws InvocationTargetException, IllegalAccessException {
        scan("");
    }

    public void scan(String packageName) throws InvocationTargetException, IllegalAccessException {

        if (StringUtils.isEmpty(packageName)) {
            String MANIFEST_PATH = "META-INF/MANIFEST.MF";
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(MANIFEST_PATH);
            if (inputStream != null) {
                try {
                    Manifest manifest = new Manifest(inputStream);
                    Attributes entries = manifest.getMainAttributes();
                    String MANIFEST_ATTR_MAIN = "Main-Class";
                    packageName = entries.getValue(MANIFEST_ATTR_MAIN);
                } catch (IOException ignored) {

                }
            }
            if (StringUtils.isEmpty(packageName)) {
                StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
                packageName = stackTrace[stackTrace.length - 1].getClassName();

            }
            String[] split = packageName.split("\\.");
            if (split.length == 2) {
                packageName = split[0];
            }
            if (split.length > 2) {
                packageName = split[0] + "." + split[1];
            }
            scanClass(packageName);
        } else if (packageName.contains(",")) {
            String[] split = packageName.split(",");
            for (String s : split) {
                scanClass(s);
            }
        } else {
            scanClass(packageName);
        }

        //基本文件扫描,加载配置文件和SPI声明的handler类
        baseFileScanHandle();
        //文件扫描
        fileScanHandle();

        /**
         * 类扫描
         */
        classScanningHandle();

        /**
         * 从@Bean注解的方法初化类
         */
        BeanStore.initBeanMethod();
        /**
         * 依赖注入
         */
        BeanStore.inject();
        //为@value注解的字段赋值
        BeanStore.setFiledValue();
        //调用@Init注解的方法
        BeanStore.invokeInit();
        /**
         * 设置proxy
         */
        BeanStore.executeHandle();

        /**
         * 类扫描完成
         */
        classScanEndHandle();

    }


    private void baseFileScanHandle(){

        for (Class<?> fileScan : baseFileScanHandlerList) {
            try {

                FileScanHandler scanHandler = (FileScanHandler) fileScan.getDeclaredConstructor().newInstance();
                String path = scanHandler.getPath();
                if (StringUtils.isNotEmpty(path)) {
                    fileScanner.scan(scanHandler, path);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            }
        }

    }

    private void classScanningHandle() {
        classScanHandleList.sort(BeanOrder::order);
        for (Class<?> clazz : classScanHandleList) {
            try {

                ClassScanHandler o = (ClassScanHandler) clazz.getDeclaredConstructor().newInstance();
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if(declaredField.getAnnotation(Value.class)!=null){
                        BeanStore.setFiledValue(declaredField,o);
                    }
                }
                classScanHandleObjectMap.put(clazz, o);
                Class<? extends Annotation>[] classes = o.targetAnnotation();
                for (Class<? extends Annotation> aClass : classes) {
                    o.handleBefore(annotationClassMap.get(aClass));
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            }
        }
    }

    private void classScanEndHandle() {


        for (Class<?> clazz : classScanHandleList) {
            ClassScanHandler o = classScanHandleObjectMap.get(clazz);

            Class<? extends Annotation>[] annotations = o.targetAnnotation();
            for (Class<? extends Annotation> annotationClass : annotations) {
                List<Class<?>> classes = annotationClassMap.get(annotationClass);
                Map<Class<?>, Object> objectMap = new HashMap<>();
                for (Class<?> aClass : classes) {
                    Object bean = BeanStore.getBean(aClass);
                    objectMap.put(aClass, bean);
                }
                o.handleAfter(objectMap);
            }

        }
        annotationClassMap.clear();
    }

    void classDestroyHandle() {
        for (Class<?> clazz : classScanHandleList) {
            ClassScanHandler o = classScanHandleObjectMap.get(clazz);
            o.handleDestroy();

        }
        classScanHandleList.clear();
        classScanHandleObjectMap.clear();
    }

    private void fileScanHandle() {
        fileScanHandlerList.sort(BeanOrder::order);
        for (Class<?> fileScan : fileScanHandlerList) {
            try {
                FileScanHandler scanHandler = (FileScanHandler) fileScan.getDeclaredConstructor().newInstance();
                Field[] declaredFields = fileScan.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if(declaredField.getAnnotation(Value.class)!=null){
                        BeanStore.setFiledValue(declaredField,scanHandler);
                    }
                }

                String path = scanHandler.getPath();
                if (StringUtils.isNotEmpty(path)) {
                    fileScanner.scan(scanHandler, path);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            }
        }


        fileScanHandlerList.clear();
    }


    /**
     * 获取包下所有实现了superStrategy的类并加入list
     */
    private void scanClass(String packageName) {
        ArrayList<URL> list = new ArrayList<>();
        try {
            list = Collections.list(Thread.currentThread().getContextClassLoader().getResources(packageName.replace('.', '/')));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (URL url : list) {
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                // 本地自己可见的代码
                try {
                    findClassLocal(url.toURI(), packageName);
                } catch (URISyntaxException e) {
                    //TODO WARN
                }
            } else if ("jar".equals(protocol)) {
                // 引用jar包的代码
                findClassJar(url, packageName);
            }

        }

    }


    /**
     * 本地查找
     *
     * @param packName
     */
    private void findClassLocal(final URI url, final String packName) {

        File file = new File(url);

        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                findClassLocal(f.toURI(), packName + "." + f.getName());
            } else if (f.getName().endsWith(".class")) {
                Class<?> clazz = null;
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(packName.replace("/", ".") + "." + f.getName().replace(".class", ""));
                } catch (ClassNotFoundException e) {
                    return;
                }
                collectionClass(clazz);
            }
        }


    }

    /**
     * jar包查找
     *
     * @param packName
     */
    private void findClassJar(URL url, String packName) {

        JarFile jarFile = null;
        try {
            JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
            jarFile = jarURLConnection.getJarFile();
        } catch (IOException e) {
            throw new RuntimeException("未找到策略资源");
        }

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarEntryName = jarEntry.getName();

            String tmpPackName = packName.replace(".", "/");
            if (jarEntryName.endsWith(".class") && jarEntryName.startsWith(tmpPackName)) {
                Class<?> clazz = null;
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(jarEntry.getName().replace("/", ".").replace(".class", ""));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
                collectionClass(clazz);
            }

        }

    }

    private void collectionClass(Class<?> clazz) {
        if(baseFileScanHandlerList.contains(clazz)){
            return;
        }
        if (!clazz.isInterface() && FileScanHandler.class.isAssignableFrom(clazz)) {
            fileScanHandlerList.add(clazz);
        } else if (!clazz.isInterface() && ClassScanHandler.class.isAssignableFrom(clazz)) {
            classScanHandleList.add(clazz);
        } else if (!clazz.isInterface() && ClassExecuteHandler.class.isAssignableFrom(clazz)) {
            BeanStore.addExecuteHandle(clazz);

        } else {
            if (clazz.isEnum()) {
                return;
            }
            if (clazz.isAnnotation()) {
                BeanStore.addAnnotationMap(clazz);
                return;
            }


            Annotation[] annotations = clazz.getAnnotations();
            boolean isBean = false;
            String beanName = null;
            for (Annotation annotation : annotations) {
                if (annotation instanceof Bean bean) {
                    beanName = bean.value();
                    isBean = true;

                } else {
                    Bean beanAnnotation = annotation.annotationType().getAnnotation(Bean.class);
                    if (beanAnnotation != null) {
                        beanName = beanAnnotation.value();
                        isBean = true;
                    }

                }
                annotationClassMap.put(annotation.annotationType(), clazz);
            }
            if (isBean) {
                BeanStore.add(beanName, clazz);
            }

        }
    }


}