package org.vessl.sql.tx;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {
    /**
     * datasourceName
     * @return
     */
    String value() default "";
}
