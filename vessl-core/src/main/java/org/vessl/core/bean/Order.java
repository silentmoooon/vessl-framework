package org.vessl.core.bean;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Order {
    int value() default 1024;
}
