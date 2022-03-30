package org.vessl.sql.handle;

import com.zaxxer.hikari.HikariConfig;
import org.apache.commons.lang3.StringUtils;
import org.vessl.core.bean.BeanStore;
import org.vessl.core.bean.Order;
import org.vessl.core.bean.config.Value;
import org.vessl.core.spi.ClassScanPlugin;
import org.vessl.core.spi.Plugin;
import org.vessl.sql.annotation.Mapper;
import org.vessl.sql.bean.BaseSqlConfig;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Plugin
@Order(1)
public class MapperClassScanPlugin implements ClassScanPlugin {

    @Value("${sql.jdbcUrl:}")
    private String jdbcUrl;
    @Value("${sql.username:}")
    private String username;
    @Value("${sql.password:}")
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

        MapperManager.initMapperBean();
    }

    @Override
    public void handleAfter(Map<Class<?>, Object> objectMap) {
        List<BaseSqlConfig> beans = BeanStore.getBeans(BaseSqlConfig.class);
        if (beans.size() == 0) {
            if(jdbcUrl.isEmpty()||username.isEmpty()||password.isEmpty()){
                throw new RuntimeException("the jdbcUrl username password is required");
            }
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", cachePrepStmts);
            config.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
            config.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
            MapperManager.initDatasource(config);

            return;
        }
        List<String> basePackages = new ArrayList<>();
        for (BaseSqlConfig bean : beans) {
            String pack = bean.getBasePackage();
            if(StringUtils.isEmpty(pack)){
                throw new RuntimeException("the basePackage username password is required");
            }
            for (String basePackage : basePackages) {
                if(basePackage.startsWith(pack)||pack.startsWith(basePackage)){
                    throw new RuntimeException("the basePackage conflicts");
                }
            }
            basePackages.add(pack);
        }
        MapperManager.initDatasource(beans);
    }
}
