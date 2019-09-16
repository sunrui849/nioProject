package com.sr.nio.netty.date0916.support;

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
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @Update 0916 netty支持 粘包拆包  换行符方式
 * 一般有四种决绝粘包拆包的问题：
 * 1.消息长度固定
 * 2.换行符作为结束符
 * 3.特殊分隔符作为结束符，换行符也算特殊分隔符一种
 * 4.在消息头中定义长度字段标识消息的总长度
 */
public class TimeClient {
    public void connect(String host, int port) throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    // 即时将消息发送出去，而不是等到多个小包组合成一个大包一起发送，可以降低延时，
                    // TCP_CORK则是将多个小包组合成大包发送，有效提高网络的有效负载，但是造成了消息延时
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new TimeClientHandler());
                        }
                    });
            // 发起同步连接操作
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 阻塞等待客户端链路关闭
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8080;
        new TimeClient().connect("127.0.0.1", port);
    }
}


class TimeClientHandler extends ChannelHandlerAdapter{
    private int counter;
    byte[] req;

    public TimeClientHandler(){
        // 初始化消息 , 如果需要传\n 字符串，则需要使用 \\n进行转义，否则会被 LineBasedFrameDecoder 切割成多个包
//                注释代码可测试粘包
        String str = "";
        for (int i = 0;i<10;i++){
            for (int j = 0;j<100;j++){
                str = str + i;
            }
        }
        req = (str + System.getProperty("line.separator")).getBytes();

//        req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
    }

    // 建立连接成功后回调
    @Override
    public void channelActive(ChannelHandlerContext ctx){
        ByteBuf firstMessage;
        for (int i = 0; i < 100; i++){
            firstMessage = Unpooled.buffer(req.length);
            firstMessage.writeBytes(req);
            // 将请求消息发送给服务端
            ctx.writeAndFlush(firstMessage);
        }
    }

    // 读到消息回调
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        String body = (String) msg;
        System.out.println("Now is:" + body + "; the counter is " + ++counter);
    }

    // 发生异常回调
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        // 释放资源，关掉连接
        ctx.close();
    }
}