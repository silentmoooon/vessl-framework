package org.vessl.sql.plugin;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapperPlugin {
    PluginType value() default PluginType.EXECUTE;
}
