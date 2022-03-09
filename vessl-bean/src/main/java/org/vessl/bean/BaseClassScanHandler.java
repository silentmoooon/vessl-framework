package org.vessl.bean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Order(Integer.MIN_VALUE)
public class BaseClassScanHandler implements FileScanHandler {
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
                        if (!aClass.isInterface()) {
                            ClassScanner.addHandle(aClass);
                        }
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
