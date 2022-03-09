package org.vessl.bean;

import java.lang.reflect.InvocationTargetException;

public class Startup {
    ClassScanner classScanner = new ClassScanner();

    public void startup() {
        try {
            classScanner.scan();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdownHook();
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }));
        } catch (InvocationTargetException | IllegalAccessException e) {
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
