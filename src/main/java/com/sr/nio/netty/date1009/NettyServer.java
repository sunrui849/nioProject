package com.sr.nio.netty.date1009;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class NettyServer {
    public void bind() throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new NettyMessageDecoder(1024, 4, 4));
                        ch.pipeline().addLast(new NettyMessageEncoder());
                        ch.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler(50));
                        ch.pipeline().addLast(new LoginAuthRespHandler());
                        ch.pipeline().addLast("HeartBeatHandler", new HeartBeatRespHandler());
//                        ch.pipeline().addLast("HearthjkBeatHandler", new TestHandler());

                    }
                });
        server.bind("127.0.0.1", 8000).sync();
        System.out.println("Netty server start ok : 127.0.0.1:8000");
    }

    public static void main(String[] args) throws Exception{
        new NettyServer().bind();
    }

}

class TestHandler extends ChannelHandlerAdapter{
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("111111111111");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("22222222222222222222");
        System.out.println(msg);
    }
}