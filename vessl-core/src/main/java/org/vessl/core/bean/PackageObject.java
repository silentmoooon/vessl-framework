package org.vessl.core.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PackageObject {
    private String beanName;
    private  Object object;
    boolean isProxy;
    private Object target;

    public PackageObject(String beanName, Object object) {
        this.beanName=beanName;
        this.object=object;
    }

}
