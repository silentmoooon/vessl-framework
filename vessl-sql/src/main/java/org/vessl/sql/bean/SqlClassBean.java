package org.vessl.sql.bean;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class SqlClassBean {
    private String name;
    private List<SqlMethodBean> methods = new ArrayList<>();
}
