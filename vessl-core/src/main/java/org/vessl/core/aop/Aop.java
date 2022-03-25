package org.vessl.core.aop;

import org.vessl.core.bean.Bean;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Bean
@Documented
public @interface Aop {
}
