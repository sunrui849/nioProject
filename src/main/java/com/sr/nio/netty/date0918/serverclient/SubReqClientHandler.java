package com.sr.nio.netty.date0918.serverclient;

import com.sr.nio.netty.date0918.SubscribeReqProto;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

public class SubReqClientHandler extends ChannelHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        for (int i = 0; i < 10; i++){
            ctx.write(subReq(i));
        }
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object){
        System.out.println(String.format("receive server response: [ %s]", object));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable){
        throwable.printStackTrace();
        ctx.close();
    }

    /**
     * 拼装请求的消息
     * @param i
     * @return
     */
    private SubscribeReqProto.SubscribeReq subReq(int i) {
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setSubReqID(i);
        builder.setUserName("Sunrui");
        builder.setProductName("Netty Book");
        List<String> addressList = new ArrayList<>();
        addressList.add("Beijing");
        addressList.add("Heilongjiang");
        addressList.add("Alibaba");
        builder.addAllAddress(addressList);
        return builder.build();
    }
}