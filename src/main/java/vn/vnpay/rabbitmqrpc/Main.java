package vn.vnpay.rabbitmqrpc;

import vn.vnpay.rabbitmqrpc.config.CommonConfig;
import vn.vnpay.rabbitmqrpc.scan.ApplicationContext;

public class Main {
    public static void main(String[] args) {
        try {
            ApplicationContext context = new ApplicationContext("vn.vnpay.rabbitmqrpc");
            CommonConfig commonConfig = context.getBean(CommonConfig.class);
            boolean isConfig = commonConfig.configure();
            if (!isConfig) {
                throw new RuntimeException("Configure failed");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}