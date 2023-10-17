package vn.vnpay.rabbitmqrpc.controller;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Autowire;
import vn.vnpay.rabbitmqrpc.annotation.Component;
import vn.vnpay.rabbitmqrpc.annotation.MethodAnnotation;
import vn.vnpay.rabbitmqrpc.handle.RequestHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentController {
    static Logger logger = LoggerFactory.getLogger(PaymentController.class);
    @Autowire
    private RequestHandler requestHandler;

    @MethodAnnotation(value = "/send")
    public void create(String path) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext(path, requestHandler);
            server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)));
            server.start();
            logger.info("Server is running on port 8080");
        } catch (Exception e) {
            logger.error("Server is started failed ", e);
        }
    }
}
