package org.vessl.core.bean;

import org.apache.commons.lang3.StringUtils;
import org.vessl.core.aop.Aop;
import org.vessl.core.aop.AopHandler;
import org.vessl.core.aop.ClassMethodAnnotation;
import org.vessl.core.aop.ExecuteInterceptor;
import org.vessl.core.bean.config.Value;
import org.vessl.core.bean.config.YamlScanPlugin;
import org.vessl.core.spi.BasePluginScanPlugin;
import org.vessl.core.spi.Plugin;
import org.vessl.core.spi.PluginHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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

    private BeanStore beanStore;
    private PluginHandler pluginHandler;
    private AopHandler aopHandler;

    public ClassScanner(BeanStore beanStore, PluginHandler pluginHandler, AopHandler aopHandler) {

        this.beanStore = beanStore;
        this.pluginHandler = pluginHandler;
        this.aopHandler = aopHandler;

        pluginHandler.addBasePlugin(new YamlScanPlugin());
        pluginHandler.addBasePlugin(new BasePluginScanPlugin(this));
    }


    public void init() throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        scanClass();

        pluginHandler.baseFileScan();

        pluginHandler.fileScan();

        pluginHandler.classScanning();

        aopHandler.initAop();

        beanStore.inject();

        beanStore.setConfigValue();

        beanStore.invokeInit();

        beanStore.scanWithBeanMethod();

        pluginHandler.classScanEnd();
    }

    public void scanClass() throws ClassNotFoundException {
        String mainClassName = "";
        String packageName = "";
        String manifestPath = "META-INF/MANIFEST.MF";
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(manifestPath);
        if (inputStream != null) {
            try {
                Manifest manifest = new Manifest(inputStream);
                Attributes entries = manifest.getMainAttributes();
                String manifestAttrMain = "Main-Class";
                mainClassName = entries.getValue(manifestAttrMain);
            } catch (IOException ignored) {

            }
        }
        if (StringUtils.isEmpty(mainClassName)) {
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            mainClassName = stackTrace[stackTrace.length - 1].getClassName();

        }
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(mainClassName);
        Scan annotation = aClass.getAnnotation(Scan.class);
        if (annotation != null && StringUtils.isNotEmpty(annotation.value())) {
            packageName = annotation.value();
        }
        if (StringUtils.isEmpty(packageName)) {

            String[] split = mainClassName.split("\\.");
            if (split.length == 2) {
                packageName = split[0];
            }
            if (split.length > 2) {
                packageName = split[0] + "." + split[1];
            }

        }
        String[] split = packageName.split(",");
        for (String s : split) {
            scanClass(s);
        }

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

    public void collectionClass(Class<?> clazz) {

        if (clazz.isEnum()) {
            return;
        }
        if (clazz.isAnnotation()) {
            return;
        }
        if (clazz.getAnnotation(Plugin.class) != null) {
            pluginHandler.addPlugin(clazz);
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
            if (!annotation.annotationType().getCanonicalName().startsWith("java.")) {
                pluginHandler.addAnnotationClassMap(annotation.annotationType(), clazz);
            }

        }

        if (isBean) {
            isNeedInject(clazz);
            isNeedSetValue(clazz);
            filterMethod(clazz);
            isAopClass(clazz);
            if (clazz.isInterface()) {
                return;
            }
            if (Modifier.isAbstract(clazz.getModifiers())) {
                return;
            }


            beanStore.addBean(beanName, clazz);
        }


    }

    private void isNeedInject(Class<?> clazz) {
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
                    beanStore.addPendingInject(clazz, field);
                }
            }
        }
    }

    private void isNeedSetValue(Class<?> clazz) {
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
                    beanStore.addPendingSetValue(clazz, field);
                }
            }
        }
    }


    private void filterMethod(Class<?> clazz) {
        if (ExecuteInterceptor.class.isAssignableFrom(clazz)) {
            return;

        }
        //如果方法上有注解,需要先缓存起来,看是否有代理类
        for (Method method : clazz.getMethods()) {
            if (Object.class.equals(method.getDeclaringClass())) {
                continue;
            }
            Annotation[] methodAnnotations = method.getAnnotations();
            ClassMethodAnnotation mAnnotation = new ClassMethodAnnotation(clazz);
            for (Annotation methodAnnotation : methodAnnotations) {
                Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
                if (annotationType.getName().startsWith("java")) {
                    continue;
                }
                if (annotationType == Bean.class) {
                    beanStore.addBeanMethod(method);
                    continue;
                }
                if (annotationType == Init.class) {
                    beanStore.addPendingInit(clazz, method);
                    continue;
                }
                if (annotationType == Destroy.class) {
                    beanStore.addPendingDestroy(clazz, method);
                    continue;
                }

                mAnnotation.add(method, annotationType);

            }
            if (!mAnnotation.isEmpty()) {
                aopHandler.addClassAnnotationMethod(mAnnotation);
            }

        }
    }

    public void isAopClass(Class<?> clazz) {
        Aop aop = clazz.getDeclaredAnnotation(Aop.class);
        if (aop != null) {
            aopHandler.addExecuteInterceptor(clazz);

        }
    }


}