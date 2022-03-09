package org.vessl.web.handle;

import com.alibaba.fastjson.JSON;
import com.google.common.net.HttpHeaders;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.vessl.web.annotation.Body;
import org.vessl.web.annotation.Param;
import org.vessl.web.annotation.PathParam;
import org.vessl.web.annotation.RequestMethod;
import org.vessl.web.constant.WebConstant;
import org.vessl.web.entity.HttpReqEntity;
import org.vessl.web.entity.HttpRspEntity;
import org.vessl.web.exception.HttpException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpReqHandler extends SimpleChannelInboundHandler<HttpReqEntity> {


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    //接收到客户都发送的消息
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpReqEntity reqMsg) throws Exception {
        String uri = reqMsg.getUrl();

        String httpMethod = reqMsg.getMethod();

        String paramString = reqMsg.getParams();

        String rspMsg = null;
        try {
            MethodHandle handle = WebPathHandleMapping.getHandle(RequestMethod.valueOf(httpMethod), uri);
            if (handle == null) {
                throw new HttpException(HttpResponseStatus.NOT_FOUND, "page not found");

            }
            Map<String, String> paramMap = resoleParamString(paramString);
            List<String> pathParamName = handle.getPathParamName();
            Map<String, String> pathParam = new HashMap<>();

            if (handle.isRegex()) {
                Pattern pathRegex = handle.getPathRegex();
                Matcher matcher = pathRegex.matcher(uri);
                if(matcher.find()) {
                    for (int i = 0; i < pathParamName.size(); i++) {
                        pathParam.put(pathParamName.get(i), matcher.group(i + 1));
                    }
                }
            }
            Method method = handle.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] invokeParam = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Body body = parameters[i].getAnnotation(Body.class);
                if (body != null && body.required()) {
                    if (StringUtils.isEmpty(reqMsg.getBody())) {
                        throw new HttpException(HttpResponseStatus.BAD_REQUEST, "request body is Require");
                    }
                    Class<?> type = parameters[i].getType();
                    if (type == String.class || type == Object.class) {
                        invokeParam[i] = reqMsg.getBody();
                    } else {
                        try {
                            if(reqMsg.getContentType().equals(HttpHeaderValues.APPLICATION_JSON.toString())){
                                invokeParam[i] = JSON.parseObject(reqMsg.getBody(), type);
                            }else{
                                throw new HttpException(HttpResponseStatus.BAD_REQUEST, "request body is invalid");
                            }

                        } catch (Exception e) {
                            throw new HttpException(HttpResponseStatus.BAD_REQUEST, "request body is invalid");
                        }
                    }
                    continue;
                }
                Param param = parameters[i].getAnnotation(Param.class);
                if(param != null) {
                    String paramKey = param.value();
                    if (StringUtils.isEmpty(paramKey)) {
                        paramKey = parameters[i].getName();
                    }
                    if (param.required()) {
                        if (!paramMap.containsKey(paramKey)) {
                            throw new HttpException(HttpResponseStatus.BAD_REQUEST, "request param " + param.value() + " is Require");
                        }
                        invokeParam[i] = paramMap.get(paramKey);
                        continue;
                    }
                }
                if (handle.isRegex()) {
                    PathParam annotation = parameters[i].getAnnotation(PathParam.class);
                    if(annotation != null) {
                        String paramKey = annotation.value();
                        if (StringUtils.isEmpty(paramKey)) {
                            paramKey = parameters[i].getName();
                        }
                        if (annotation.required()) {
                            if (!pathParam.containsKey(paramKey)) {
                                throw new HttpException(HttpResponseStatus.BAD_REQUEST, "path param " + annotation.value() + " is Require");
                            }
                            invokeParam[i] = pathParam.get(paramKey);
                            continue;
                        }
                    }
                }
                invokeParam[i] = null;

            }

            Object invoke = handle.getMethod().invoke(handle.getObject(),invokeParam);
            rspMsg = invoke.toString();
        } catch (HttpException e) {
            HttpRspEntity rspEntity = new HttpRspEntity(WebConstant.HTTP_VERSION_1_1, e.getStatus());
            write(ctx,rspEntity,reqMsg.isKeepAlive());
            return;
        }


        HttpRspEntity rspEntity = new HttpRspEntity(WebConstant.HTTP_VERSION_1_1, HttpResponseStatus.OK);
        rspEntity.setBody(rspMsg);
        write(ctx,rspEntity,reqMsg.isKeepAlive());

    }

    private void write(ChannelHandlerContext ctx,HttpRspEntity rspEntity,boolean keepAlive){
        rspEntity.getHeaders().put(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON.toString());
        rspEntity.getHeaders().put(HttpHeaders.CONTENT_LENGTH, String.valueOf(rspEntity.getBody().length()));
        if(keepAlive){
            rspEntity.getHeaders().put(HttpHeaders.CONNECTION, HttpHeaderValues.KEEP_ALIVE.toString());
            ctx.writeAndFlush(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(rspEntity.toString()), StandardCharsets.UTF_8));

        }else{
            ctx.writeAndFlush(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(rspEntity.toString()), StandardCharsets.UTF_8)).addListener(ChannelFutureListener.CLOSE);

        }
    }

    private Map<String, String> resoleParamString(String paramString) {
        Map<String, String> paramMap = new HashMap<>();
        if (StringUtils.isEmpty(paramString)) {
            return paramMap;
        }
        paramString = URLDecoder.decode(paramString, StandardCharsets.UTF_8);
        String[] split = paramString.split("&");
        for (String s : split) {
            if(StringUtils.isEmpty(s)){
                continue;
            }
            String[] split1 = s.split("=");
            if (split1.length == 1) {
                continue;
            }
            paramMap.put(split1[0], split1[1]);
        }
        return paramMap;
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


}