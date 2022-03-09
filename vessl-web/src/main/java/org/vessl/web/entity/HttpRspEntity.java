package org.vessl.web.entity;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import org.vessl.web.constant.WebConstant;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpRspEntity {
    private String version;
    private HttpResponseStatus status;
    private Map<String, String> headers = new HashMap<>();
    private String body="";

    public HttpRspEntity(String version, HttpResponseStatus status) {
        this.version = version;
        this.status=status;
    }
    @Override
    public String toString() {
        StringBuilder rspMsg = new StringBuilder(version + " " + status.code() + " " + status.reasonPhrase() + WebConstant.HTTP_DELIMITER_WIN);
        for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) {
            rspMsg.append(stringStringEntry.getKey()).append(WebConstant.HEADER_DELIMITER).append(stringStringEntry.getValue()).append(WebConstant.HTTP_DELIMITER_WIN);
        }
        rspMsg.append(WebConstant.HTTP_DELIMITER_WIN);
        rspMsg.append(body);
        return rspMsg.toString();
    }
}
