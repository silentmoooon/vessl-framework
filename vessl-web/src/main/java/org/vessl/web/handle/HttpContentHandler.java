package org.vessl.web.handle;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.apache.commons.lang3.StringUtils;
import org.vessl.web.constant.WebConstant;
import org.vessl.web.entity.HttpReqEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HttpContentHandler extends SimpleChannelInboundHandler {

    HttpReqEntity httpEntity;
    private String partMsg = "";


    boolean isWaitBody =false;
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    //接收到客户都发送的消息
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object reqMsg) throws Exception {
        ByteBuf data = (ByteBuf) reqMsg;
        String msg = data.toString(StandardCharsets.UTF_8);
        msg=partMsg+msg;
        partMsg = "";
        if(isWaitBody){
            int contentLength = httpEntity.getContentLength();
            int bodyLength = httpEntity.getBodyLength();
            int needLength = contentLength - bodyLength;
            if(msg.length()<needLength){
                partMsg=msg;
                return;
            }
            else if(msg.length()>needLength){
                httpEntity.setBody(httpEntity.getBody()+msg.substring(0,needLength));
                ctx.fireChannelRead(httpEntity);
                msg = msg.substring(needLength);
                isWaitBody=false;
            }
            else {
                httpEntity.setBody(httpEntity.getBody()+msg);
                ctx.fireChannelRead(httpEntity);
                isWaitBody=false;
                return;
            }
        }


        handleMsg(ctx, msg);

    }

    private void handleMsg(ChannelHandlerContext ctx, String msg) {
       String[] msgSplit;
        if(msg.contains(WebConstant.HTTP_DELIMITER_WIN +WebConstant.HTTP_DELIMITER_WIN)){
            msgSplit = StringUtils.split(msg,WebConstant.HTTP_DELIMITER_WIN + WebConstant.HTTP_DELIMITER_WIN);
        }else if(msg.contains(WebConstant.HTTP_DELIMITER_LINUX +WebConstant.HTTP_DELIMITER_LINUX)){
            msgSplit = StringUtils.split(msg,WebConstant.HTTP_DELIMITER_LINUX + WebConstant.HTTP_DELIMITER_LINUX);
        }else{
            partMsg= msg;
            return;
        }
        String lineHeader = msgSplit[0].replaceAll("\\r","");
        List<String> headerSplit =  split(lineHeader,WebConstant.HTTP_DELIMITER_LINUX);
        httpEntity = new HttpReqEntity();
        String reqLine = headerSplit.remove(0);
        httpEntity.setReqLine(reqLine);
        String[] split = reqLine.split(" ");
        httpEntity.setMethod(split[0]);
        httpEntity.setVersion(split[2]);
        String[] split1 = split[1].split( "\\?");
        httpEntity.setUrl(split1[0]);
        if(split1.length>1) {
            httpEntity.setParamString(split1[1]);
        }

        for (String s : headerSplit) {
            split = StringUtils.split(s, WebConstant.HEADER_DELIMITER);
            httpEntity.getHeaders().put(split[0],split[1]);
            if(split[0].equals(WebConstant.CONTENT_TYPE)){
                httpEntity.setContentType(split[1]);
            }
            else if(split[0].equals(WebConstant.CONTENT_LENGTH)){
                httpEntity.setContentLength(Integer.parseInt(split[1]));
            }
            else if(split[0].equals(WebConstant.HEADER_CONNECTION)){
                if(WebConstant.HEADER_KEEP_ALIVE.equals(split[1])) {
                    httpEntity.setKeepAlive(true);
                }
            }
        }
        if (StringUtils.isEmpty(httpEntity.getContentType())) {
            httpEntity.setContentType(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString());
        }
        if(httpEntity.getContentLength()>0){
            if(msgSplit.length==1){
                return;
            }
            String body = msgSplit[1];
            if(body.length()<httpEntity.getContentLength()){
                partMsg=body;
                isWaitBody =true;
            }
            else if(body.length()>httpEntity.getContentLength()){
                httpEntity.setBody(body.substring(0,httpEntity.getContentLength()));
                ctx.fireChannelRead(httpEntity);
                msg = body.substring(httpEntity.getContentLength());
                handleMsg(ctx, msg);
            }
            else {
                httpEntity.setBody(body);
                ctx.fireChannelRead(httpEntity);
            }
        }else{
            ctx.fireChannelRead(httpEntity);
        }


    }


    private List<String> split(String str, String delimiter) {
        List<String> split = new ArrayList<>();
        int fromIndex = 0;
        if (str.indexOf(delimiter) == 0) {
            fromIndex += delimiter.length();
        }
        int index = 0;
        while ((index = str.indexOf(delimiter, fromIndex)) > 0) {
            split.add(str.substring(fromIndex, index));
            fromIndex = index + delimiter.length();
        }
        if (!str.endsWith(delimiter)) {
            split.add(str.substring(fromIndex));
        }

        return split;
    }

    //客户端建立连接
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

        //System.out.println(ctx.channel().remoteAddress()+"连接了!");
    }

    //关闭连接
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        //System.out.println(ctx.channel().remoteAddress()+"断开连接");
    }

    //出现异常
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    public static void main(String[] args) {
        String aaa = "a\rb";
        System.out.println(aaa.replaceAll("\\r","b"));
    }
}