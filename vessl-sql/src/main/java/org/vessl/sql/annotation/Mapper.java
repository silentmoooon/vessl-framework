package org.vessl.sql.annotation;


import org.vessl.core.bean.Bean;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Bean
public @interface Mapper {
}
