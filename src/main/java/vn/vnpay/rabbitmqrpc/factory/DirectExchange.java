package vn.vnpay.rabbitmqrpc.factory;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Component;
import vn.vnpay.rabbitmqrpc.annotation.CustomValue;
import vn.vnpay.rabbitmqrpc.config.channel.ChannelPool;

@Component
public class DirectExchange {

    Logger logger = LoggerFactory.getLogger(DirectExchange.class);

    @CustomValue("exchange.direct.name")
    private String exchangeDirect;
    @CustomValue("exchange.direct.routingKey")
    private String routingKey;
    @CustomValue("exchange.direct.queueName")
    private String queueName;
    @CustomValue("exchange.dead.letter.name")
    private String deadLetterExchange;
    @CustomValue("exchange.dead.letter.routingKey")
    private String deadLetterRoutingKey;


    public void createExchangeAndQueue() {
        Long start = System.currentTimeMillis();
        logger.info("Start createExchangeAndQueue in DirectExchange");
        ChannelPool channelPool = ChannelPool.getInstance();
        Channel channel = null;
        try {
            channel = channelPool.getChannel();
            channel.exchangeDeclare(exchangeDirect, BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, exchangeDirect, routingKey);
            Long end = System.currentTimeMillis();
            logger.info(" Process createExchangeAndQueue in DirectExchange take {} milliSecond ", (end - start));
        } catch (Exception e) {
            logger.error("CreateExchangeAndQueue in DirectExchange failed with root cause ", e);
        } finally {
            if (channel != null) {
                channelPool.returnChannel(channel);
            }
        }
    }


}
