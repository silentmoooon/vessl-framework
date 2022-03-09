package org.vessl.sql.plugin;

import org.vessl.bean.Bean;
import org.vessl.sql.plugin.PluginType;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {
    PluginType value() default PluginType.EXECUTE;
}
