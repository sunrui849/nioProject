package com.sr.nio.netty.date0918.serverclient;

import com.sr.nio.netty.date0918.SubscribeReqProto;
import com.sr.nio.netty.date0918.SubscribeRespProto;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class SubReqServerHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        SubscribeReqProto.SubscribeReq req = (SubscribeReqProto.SubscribeReq) msg;
        if ("Sunrui".equalsIgnoreCase(req.getUserName())){
            System.out.println(String.format("service accept client subscribe req [%s]", req.toString()));
            ctx.writeAndFlush(resp(req.getSubReqID()));
        }
    }

    /**
     * 封装相应信息
     * @param subReqID
     * @return
     */
    private SubscribeRespProto.SubscribeResp resp(int subReqID) {
        SubscribeRespProto.SubscribeResp.Builder builder = SubscribeRespProto.SubscribeResp.newBuilder();
        builder.setSubReqID(subReqID);
        builder.setRespCode(0);
        builder.setDesc("Netty book order succeed, will to designated address");
        return builder.build();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable){
        throwable.printStackTrace();
        ctx.close();
    }
}