package com.sr.nio.netty.date1009;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/10/8.
 */
public class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage>{
    private NettyMarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws IOException{
        marshallingEncoder = MarshallingCodecFactory.buildMarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          NettyMessage msg, List<Object> list) throws Exception {
        if (msg == null || msg.getHeader() == null){
            throw new Exception("The encode message is null");
        }

        ByteBuf sendBuf = Unpooled.buffer();
        sendBuf.writeInt(msg.getHeader().getCrcCode());
        sendBuf.writeInt(msg.getHeader().getLength());
        sendBuf.writeLong(msg.getHeader().getSessionID());
        sendBuf.writeByte(msg.getHeader().getType());
        sendBuf.writeByte(msg.getHeader().getPriority());
        sendBuf.writeInt(msg.getHeader().getAttachment().size());

        String key = null;
        byte[] keyArray = null;
        Object value = null;
        for (Map.Entry<String, Object> param : msg.getHeader().getAttachment().entrySet()){
            key = param.getKey();
            keyArray = key.getBytes(StandardCharsets.UTF_8);
            sendBuf.writeInt(keyArray.length);
            sendBuf.writeBytes(keyArray);
            value = param.getValue();
            marshallingEncoder.encode(ctx, value, sendBuf);
        }
        if (msg.getBody() != null){
            marshallingEncoder.encode(ctx, msg.getBody(), sendBuf);
        }else {
            sendBuf.writeInt(0);
        }
        //更新消息长度字段的值，至于为什么-8，是因为8是长度字段后的偏移量，LengthFieldBasedFrameDecoder的源码中
        //对长度字段和长度的偏移量之和做了判断，如果不-8，会导致LengthFieldBasedFrameDecoder解码返回null
        //这是 《Netty权威指南》中的写错的地方《引自 https://www.cnblogs.com/manmanrenshenglu/p/9264769.html》
        // todo 后面需要深入了解一下这里
        sendBuf.setInt(4, sendBuf.readableBytes() - 8);
        list.add(sendBuf);// 书中没有这段代码，不加这段代码编码失效，消息发送不过去
    }
}
