package org.vessl.core.bean.config;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value() default "";
}
