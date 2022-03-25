package org.vessl.core.bean;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {
    String value() default "";

    boolean required() default true;
}
