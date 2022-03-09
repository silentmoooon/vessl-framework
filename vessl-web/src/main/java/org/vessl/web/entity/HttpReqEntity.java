package org.vessl.web.entity;

import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpReqEntity {
    private String reqLine;
    private String version;
    private String url;
    private String method;
    private String paramString;
    private Map<String, String> headers = new HashMap<>();
    private String contentType;
    private int contentLength;
    private boolean keepAlive;
    private String body;

    public String getParams(){
        if (contentType.equals(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
            return StringUtils.isNoneEmpty(paramString)?paramString:body;
        }else{
            return paramString;
        }
    }

    public int getBodyLength(){
        if (StringUtils.isNoneEmpty(body)) {
            return body.length();
        }
        return 0;
    }

}
