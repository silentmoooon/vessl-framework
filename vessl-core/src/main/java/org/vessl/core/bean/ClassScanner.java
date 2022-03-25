package org.vessl.core.bean;

import org.apache.commons.lang3.StringUtils;
import org.vessl.core.aop.AopHandler;
import org.vessl.core.spi.PluginHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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





    public void init() throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        scanClass();

        PluginHandler.baseFileScan();

        PluginHandler.fileScan();

        PluginHandler.classScanning();

        AopHandler.initAop();

        BeanStore.inject();

        BeanStore.setConfigValue();

        BeanStore.invokeInit();

        BeanStore.scanWithBeanMethod();

        PluginHandler.classScanEnd();
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
                BeanStore.collectionClass(clazz);
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
                BeanStore.collectionClass(clazz);
            }

        }

    }


}