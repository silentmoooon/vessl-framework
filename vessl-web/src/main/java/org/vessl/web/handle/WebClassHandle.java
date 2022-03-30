package org.vessl.web.handle;


import org.vessl.core.bean.config.Value;
import org.vessl.core.spi.ClassScanPlugin;
import org.vessl.core.spi.Plugin;
import org.vessl.web.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
@Plugin
public class WebClassHandle implements ClassScanPlugin {

    @Value("${web.port}")
    private int port;
    WebServer webServer = new WebServer();

    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Web.class};
    }

    @Override
    public void handleAfter(Map<Class<?>, Object> objectMap) {
        objectMap.forEach((aClass, o) -> {
            Web ca = aClass.getAnnotation(Web.class);
            String basePath = ca.value();
            Method[] methods = aClass.getMethods();
            for (Method declaredMethod : methods) {
                if (Object.class.equals(declaredMethod.getDeclaringClass())) {
                    continue;
                }
                Get get = declaredMethod.getAnnotation( Get.class);
                if (get != null) {
                    System.out.println(RequestMethod.GET + "===" + "/" + basePath + "/" + get.value().replaceAll("//", "/"));
                    WebPathHandleMapping.register(RequestMethod.GET, ("/" + basePath + "/" + get.value()).replaceAll("//", "/"), o, declaredMethod);

                }
                Post post = declaredMethod.getAnnotation( Post.class);
                if (post != null) {
                    System.out.println(RequestMethod.POST + "===" + "/" + basePath + "/" + get.value().replaceAll("//", "/"));
                    WebPathHandleMapping.register(RequestMethod.POST, ("/" + basePath + "/" + get.value()).replaceAll("//", "/"), o, declaredMethod);

                }
                HttpMethod httpMethod = declaredMethod.getAnnotation( HttpMethod.class);
                if (httpMethod != null) {
                    for (RequestMethod requestMethod : httpMethod.method()) {
                        System.out.println(requestMethod + "===" + "/" + basePath + "/" + get.value().replaceAll("//", "/"));
                        WebPathHandleMapping.register(requestMethod, ("/" + basePath + "/" + get.value()).replaceAll("//", "/"), o, declaredMethod);
                    }

                }

            }
        });
        webServer.setPort(port);
        webServer.init();

    }

    @Override
    public void handleDestroy() {
        webServer.destroy();
    }
}
