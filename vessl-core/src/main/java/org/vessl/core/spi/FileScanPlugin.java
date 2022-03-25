package org.vessl.core.spi;

import java.io.InputStream;
import java.util.List;

public interface FileScanPlugin {
    String getPath();
    void handle(List<InputStream> inputStreams);
}
