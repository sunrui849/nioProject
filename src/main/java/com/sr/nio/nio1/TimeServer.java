package com.sr.nio.nio1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class TimeServer {
    public static void main(String[] args) {
        int port = 8080;
        new Thread(new MultiplexerTimeServer(port)," NIO Thread 01").start();
    }
}

class MultiplexerTimeServer implements Runnable{
    private Selector selector;
    private ServerSocketChannel channel;
    private volatile boolean stop;
    public MultiplexerTimeServer(int port){
        try {
            selector = Selector.open();
            channel = ServerSocketChannel.open();
            // 设置异步非阻塞
            channel.configureBlocking(false);
            // 第二个参数backlog是指请求server建立连接，但是server没有处理的连接允许的数量
            channel.socket().bind(new InetSocketAddress(port), 1024);
            // 将channel注册到selector上 并监听OP_ACCEPT操作位，类似事件监听，表示服务端监听到了客户连接
            channel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The time server is start in port:" + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // 这段代码与客户端基本一致，客户端增加连接
    public void run() {
        // 遍历查询 selectionKey ，如果查询到则处理，针对连接或数据不同处理
       while (!stop){
           try {
               // 表示休眠时间
               selector.select(1000);
               Set<SelectionKey> selectedKeys = selector.selectedKeys();
               Iterator<SelectionKey> it = selectedKeys.iterator();
               SelectionKey key = null;
               while (it.hasNext()){
                   key  = it.next();
                   // 删除刚才next返回的元素
                   it.remove();
                   try {
                       handleInput(key);
                   } catch (Exception e) {
                       if (key != null){
                           key.cancel();
                           if (key.channel() != null){
                               key.channel().close();
                           }
                       }
                   }
               }
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
       if (selector != null){
            try {
                // 当selector（多路复用器）关闭时，注册再上面的channel和pipe等资源都会被关闭
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException{
        // 判断 SelectionKey 是否有效， 由与cancel时不会立即失效，会放入一个队列里，所以需要校验一下
        if (key.isValid()){
            // 判断此键是否已经准备好接受新的连接
            if (key.isAcceptable()){
                ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ);
                // 这里相当于完成三次握手
            }
            // 判断此键是否已经准备好读取
            if (key.isReadable()){
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                // read是非阻塞的
                int readBytes = sc.read(byteBuffer);
                if (readBytes > 0){
                    // 切换读写状态,从头开读,设置position为0 , limit为原来的position
                    byteBuffer.flip();
                    // reamaining 是指 limit和position之间的差，即buffer中的字节数
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order:" + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(sc, currentTime);
                }
            }
        }
    }

    private void doWrite(SocketChannel sc, String currentTime) throws IOException{
        if (currentTime != null && currentTime.trim().length() > 0){
            byte[] bytes = currentTime.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            sc.write(byteBuffer);
        }
    }
}