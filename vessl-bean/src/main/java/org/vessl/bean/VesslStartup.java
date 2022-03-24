package org.vessl.bean;

import java.lang.reflect.InvocationTargetException;

public class VesslStartup {
    ClassScanner classScanner = new ClassScanner();

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
        classScanner.classDestroyHandle();
        BeanStore.invokeDestroy();
        shutdown();
    }
    public void shutdown(){

    }
}
