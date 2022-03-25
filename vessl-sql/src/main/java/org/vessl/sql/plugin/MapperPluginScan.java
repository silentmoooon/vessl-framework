package org.vessl.sql.plugin;


import org.vessl.base.spi.ClassScanPlugin;
import org.vessl.base.spi.Plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
@Plugin
public class MapperPluginScan implements ClassScanPlugin {
    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{MapperPlugin.class};
    }

    @Override
    public void handleBefore(List<Class<?>> classes)  {
        for (Class<?> aClass : classes) {
            MapperPlugin annotation = aClass.getAnnotation(MapperPlugin.class);
            Object o = null;
            try {
                o = aClass.getDeclaredConstructor().newInstance();
                PluginManager.addPlugin(annotation.value(),(PluginInterceptor) o);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                //TODO
            }

        }
    }
}
