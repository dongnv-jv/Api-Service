package org.example;

import org.example.factory.HttpReply;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try {
            ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(8080, 10);
            StringBuilder body = new StringBuilder();
            body.append("<html><body><h1>Hello, World!</h1></body></html>");
            while (true) {
                Socket s = ss.accept();
                Thread t = new Thread(new HttpReply(s, body));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}