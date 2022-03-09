package org.vessl.sql.annotation;

import java.lang.annotation.*;


/**
 * 为形能定义参数名
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Name {
    String value() default "";
}
