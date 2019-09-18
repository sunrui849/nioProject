package com.sr.nio.netty.date0917;

import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

import java.util.ArrayList;
import java.util.List;

public class MessagePackDemo {
    // 可以作为比较固定的深度克隆
    public static void main(String[] args) throws Exception{
        List<String> list = new ArrayList<String>();
        list.add("111");
        list.add("222");
        list.add("333");
        MessagePack messagePack = new MessagePack();
        byte[] raw = messagePack.write(list);// 序列化成字节
        List<String> result = messagePack.read(raw, Templates.tList(Templates.TString));
        System.out.println(result);
        System.out.println(list == result); // false
    }

    // 深克隆
    public Object deepClone(Object obj)throws Exception{
        MessagePack messagePack = new MessagePack();
        byte[] raw = messagePack.write(obj);
        return messagePack.read(raw);
    }
}
