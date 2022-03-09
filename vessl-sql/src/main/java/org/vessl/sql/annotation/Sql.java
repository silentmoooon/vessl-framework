package org.vessl.sql.annotation;

import org.vessl.sql.constant.SqlType;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sql {
    String value() default "";
    SqlType type() default SqlType.SELECT;
}
