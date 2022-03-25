package org.vessl.core.bean.config;

import org.vessl.core.spi.FileScanPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
public class YamlScanPlugin implements FileScanPlugin {
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
