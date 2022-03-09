package org.vessl.web.handle;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline= socketChannel.pipeline();
        //pipeline.addLast(new LineBasedFrameDecoder(10240));
        //pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(new HttpContentHandler());
        pipeline.addLast(new HttpReqHandler());
    }
}