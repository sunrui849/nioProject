package com.sr.nio.netty.date0918;

import java.util.ArrayList;
import java.util.List;

public class TestSubscribeReqProto {
    private static byte[] encode(SubscribeReqProto.SubscribeReq req){
        return req.toByteArray();
    }

    private static SubscribeReqProto.SubscribeReq decode(byte[] bytes)throws Exception{
        return SubscribeReqProto.SubscribeReq.parseFrom(bytes);
    }

    private static SubscribeReqProto.SubscribeReq createSubscribeReq(){
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setSubReqID(1);
        builder.setUserName("sunrui");
        builder.setProductName("netty book");

        List<String> address = new ArrayList<>();
        address.add("BeiJing TianAnMen");
        address.add("HeiLongJiang JiaMusi");
        address.add("AoMen XinPuJing");
        builder.addAllAddress(address);
        return builder.build();
    }

    public static void main(String[] args) throws Exception{
        SubscribeReqProto.SubscribeReq req = createSubscribeReq();
        System.out.println("Before encode:" + req.toString());
        SubscribeReqProto.SubscribeReq req2 = decode(encode(req));// 编码在解码
        System.out.println("After decode:" + req2.toString());
        System.out.println("Assert equal : -->" + req2.equals(req));
    }
}
