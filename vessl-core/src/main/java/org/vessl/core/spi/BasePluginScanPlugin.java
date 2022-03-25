package org.vessl.core.spi;

import org.vessl.core.bean.BeanStore;
import org.vessl.core.bean.Order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 通过vessl.handles来扫描插件
 */
@Plugin
@Order(Integer.MIN_VALUE)
public class BasePluginScanPlugin implements FileScanPlugin {
    @Override
    public String getPath() {
        return "classpath*:META-INF/vessl.handles";
    }

    @Override
    public void handle(List<InputStream> inputStreams) {
        for (InputStream inputStream : inputStreams) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            try (br) {
                String s;
                while ((s = br.readLine()) != null) {
                    try {
                        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(s);
                        BeanStore.collectionClass(aClass);

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
