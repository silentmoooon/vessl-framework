package org.vessl.web.exception;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;

@Data
public class HttpException extends RuntimeException {
    private HttpResponseStatus status;
    private String msg;
    private HttpException(){

    }
    public HttpException(HttpResponseStatus status,String msg){
        super(msg);
        this.status=status;
        this.msg=msg;
    }
}
