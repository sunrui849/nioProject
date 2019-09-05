package com.sr.nio.bio01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TimeClient {
    public static void main(String[] args) {
        int serverPort = 8080;

        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            socket = new Socket("127.0.0.1", serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            // 写入消息
            out.println("Query Time ORDER");
            System.out.println("send order 2 server success");
            // 阻塞等待消息
            String resp = in.readLine();
            System.out.println("Now is :" + resp);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
