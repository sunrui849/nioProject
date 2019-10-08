package com.sr.nio.netty.date1009;

import io.netty.buffer.ByteBuf;
import java.io.IOException;

/**
 * Created by Administrator on 2019/10/8.
 */
public class MarshallingEncoder {
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private Marshaller marshaller;
//https://www.cnblogs.com/manmanrenshenglu/p/9264769.html
    public MarshallingEncoder() throws IOException {
    }

    public void encode(Object msg, ByteBuf out) {
        int lengthPos = out.writerIndex();
        out.writeBytes(LENGTH_PLACEHOLDER);
        ChannelBufferByteOutput output = new ChannelBufferByteOutput();
        marshaller.start(output);
    }
}
