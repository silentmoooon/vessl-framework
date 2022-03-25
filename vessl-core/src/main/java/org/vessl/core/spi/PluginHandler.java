package org.vessl.core.spi;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.vessl.core.bean.BeanOrder;
import org.vessl.core.bean.BeanStore;
import org.vessl.core.bean.config.FileScanner;
import org.vessl.core.bean.config.Value;
import org.vessl.core.bean.config.YamlScanPlugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PluginHandler {

    private static final FileScanner fileScanner = new FileScanner();
    /**
     * 缓存扫描到的类(plugin类除外)
     */
    private static ArrayListMultimap<Class<? extends Annotation>, Class<?>> annotationClassMap = ArrayListMultimap.create();

    /**
     * 缓存类扫描插件类
     */
    private static List<Class<?>> classScanHandleList = new ArrayList<>();

    /**
     * 缓存类扫描插件实例
     */
    private static Map<Class<?>, ClassScanPlugin> classScanHandleObjectMap = new HashMap<>();

    /**
     * 缓存文件扫描插件类
     */
    private static List<Class<?>> fileScanHandlerList = new ArrayList<>();

    /**
     * 基本插件
     */
    private static List<Class<?>> baseFileScanHandlerList = Arrays.asList(BasePluginScanPlugin.class, YamlScanPlugin.class);

    public static void addAnnotationClassMap(Class<? extends Annotation> annotationClass, Class<?> clazz){
        annotationClassMap.put(annotationClass, clazz);
    }
    public static void addPlugin(Class<?> clazz) {

        if (ClassScanPlugin.class.isAssignableFrom(clazz)) {
            if (!classScanHandleList.contains(clazz)) {
                classScanHandleList.add(clazz);
            }

        } else if (FileScanPlugin.class.isAssignableFrom(clazz)) {
            if (!fileScanHandlerList.contains(clazz)) {
                fileScanHandlerList.add(clazz);
            }
        }
    }

    /**
     * 基本文件扫描,加载配置文件和SPI声明的handler类
     */
    public static void baseFileScan() {

        for (Class<?> fileScan : baseFileScanHandlerList) {
            try {

                FileScanPlugin scanHandler = (FileScanPlugin) fileScan.getDeclaredConstructor().newInstance();
                String path = scanHandler.getPath();
                if (StringUtils.isNotEmpty(path)) {
                    fileScanner.scan(scanHandler, path);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            }
        }

    }

    /**
     * 类扫描
     */
    public static void classScanning() {
        classScanHandleList.sort(BeanOrder::order);
        for (Class<?> clazz : classScanHandleList) {
            try {

                ClassScanPlugin o = (ClassScanPlugin) clazz.getDeclaredConstructor().newInstance();
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.getAnnotation(Value.class) != null) {
                        BeanStore.setConfigValue(declaredField, o);
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

    public static void classScanEnd() {


        for (Class<?> clazz : classScanHandleList) {
            ClassScanPlugin o = classScanHandleObjectMap.get(clazz);

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

    public static void classDestroyHandle() {
        for (Class<?> clazz : classScanHandleList) {
            ClassScanPlugin o = classScanHandleObjectMap.get(clazz);
            o.handleDestroy();

        }
        classScanHandleList.clear();
        classScanHandleObjectMap.clear();
    }

    public static void fileScan() {
        fileScanHandlerList.sort(BeanOrder::order);
        for (Class<?> fileScan : fileScanHandlerList) {
            try {
                FileScanPlugin scanHandler = (FileScanPlugin) fileScan.getDeclaredConstructor().newInstance();
                Field[] declaredFields = fileScan.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.getAnnotation(Value.class) != null) {
                        BeanStore.setConfigValue(declaredField, scanHandler);
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

}
