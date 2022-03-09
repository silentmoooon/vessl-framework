package org.vessl.web.annotation;


import org.vessl.bean.Bean;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Bean
public @interface Web {
    String value() default "";
}
