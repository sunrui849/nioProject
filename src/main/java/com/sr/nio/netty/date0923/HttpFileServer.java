package com.sr.nio.netty.date0923;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class HttpFileServer {
    private static final String DEFAULT_URL = "/src/main/java/com/sr/nio";

    public void run(final int port, final String url) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workGroup)
                    .option(ChannelOption.SO_BACKLOG,100)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());// 请求解码
                            // 将多个消息转换为单一的FullHttpRequest 或 FullHttpResponse, 原因是http解码器在每个http消息中会生成多个消息对象
                            // HttpRequest/ HttpResponse  HttpContent  LastHttpContent
                            // 比如对于POST请求，如果只使用HttpServerCodec，是无法完全解析的，不能拿到body体中的信息
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());// 响应编码
                            ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler()); // 支持异步发送大的码流，防止发生内存溢出
                            ch.pipeline().addLast("fileServerHandler", new HttpFileServerHandler(url));
                        }
                    });
            ChannelFuture future = server.bind("10.160.0.194", port).sync();
            System.out.println(" http 文件目录服务器启动，网址是  http://10.160.0.194:" + port + url);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8080;
        new HttpFileServer().run(port, DEFAULT_URL);
    }
}


/**
 * SimpleChannelInboundHandler 继承自 ChannelHandlerAdapter
 */
class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");


    private final String url;

    public HttpFileServerHandler(String url) {
        this.url = url;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        System.out.println("服务器接收消息" + request);

        // 如果解码失败直接返回400
        if (!request.getDecoderResult().isSuccess()){
            sendError(ctx, BAD_REQUEST);
            return;
        }

        // 如果不是GET请求，则405
        if (request.getMethod() != HttpMethod.GET){
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        // 对请求的URL进行包装
        final String uri = request.getUri();
        final String path = sanitizeUri(uri);

        if (path == null){
            sendError(ctx, FORBIDDEN);
            return;
        }

        // 如果文件不存在或是系统隐藏文件，404
        File file = new File(path);
        if (file.isHidden() || !file.exists()){
            sendError(ctx, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (uri.endsWith("/")){
                sendListing(ctx, file);
            } else {
                sendRedirect(ctx, uri + '/');
            }
            return;
        }

        // 文件合法性检查
        if (!file.isFile()){
            sendError(ctx, FORBIDDEN);
            return;
        }

        // IE下才会打开文件，其他浏览器都是直接下载
        // 随机文件读写类以只读的方式打开文件
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");// 以只读的方式打开文件
        } catch (FileNotFoundException e) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        // 获取文件长度，构建成功的http应答消息
        long fileLength = randomAccessFile.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK);
        // HTTPHeader.setContentLength
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file);

        if (isKeepAlive(request)){
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);

        ChannelFuture sendFileFuture;
        // 通过netty的 ChunkedFile 对象直接将文件写入到发送缓冲区，最后为 sendFileFeature 增加GenericFeatureListener,
        // 如果发送完成，打印 Transfer progress
        sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                if (total < 0 ){
                    System.err.println("Transfer progress:" + progress);
                }else {
                    System.err.println("Transfer progress:" + progress + "/" + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                System.out.println("Transfer complete");
            }
        });

        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!isKeepAlive(request)){
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));
    }

    private String sanitizeUri(String uri){
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException ex) {
                throw new Error();
            }
        }

        if (!uri.startsWith(url)){
            return null;
        }

        if(!uri.startsWith("/")){
            return null;
        }

        uri = uri.replace('/', File.separatorChar);

        if (uri.contains(File.separator + '.')
        || uri.contains('.' + File.separator)
        || uri.startsWith(".")
        || uri.endsWith(".")
        || INSECURE_URI.matcher(uri).matches()){

            return null;
        }
        return System.getProperty("user.dir") + File.separator + uri;
    }

    private static void sendListing(ChannelHandlerContext ctx, File dir){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK);
        response.headers().set(CONNECTION, "text/html;charset=UTF-8");
        StringBuilder buf = new StringBuilder();
        String dirPath = dir.getPath();
        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");
        buf.append(dirPath);
        buf.append("目录：");
        buf.append("</title></head><body>\r\n");
        buf.append("<h3>");
        buf.append(dirPath).append("目录：");
        buf.append("</h3>\r\n");
        buf.append("<ul>");
        buf.append("<li>链接：<a href=\"../\">..</a></li>\r\n");

        // 用于展示根目录下的所有文件和文件夹，同时使用超链接
        for (File f : dir.listFiles()){
            if (f.isHidden() || !f.canRead()){
                continue;
            }
            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()){
                continue;
            }

            buf.append("<li>连接:<a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }
        buf.append("</ul></body></html>\r\n");
        ByteBuf byteBuf = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(byteBuf);
        byteBuf.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, FOUND);
        response.headers().set(LOCATION, newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    /**
     * @param ctx
     * @param status
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure:" + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain;charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


}
