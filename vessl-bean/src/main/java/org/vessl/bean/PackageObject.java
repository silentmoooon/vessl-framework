package org.vessl.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PackageObject {
    private String beanName;
    private  Object object;
}
