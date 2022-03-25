package org.vessl.sql.handle;

import com.zaxxer.hikari.HikariConfig;
import org.vessl.core.spi.ClassScanPlugin;
import org.vessl.core.bean.Order;
import org.vessl.core.bean.config.Value;
import org.vessl.core.spi.Plugin;
import org.vessl.sql.annotation.Mapper;

import java.lang.annotation.Annotation;
import java.util.List;
@Plugin
@Order(1)
public class MapperClassScanPlugin implements ClassScanPlugin {

    @Value("${sql.jdbcUrl}")
    private String jdbcUrl;
    @Value("${sql.username}")
    private String username;
    @Value("${sql.password}")
    private String password;
    @Value("${sql.cachePrepStmts:true}")
    private String cachePrepStmts;
    @Value("${sql.prepStmtCacheSize:250}")
    private String prepStmtCacheSize;
    @Value("${sql.prepStmtCacheSqlLimit:2048}")
    private String prepStmtCacheSqlLimit;
    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Mapper.class};
    }

    @Override
    public void handleBefore(List<Class<?>> classes) {
        for (Class<?> aClass : classes) {
            MapperManager.addMapper(aClass);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", cachePrepStmts);
        config.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
        MapperManager.initMapperProxyAndDatasource(config);
    }
}
