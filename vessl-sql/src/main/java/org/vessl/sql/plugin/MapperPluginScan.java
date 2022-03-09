package org.vessl.sql.plugin;

import org.vessl.bean.ClassScanHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class MapperPluginScan implements ClassScanHandler {
    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Plugin.class};
    }

    @Override
    public void handleBefore(List<Class<?>> classes)  {
        for (Class<?> aClass : classes) {
            Plugin annotation = aClass.getAnnotation(Plugin.class);
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
