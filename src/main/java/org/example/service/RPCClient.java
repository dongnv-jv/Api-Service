package org.example.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.sun.net.httpserver.HttpExchange;
import org.example.annotation.CustomValue;
import org.example.common.ObjectConverter;
import org.example.config.channel.ChannelPool;
import org.example.factory.PaymentRequest;
import org.example.factory.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RPCClient {
    Logger logger = LoggerFactory.getLogger(RPCClient.class);

    @CustomValue("exchange.rpc.queueName")
    private static String queueName;
//    @CustomValue("exchange.rpc.replyQueueName")
//    private static String replyQueueName;


    public CompletableFuture<Response<String>> processRPC(PaymentRequest paymentRequest, long timeout, HttpExchange httpExchange) {
        ChannelPool channelPool = ChannelPool.getInstance();
        Channel channel = null;
        Channel channelConsumer = null;
        CompletableFuture<Response<String>> future = new CompletableFuture<>();

        try {
            channel = channelPool.getChannel();
            channelConsumer = channelPool.getChannel();
            String replyQueueName = channel.queueDeclare().getQueue();
            String correlationId = java.util.UUID.randomUUID().toString();
            byte[] bytes = ObjectConverter.objectToBytes(paymentRequest);
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish("", queueName, props, bytes);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    future.complete(ObjectConverter.bytesToObject(delivery.getBody(), Response.class));
                }
            };

            channelConsumer.basicConsume(replyQueueName, true, deliverCallback, consumerTag -> {
            });

            // Đặt timeout cho CompletableFuture
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.schedule(() -> {
                if (!future.isDone()) {
                    try {
                        sendResponse(httpExchange);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    future.completeExceptionally(new TimeoutException("Timeout while waiting for response"));
                }
            }, timeout, TimeUnit.MILLISECONDS);

            logger.info("Consuming response from queue {}", queueName);

        } catch (Exception e) {
            future.completeExceptionally(e);
            logger.error("Failed to consume response from queue {}", queueName, e);
        } finally {
            if (channel != null && channelConsumer != null) {
                channelPool.returnChannel(channel);
                channelPool.returnChannel(channelConsumer);
            }
        }
        return future;
    }


    private void sendResponse(HttpExchange httpExchange, int statusCode) throws IOException {
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        String responseTimeout = "Timeout";
        httpExchange.sendResponseHeaders(statusCode, responseTimeout.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseTimeout.getBytes());
        os.close();
    }

    private void sendResponse(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, 408);
    }
}
