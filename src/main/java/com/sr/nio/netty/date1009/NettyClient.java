package com.sr.nio.netty.date1009;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NettyClient {
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final int LOCAL_PORT = 8684;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public void connect(String host, int port) throws Exception{
        try {
            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap client = new Bootstrap();
            client.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyMessageDecoder(1024*1024, 4, 4));// 解码
                            ch.pipeline().addLast("MessageEncoder", new NettyMessageEncoder());// 编码
                            ch.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler(50));// 超时
                            ch.pipeline().addLast("LoginAuthHandler", new LoginAuthReqHandler());// 握手
                            ch.pipeline().addLast("HeartBeatHandler", new HeartBeatReqHandler());// 心跳
//                            ch.pipeline().addLast("LoginAuthttryHandler", new TestHandler2());// 握手
                        }
                    });
            ChannelFuture future = client.connect(new InetSocketAddress(host, port), new InetSocketAddress(LOCAL_HOST, LOCAL_PORT)).sync();
            future.channel().closeFuture().sync();
        } finally {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        try {
                            connect(LOCAL_HOST, LOCAL_PORT);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void main(String[] args) throws Exception{
        new NettyClient().connect("127.0.0.1", 8000);
    }
}
class TestHandler2 extends ChannelHandlerAdapter{
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuf firstMessage = Unpooled.buffer(req.length);
        firstMessage.writeBytes(req);
        System.out.println("233333333333333333");
        ctx.writeAndFlush("ddddddddd");
    }
}