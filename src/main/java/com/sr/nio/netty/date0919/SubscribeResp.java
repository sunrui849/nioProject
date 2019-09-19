package com.sr.nio.netty.date0919;

import java.io.Serializable;

public class SubscribeResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private int subReqId;
    private int respCode;
    private String desc;

    public SubscribeResp() {
    }

    public SubscribeResp(int subReqId, int respCode, String desc) {
        this.subReqId = subReqId;
        this.respCode = respCode;
        this.desc = desc;
    }

    public int getSubReqId() {
        return subReqId;
    }

    public void setSubReqId(int subReqId) {
        this.subReqId = subReqId;
    }

    public int getRespCode() {
        return respCode;
    }

    public void setRespCode(int respCode) {
        this.respCode = respCode;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "SubscribeResp{" +
                "subReqId=" + subReqId +
                ", respCode=" + respCode +
                ", desc='" + desc + '\'' +
                '}';
    }
}
