package org.vessl.sql.handle;

import org.apache.commons.lang3.StringUtils;
import org.vessl.sql.bean.SqlClassBean;
import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.bean.FileScanHandler;
import org.vessl.sql.constant.SqlType;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.List;

public class MapperFileHandle implements FileScanHandler {

    @Override
    public String getPath() {
        return "classpath*:mapper/*.yml";
    }

    @Override
    public void handle(List<InputStream> inputStreams) {
        for (InputStream inputStream : inputStreams) {
            readSql(inputStream);
        }
    }
    private void readSql(InputStream inputStream) {
        Constructor constructor = new Constructor(SqlClassBean.class);
        TypeDescription customTypeDescription = new TypeDescription(SqlClassBean.class);
        customTypeDescription.addPropertyParameters("methods", SqlMethodBean.class);
        constructor.addTypeDescription(customTypeDescription);
        Yaml yaml = new Yaml(constructor);
        Iterable<Object> objects = yaml.loadAll(inputStream);
        objects.forEach(o -> {
            if (o instanceof SqlClassBean classBean) {
                for (SqlMethodBean method : classBean.getMethods()) {
                    if (StringUtils.isEmpty(method.getName()) || StringUtils.isEmpty(method.getSql())) {
                        return;
                    }
                    if (StringUtils.isEmpty(method.getType())) {
                        method.setType(SqlType.SELECT.toString());
                    }
                }
                MapperManager.addMapper(classBean);
            }
        });


    }
}
