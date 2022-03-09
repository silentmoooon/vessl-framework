package org.vessl.bean;

import java.io.InputStream;
import java.util.List;

public interface FileScanHandler {
    String getPath();
    void handle(List<InputStream> inputStreams);
}
