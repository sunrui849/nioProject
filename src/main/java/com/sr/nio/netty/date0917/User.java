package com.sr.nio.netty.date0917;

import org.msgpack.annotation.Message;

// 这个注解一定要加
@Message
public class User {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
    // 要有无参构造器
    public User() {
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
