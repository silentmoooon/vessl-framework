package org.vessl.core.bean;

public class BeanOrder {
    public static final int DEFAULT_ORDER = 1025;

    public static int order(Object o1, Object o2) {
        Order o1Annotation = o1.getClass().getAnnotation(Order.class);
        int o1Order = DEFAULT_ORDER;
        if (o1Annotation != null) {
            o1Order = o1Annotation.value();
        }
        Order o2Annotation = o2.getClass().getAnnotation(Order.class);
        int o2Order = DEFAULT_ORDER;
        if (o2Annotation != null) {
            o2Order = o2Annotation.value();
        }
        return Integer.compare(o1Order, o2Order);
    }

    public static int reverse(Object o1, Object o2) {
        Order o1Annotation = o1.getClass().getAnnotation(Order.class);
        int o1Order = DEFAULT_ORDER;
        if (o1Annotation != null) {
            o1Order = o1Annotation.value();
        }
        Order o2Annotation = o2.getClass().getAnnotation(Order.class);
        int o2Order = DEFAULT_ORDER;
        if (o2Annotation != null) {
            o2Order = o2Annotation.value();
        }
        return Integer.compare(o2Order, o1Order);
    }
}
