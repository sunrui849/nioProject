package com.sr.nio.aio1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class TimeServer {
    public static void main(String[] args) {
        int port = 8080;
        new Thread(new AsyncTimeServerHandler(port), " AIO-Thread-01").start();
    }

}

class AsyncTimeServerHandler implements Runnable{

    int port;
    CountDownLatch latch;
    AsynchronousServerSocketChannel asynchronousServerSocketChannel;
    public AsyncTimeServerHandler(int port){
        this.port = port;
        try {
            // 创建异步服务通道
            asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
            // 绑定端口号
            asynchronousServerSocketChannel.bind(new InetSocketAddress((port)));
            System.out.println("the time server is start in port" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        // 初始化latch
        latch = new CountDownLatch(1);
        doAccept();
        try {
            // 阻塞直到完成
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doAccept() {
        // 监听接收新的连接客户端
        asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
    }
}


class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel,AsyncTimeServerHandler> {

    @Override
    public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
        // 异步接收新的客户端，这样才可以允许成千上万的客户端接入
        attachment.asynchronousServerSocketChannel.accept(attachment, this);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        // 异步读，通知回调ReadCompletionHandler
        result.read(buffer, buffer, new ReadCompletionHandler(result));
    }

    @Override
    public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
        exc.printStackTrace();
        attachment.latch.countDown();
    }
}

class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>{
    private AsynchronousSocketChannel channel;
    ReadCompletionHandler(AsynchronousSocketChannel channel){
        this.channel = channel;
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        attachment.flip();
        byte[] body = new byte[attachment.remaining()];
        attachment.get(body);
        try {
            String req = new String(body, "UTF-8");
            System.out.println("the time server receive order :" + req);
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
            doWrite(currentTime);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void doWrite(String currentTime) {
        if (currentTime != null && currentTime.trim().length() > 0){
            byte[] bytes = currentTime.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            channel.write(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    // 如果没有发送完成，继续发送
                    if (attachment.hasRemaining()){
                        channel.write(attachment, attachment, this);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        try {
            this.channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}