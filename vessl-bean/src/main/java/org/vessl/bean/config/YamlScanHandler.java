package org.vessl.bean.config;

import org.vessl.bean.FileScanHandler;
import org.vessl.bean.Order;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Order(Integer.MIN_VALUE)
public class YamlScanHandler implements FileScanHandler {
    @Override
    public String getPath() {
        return "classpath:config/*.yml,config/*.yml";
    }

    @Override
    public void handle(List<InputStream> inputStreams) {
        for (InputStream inputStream : inputStreams) {
            Map<String,Object> conf = new Yaml().load(inputStream);

            ConfigManager.put(conf);
        }
    }
}
