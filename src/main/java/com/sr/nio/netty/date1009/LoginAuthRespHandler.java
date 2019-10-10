package com.sr.nio.netty.date1009;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * 服务端握手接入及 重复登录白名单限制
 */
public class LoginAuthRespHandler extends ChannelHandlerAdapter {
    private Set<String> nodeCheck = new HashSet<>();

    private String[] whiteList = {"127.0.0.1", "10.160.0.194"};

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        // 如果是握手请求消息
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_REQ){
            String nodeIndex = ctx.channel().remoteAddress().toString();
            NettyMessage loginResp = null;
            // 重复登录
            if (nodeCheck.contains(nodeIndex)){
                loginResp = buildResponse((byte) -1);
            }else {
                InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                String ip = address.getAddress().getHostAddress();
                boolean isOk = false;
                // 是否在白名单内
                for (String wip : whiteList){
                    if (wip.equals(ip)){
                        isOk = true;
                        break;
                    }
                }
                loginResp = isOk ? buildResponse((byte)0) : buildResponse((byte)-1);
                if (isOk){
                    nodeCheck.add(nodeIndex);
                }
            }
            System.out.println("The login response is :" + loginResp + " body [" + loginResp.getBody() + "]");
            ctx.writeAndFlush(loginResp);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        nodeCheck.remove(ctx.channel().remoteAddress().toString());// 清除已登录标记
        ctx.close();
        ctx.fireExceptionCaught(cause);
    }

    private NettyMessage buildResponse(byte b) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_RESP);
        message.setHeader(header);
        message.setBody(b);
        return message;
    }
}
