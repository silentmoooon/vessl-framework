package org.vessl.core;

import org.vessl.core.aop.AopHandler;
import org.vessl.core.bean.BeanStore;
import org.vessl.core.bean.ClassScanner;
import org.vessl.core.spi.PluginHandler;

import java.lang.reflect.InvocationTargetException;

public class VesslStartup {
    BeanStore beanStore = new BeanStore();
    PluginHandler pluginHandler = new PluginHandler(beanStore);
    AopHandler aopHandler = new AopHandler();
    ClassScanner classScanner = new ClassScanner(beanStore, pluginHandler, aopHandler);


    public void startup() {
        try {
            classScanner.init();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdownHook();
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }));
        } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void shutdownHook() throws InvocationTargetException, IllegalAccessException {
        pluginHandler.classDestroyHandle();
        beanStore.invokeDestroy();
        shutdown();
    }

    public void shutdown() {

    }
}
