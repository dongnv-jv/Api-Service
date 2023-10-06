package org.example.config;

import org.example.annotation.CustomValue;
import org.example.config.channel.ChannelFactory;
import org.example.config.channel.ChannelPool;
import org.example.config.connection.RabbitMqConnectionFactory;
import org.example.config.connection.RabbitMqConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonConfig {
    Logger logger = LoggerFactory.getLogger(CommonConfig.class);
    @CustomValue("rabbitMq.host")
    private String host;
    @CustomValue("rabbitMq.port")
    private int port;
    @CustomValue("rabbitMq.username")
    private String username;
    @CustomValue("rabbitMq.password")
    private String password;
    @CustomValue("rabbitMq.virtualHost")
    private String virtualHost;
    @CustomValue("connection.pool.maxTotal")
    private int maxTotalConnPool;
    @CustomValue("connection.pool.maxIdle")
    private int minIdleConnPool;
    @CustomValue("connection.pool.minIdle")
    private int maxIdleConnPool;
    @CustomValue("connection.pool.blockWhenExhausted")
    private boolean blockWhenExhaustedConnPool;
    @CustomValue("channel.pool.maxTotal")
    private int maxTotalChannelPool;
    @CustomValue("channel.pool.maxIdle")
    private int minIdleChannelPool;
    @CustomValue("channel.pool.minIdle")
    private int maxIdleChannelPool;
    @CustomValue("channel.pool.blockWhenExhausted")
    private boolean blockWhenExhaustedChannelPool;

    public void configure() {
        try {
            RabbitMqConnectionFactory connectionFactoryFactory = RabbitMqConnectionFactory.getInstance(host, port, username, password, virtualHost);
            RabbitMqConnectionPool.initConnectionPool(maxTotalConnPool, minIdleConnPool, maxIdleConnPool, blockWhenExhaustedConnPool, connectionFactoryFactory);
            RabbitMqConnectionPool rabbitMqConnectionPool = RabbitMqConnectionPool.getInstance();
            ChannelFactory channelFactory = ChannelFactory.getInstance(rabbitMqConnectionPool);
            ChannelPool.initChannelPool(maxTotalChannelPool, minIdleChannelPool, maxIdleChannelPool, blockWhenExhaustedChannelPool, channelFactory);
        } catch (Exception e) {
            logger.error("Failed to configure RabbitMQ ", e);
        }

    }

}
// cấu hình config động
// Hàm config không trả về void => có thể không câ hình được nhưng code vẫn chạy
//