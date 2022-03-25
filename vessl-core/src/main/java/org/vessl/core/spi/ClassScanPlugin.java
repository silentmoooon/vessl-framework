package org.vessl.core.spi;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public interface ClassScanPlugin {
    Class<? extends Annotation>[] targetAnnotation();
    default void handleBefore(List<Class<?>> classes){

    }
    default void handleAfter(Map<Class<?>,Object> objectMap){

    }

    default void handleDestroy() {

    }
}
