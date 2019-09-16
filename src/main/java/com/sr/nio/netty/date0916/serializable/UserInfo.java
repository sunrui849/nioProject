package com.sr.nio.netty.date0916.serializable;


import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userName;
    private int userId;

    public UserInfo buildUserName(String userName){
        this.userName = userName;
        return this;
    }
    public UserInfo buildUserId(int userId){
        this.userId = userId;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * 基于通用二进制编解码技术对UserInfo进行编码，返回byte数组
     * @return
     */
    public byte[] codeC(){
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] value = this.userName.getBytes();
        buffer.putInt(value.length);
        buffer.put(value);
        buffer.putInt(userId);
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    // 一次比较
    public static void main(String[] args) throws Exception{
        UserInfo info = new UserInfo();
        info.buildUserId(100).buildUserName("welcome to alibaba");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(bos);
        outputStream.writeObject(info);
        outputStream.flush();
        outputStream.close();
        byte[] b = bos.toByteArray();
        bos.close();
        System.out.println("the jdk serializable length is :" + b.length);
        System.out.println("the byte serializable length is:" + info.codeC().length);
    }
}

class Main{
    // 多次比较
    public static void main(String[] args) throws Exception{
        UserInfo info = new UserInfo();
        info.buildUserId(100).buildUserName("welcome to alibaba");
        int loop = 1000000;
        long startTime = System.currentTimeMillis();
        ByteArrayOutputStream bos = null;
        ObjectOutputStream objectOutputStream = null;
        for (int i = 0; i < loop; i++){
            bos = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(bos);
            objectOutputStream.writeObject(info);
            objectOutputStream.flush();
            objectOutputStream.close();
            byte[] b = bos.toByteArray();
            bos.close();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("the jdk serializable cost time is : " + (endTime - startTime) + " ms");
        System.out.println("--------------------------------------------");
        startTime = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        for (int i = 0;i < loop; i++){
            byte[] b = info.codeC();
        }
        endTime = System.currentTimeMillis();
        System.out.println("the byte array serializable cost time is : " + (endTime - startTime) + " ms");
    }
}