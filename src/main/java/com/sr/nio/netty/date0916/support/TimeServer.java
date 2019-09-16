package com.sr.nio.netty.date0916.support;

import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.Date;

/**
 * @Update 0916 netty支持 粘包拆包  换行符方式
 * 一般有四种决绝粘包拆包的问题：
 * 1.消息长度固定
 * 2.换行符作为结束符
 * 3.特殊分隔符作为结束符，换行符也算特殊分隔符一种
 * 4.在消息头中定义长度字段标识消息的总长度
 */
public class TimeServer {
    public void bind(int port) throws Exception{
        // 一个用于服务端接收客户端的连接，另一个用于进行socketChannel的网络读写
        // 用于处理连接请求和建立连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 处理IO请求
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 用于启动NIO服务端的辅助启动类，为了降低服务端的开发复杂度
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 初始化服务端可连接队列，处理不过来的连接放到队列里等待处理
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());
            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8080;
        new TimeServer().bind(port);
    }
}

class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // LineBaseFrameDecoder是通过遍历byteBuf中的可读字节，如果有\n 或 \r\n 则标记为结束为止
        // 使用LineBaseFrameDecoder和StringDecoder ，可以解决拆包、粘包的问题，需要在写流的时候加上 换行符
        socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
        socketChannel.pipeline().addLast(new StringDecoder());
        socketChannel.pipeline().addLast(new TimeServerHandler());
    }
}

class TimeServerHandler extends ChannelHandlerAdapter{
    private int counter;

    // 读到消息
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        String body = (String) msg;
        System.out.println("the time server receive order" + body + "; the counter is:" + ++counter);
        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
        currentTime += System.getProperty("line.separator");
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        // 把待发送的消息放到发送缓冲数组中
        ctx.write(resp);
    }

    // 读完成,异常也会触发
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx){
        // 将发送缓冲区中的消息全部写到socketChannel中
        ctx.flush();
    }

    // 异常释放
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        // 释放资源，关掉连接
        ctx.close();
        cause.printStackTrace();
    }
}
