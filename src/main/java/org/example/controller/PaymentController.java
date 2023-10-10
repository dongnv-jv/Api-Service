package org.example.controller;

import com.sun.net.httpserver.HttpServer;
import org.example.annotation.ValueInjector;
import org.example.config.CommonConfig;
import org.example.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PaymentController {

    static Logger logger = LoggerFactory.getLogger(PaymentController.class);

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            // Inject values to CommonConfig
            CommonConfig appConfig = new CommonConfig();
            ValueInjector.injectValues(appConfig);
            appConfig.configure();

            RequestHandler requestHandler = new RequestHandler();
            ValueInjector.injectValues(requestHandler);

            server.createContext("/send", requestHandler);

            server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)));
            server.start();
            logger.info("Server is running on port 8080");

        } catch (Exception e) {
// do something
        }

    }


}
