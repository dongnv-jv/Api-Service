package vn.vnpay.rabbitmqrpc;

import vn.vnpay.rabbitmqrpc.controller.PaymentController;
import vn.vnpay.rabbitmqrpc.scan.ApplicationContext;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            ApplicationContext context = new ApplicationContext("vn.vnpay.rabbitmqrpc");
            PaymentController paymentController = context.getBean(PaymentController.class);
            List<String> paths = Arrays.asList("/", "/send", "/get");
            paymentController.start(paths);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}