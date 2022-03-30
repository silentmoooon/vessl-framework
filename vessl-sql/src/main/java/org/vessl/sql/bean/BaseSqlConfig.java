package org.vessl.sql.bean;

import lombok.Data;

@Data
public class BaseSqlConfig {
    private String datasourceName;
    private String basePackage;
    private String jdbcUrl;
    private String username;
    private String password;
    private String cachePrepStmts;
    private String prepStmtCacheSize;
    private String prepStmtCacheSqlLimit;
}
