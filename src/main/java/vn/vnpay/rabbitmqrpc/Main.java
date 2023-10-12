package vn.vnpay.rabbitmqrpc;

import vn.vnpay.rabbitmqrpc.scan.ApplicationContext;

public class Main {
    public static void main(String[] args) {
        try {
            new ApplicationContext("vn.vnpay.rabbitmqrpc");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}