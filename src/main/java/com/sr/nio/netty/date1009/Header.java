package com.sr.nio.netty.date1009;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2019/10/8.
 */
public class Header {
    private int crcCode = 0xabef0101;// 0x (十六进制) abef (固定值，表示是netty协议，两个字节) 01(主版本号) 01(次版本号)
    private int length;// 消息长度（包括消息头和消息体）
    private long sessionID;// 会话ID
    private byte type;// 消息类型 0:业务请求  1：业务响应  2：业务ONE WAY消息（既是请求又是响应） 3：握手请求  4：握手响应  5：心跳请求  6：心跳响应
    private byte priority;// 消息优先级 0-255
    private Map<String, Object> attachment = new HashMap<String, Object>(); // 附件（扩展属性）

    public int getCrcCode() {
        return crcCode;
    }

    public void setCrcCode(int crcCode) {
        this.crcCode = crcCode;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getSessionID() {
        return sessionID;
    }

    public void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public Map<String, Object> getAttachment() {
        return attachment;
    }

    public void setAttachment(Map<String, Object> attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "Header{" +
                "crcCode=" + crcCode +
                ", length=" + length +
                ", sessionID=" + sessionID +
                ", type=" + type +
                ", priority=" + priority +
                ", attachment=" + attachment +
                '}';
    }
}
