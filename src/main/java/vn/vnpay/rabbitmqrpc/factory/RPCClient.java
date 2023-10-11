package vn.vnpay.rabbitmqrpc.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Component;
import vn.vnpay.rabbitmqrpc.annotation.CustomValue;
import vn.vnpay.rabbitmqrpc.bean.GeneralResponse;
import vn.vnpay.rabbitmqrpc.bean.PaymentRequest;
import vn.vnpay.rabbitmqrpc.bean.ResponsePayment;
import vn.vnpay.rabbitmqrpc.common.HttpStatus;
import vn.vnpay.rabbitmqrpc.common.ObjectConverter;
import vn.vnpay.rabbitmqrpc.config.channel.ChannelPool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RPCClient {
    private final Logger logger = LoggerFactory.getLogger(RPCClient.class);

    @CustomValue("exchange.rpc.queueName")
    private static String queueName;

    public CompletableFuture<GeneralResponse<ResponsePayment>> processRPC(PaymentRequest paymentRequest, long timeout, HttpExchange httpExchange) {
        ChannelPool channelPool = ChannelPool.getInstance();
        Channel channelPublish = null;
        Channel channelConsumer = null;
        CompletableFuture<GeneralResponse<ResponsePayment>> future = new CompletableFuture<>();

        try {
            channelPublish = channelPool.getChannel();
            channelConsumer = channelPool.getChannel();
            String replyQueueName = channelPublish.queueDeclare().getQueue();
            String correlationId = java.util.UUID.randomUUID().toString();
            byte[] bytes = ObjectConverter.objectToBytes(paymentRequest);
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            channelPublish.basicPublish("", queueName, props, bytes);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    TypeReference<GeneralResponse<ResponsePayment>> typeRef = new TypeReference<GeneralResponse<ResponsePayment>>() {
                    };
                    GeneralResponse<ResponsePayment> paymentResponse = ObjectConverter.bytesToObject(delivery.getBody(), typeRef);
                    future.complete(paymentResponse);
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
                        logger.error("Failed to response request timeout ", e);
                    }
                    future.completeExceptionally(new TimeoutException("Timeout while waiting for response"));
                }
            }, timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
            logger.error("Failed to consume response from queue {}", queueName, e);
        } finally {
            if (channelPublish != null && channelConsumer != null) {
                channelPool.returnChannel(channelPublish);
                channelPool.returnChannel(channelConsumer);
            }
        }
        return future;
    }


    private void sendResponse(HttpExchange httpExchange, int statusCode) throws IOException {
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        GeneralResponse<String> responseTimeout = new GeneralResponse<>();
        responseTimeout.setCode("408");
        responseTimeout.setMessage("requestTimeout");
        String responseString = ObjectConverter.objectToJson(responseTimeout);
        httpExchange.sendResponseHeaders(statusCode, responseString.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseString.getBytes());
        os.close();
    }

    private void sendResponse(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, HttpStatus.REQUEST_TIMEOUT.getCode());
    }
}
