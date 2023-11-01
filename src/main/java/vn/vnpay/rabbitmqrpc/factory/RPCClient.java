package vn.vnpay.rabbitmqrpc.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Autowire;
import vn.vnpay.rabbitmqrpc.annotation.Component;
import vn.vnpay.rabbitmqrpc.annotation.CustomValue;
import vn.vnpay.rabbitmqrpc.bean.GeneralResponse;
import vn.vnpay.rabbitmqrpc.bean.PaymentRequest;
import vn.vnpay.rabbitmqrpc.bean.ResponsePayment;
import vn.vnpay.rabbitmqrpc.common.CommonUtil;
import vn.vnpay.rabbitmqrpc.config.channel.ChannelPool;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static vn.vnpay.rabbitmqrpc.handle.RequestHandler.logIdThreadLocal;

@Component
public class RPCClient {
    private final Logger logger = LoggerFactory.getLogger(RPCClient.class);

    @CustomValue("exchange.rpc.queueName")
    private String queueName;
    @Autowire
    private ChannelPool channelPool;
    @Autowire
    private Snowflake snowflake;

    public CompletableFuture<GeneralResponse<ResponsePayment>> processRPC(PaymentRequest paymentRequest) {
        CompletableFuture<GeneralResponse<ResponsePayment>> future = new CompletableFuture<>();
        Channel channel = null;
        String logId = logIdThreadLocal.get();
        try {
            channel = channelPool.getChannel();
            String replyQueueName = this.declareQueue(channel);
            logger.info("[{}] - Create RPC client with replyQueueName: {}", logId, replyQueueName);
            String correlationId = this.publishMessage(paymentRequest, channel, replyQueueName);
            this.consumeMessage(future, channel, replyQueueName, correlationId);
        } catch (Exception e) {
            future.completeExceptionally(e);
            logger.error("[{}] - Failed to consume response from queue {} with root cause :{}", logId, queueName, e.getMessage());
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
        String correlationId = generateId();
        String logId = logIdThreadLocal.get();
        byte[] bytes = CommonUtil.objectToBytes(paymentRequest);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(replyQueueName)
                .timestamp(new Date())
                .messageId(logId)
                .build();
        channel.basicPublish("", queueName, props, bytes);
        logger.info("[{}] - Sent message to queue: {} successfully with correlationId: {}", logId, queueName, correlationId);
        return correlationId;
    }

    private String generateId() {
        long correlationId = snowflake.nextId();
        return correlationId + "";
    }

    private void consumeMessage(CompletableFuture<GeneralResponse<ResponsePayment>> future, Channel channel,
                                String replyQueueName, String correlationId) throws IOException {
        String logId = logIdThreadLocal.get();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                try {
                    TypeReference<GeneralResponse<ResponsePayment>> typeRef = new TypeReference<GeneralResponse<ResponsePayment>>() {
                    };
                    GeneralResponse<ResponsePayment> paymentResponse = CommonUtil.bytesToObject(delivery.getBody(), typeRef);
                    future.complete(paymentResponse);
                    channel.queueDelete(replyQueueName);
                } catch (Exception e) {
                    logger.error("[{}] - Failed to consume message with correlationId: {}", logId, correlationId);
                    future.completeExceptionally(e);
                }
            }
        };
        channel.basicConsume(replyQueueName, true, deliverCallback, consumerTag -> {
        });
    }


}
