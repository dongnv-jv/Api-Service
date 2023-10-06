package org.example.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import org.example.config.channel.ChannelPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Producer {
    private final Logger logger = LoggerFactory.getLogger(Producer.class);

    private volatile boolean hasFailedMessage = false;

    private synchronized void handleSendFailedMessage(Channel channel) {

        if (!hasFailedMessage) {
            channel.addReturnListener((replyCode, replyText, exchange, routingKey, properties, body) -> {
                logger.error("Message send failed because wrong routing key : {} with exchange : {}", routingKey, exchange);
                hasFailedMessage = true;
            });
// Xác nhận rabbitServer có nhân message thành công hay không
            channel.addConfirmListener(new ConfirmListener() {
                @Override
                public void handleAck(long deliveryTag, boolean multiple) {
                    // do nothing
                }

                @Override
                public void handleNack(long deliveryTag, boolean multiple) {
                    logger.info("Failed to send message to Rabbit server with deliveryTag: {} ", deliveryTag);
                }
            });
        }

    }

    public void sendMessage(byte[] message, String routingKey, String exchangeName, Map<String, Object> mapPropsForHeaders) {
        long start = System.currentTimeMillis();
        logger.info("Start sendToExchange in Producer ");
        ChannelPool channelPool = ChannelPool.getInstance();
        this.sendToExchange(message, channelPool, routingKey, exchangeName, mapPropsForHeaders);
        long end = System.currentTimeMillis();
        logger.info("Process sendToExchange in Producer take {} millisecond", (end - start));
    }

    private void sendToExchange(byte[] message, ChannelPool channelPool, String routingKey, String exchangeName, Map<String, Object> mapPropsForHeaders) {

        Channel channel = null;
        try {
            channel = channelPool.getChannel();
            channel.confirmSelect();
            this.handleSendFailedMessage(channel);
            AMQP.BasicProperties props = new AMQP.BasicProperties();
            props = props.builder().headers(mapPropsForHeaders).build();
            channel.basicPublish(exchangeName, routingKey, true, props, message);
        } catch (Exception e) {
            logger.error(" Send message to exchange failed with root cause ", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel);
            }

        }
    }
}
