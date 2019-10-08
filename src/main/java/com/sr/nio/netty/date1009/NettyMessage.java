package com.sr.nio.netty.date1009;

/**
 * Created by Administrator on 2019/10/8.
 */
public class NettyMessage {
    private Header header;// 消息头
    private Object body;// 消息体

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "NettyMessage [header=" + header + "]";
    }
}
