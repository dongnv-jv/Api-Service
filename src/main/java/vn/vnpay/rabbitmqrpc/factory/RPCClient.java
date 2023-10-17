package vn.vnpay.rabbitmqrpc.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Autowire;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RPCClient {
    private final Logger logger = LoggerFactory.getLogger(RPCClient.class);

    @CustomValue("exchange.rpc.queueName")
    private String queueName;
    @Autowire
    private ChannelPool channelPool;

    public CompletableFuture<GeneralResponse<ResponsePayment>> processRPC(PaymentRequest paymentRequest, long timeout, HttpExchange httpExchange) {
        CompletableFuture<GeneralResponse<ResponsePayment>> future = new CompletableFuture<>();
        Channel channel = null;
        try {
            channel = channelPool.getChannel();
            String replyQueueName = this.declareQueue(channel);
            String correlationId = this.publishMessage(paymentRequest, channel, replyQueueName);
            this.consumeMessage(future, channel, replyQueueName, correlationId);
            this.scheduleTimeout(future, httpExchange, timeout);
        } catch (Exception e) {
            future.completeExceptionally(e);
            logger.error("Failed to consume response from queue {}", queueName, e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel);
            }
        }
        return future;
    }

    private String declareQueue(Channel channel) throws IOException {
        return channel.queueDeclare().getQueue();
    }

    private String publishMessage(PaymentRequest paymentRequest, Channel channel, String replyQueueName) throws IOException {
        String correlationId = UUID.randomUUID().toString();
        byte[] bytes = ObjectConverter.objectToBytes(paymentRequest);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().correlationId(correlationId).replyTo(replyQueueName).build();
        channel.basicPublish("", queueName, props, bytes);
        logger.info("Sent message successfully with correlationId: {}", correlationId);
        return correlationId;
    }

    private void consumeMessage(CompletableFuture<GeneralResponse<ResponsePayment>> future, Channel channel, String replyQueueName, String correlationId) throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                TypeReference<GeneralResponse<ResponsePayment>> typeRef = new TypeReference<GeneralResponse<ResponsePayment>>() {
                };
                GeneralResponse<ResponsePayment> paymentResponse = ObjectConverter.bytesToObject(delivery.getBody(), typeRef);
                future.complete(paymentResponse);
                channel.queueDelete(replyQueueName);
            }
        };
        channel.basicConsume(replyQueueName, true, deliverCallback, consumerTag -> {
        });
    }

    private void scheduleTimeout(CompletableFuture<GeneralResponse<ResponsePayment>> future, HttpExchange httpExchange, long timeout) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(() -> {
            if (!future.isDone()) {
                try {
                    this.sendResponse(httpExchange);
                } catch (IOException e) {
                    logger.error("Failed to response request timeout ", e);
                }
                future.completeExceptionally(new TimeoutException("Timeout while waiting for response"));
            }
        }, timeout, TimeUnit.MILLISECONDS);
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
