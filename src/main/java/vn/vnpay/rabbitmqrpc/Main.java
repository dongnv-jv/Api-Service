package vn.vnpay.rabbitmqrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.controller.PaymentController;
import vn.vnpay.rabbitmqrpc.scan.ApplicationContext;

import java.util.Arrays;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        try {
            ApplicationContext context = new ApplicationContext("vn.vnpay.rabbitmqrpc");
            PaymentController paymentController = context.getBean(PaymentController.class);
            List<String> paths = Arrays.asList("/", "/send", "/get");
            paymentController.start(paths);
        } catch (Exception e) {
            logger.error("Failed to start programming", e);
            System.exit(1);
        }
    }
}