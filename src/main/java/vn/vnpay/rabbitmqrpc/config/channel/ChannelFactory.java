package vn.vnpay.rabbitmqrpc.config.channel;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.config.connection.RabbitMqConnectionPool;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ChannelFactory implements PooledObjectFactory<Channel> {

    private static volatile ChannelFactory instance;
    private final Connection connection;
    private final Logger logger = LoggerFactory.getLogger(ChannelFactory.class);

    public static ChannelFactory getInstance(RabbitMqConnectionPool connectionPool) throws IOException, TimeoutException {
        Connection connectionRaw = connectionPool.getConnection();
        if (instance == null) {
            synchronized (ChannelFactory.class) {
                if (instance == null) {
                    instance = new ChannelFactory(connectionRaw);
                    connectionPool.returnConnection(connectionRaw);
                }
            }
        }
        return instance;
    }

    public ChannelFactory(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PooledObject<Channel> makeObject() throws Exception {
        Channel channel = connection.createChannel();
        return new DefaultPooledObject<>(channel);
    }

    @Override
    public void destroyObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
                logger.error(" Can not closing Channel with root cause  ", e);
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        return channel.isOpen();
    }

    @Override
    public void activateObject(PooledObject<Channel> pooledObject) {
    }

    @Override
    public void passivateObject(PooledObject<Channel> pooledObject) {
    }
}
