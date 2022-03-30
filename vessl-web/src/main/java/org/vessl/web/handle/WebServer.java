package org.vessl.web.handle;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebServer {

    private int port = 8080;

    void setPort(int port) {
        this.port=port;
    }

    private HttpServerInitializer httpServerInitializer = new HttpServerInitializer();

    private NioEventLoopGroup bossGroup = new NioEventLoopGroup();
    private NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    private ChannelFuture channelFuture;

    public void init() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(httpServerInitializer);

        channelFuture = serverBootstrap.bind(port);
        log.info("the webServer is started,bind port:{}",port);
    }

    public void destroy() {
        try {
            channelFuture.channel().closeFuture();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
