package vn.vnpay.rabbitmqrpc.config.connection;

import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RabbitMqConnectionFactory implements PooledObjectFactory<Connection> {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String virtualHost;
    private volatile static RabbitMqConnectionFactory instance;
    private final Logger logger = LoggerFactory.getLogger(RabbitMqConnectionFactory.class);

    public RabbitMqConnectionFactory(String host, int port, String username, String password, String virtualHost) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
    }

    public ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setHost(host);
        factory.setPort(port);
        return factory;
    }

    public static RabbitMqConnectionFactory getInstance(String host, int port, String username, String password, String virtualHost) {

        if (instance == null) {
            synchronized (RabbitMqConnectionFactory.class) {
                if (instance == null) {
                    instance = new RabbitMqConnectionFactory(host, port, username, password, virtualHost);
                }
            }
        }
        return instance;
    }

    @Override
    public PooledObject<Connection> makeObject() throws IOException, TimeoutException {

        Connection connection = connectionFactory().newConnection();
        connection.addBlockedListener(new BlockedListener() {
            public void handleBlocked(String reason) {
                // Connection is now blocked
                logger.error(" Object Connection {} is now blocked ", connection.getId());
            }

            public void handleUnblocked() {
                // Connection is now unblocked
                logger.error(" Object Connection {} is now unblocked ", connection.getId());
            }
        });
        String id = connection.getId();
        if (id == null) {
            connection.setId(UUID.randomUUID().toString());
        }
        logger.info(" Object Connection {} is creating ", connection.getId());
        return new DefaultPooledObject<>(connection);
    }

    @Override
    public void destroyObject(PooledObject<Connection> pooledObject) {
        final Connection connection = pooledObject.getObject();
        if (connection.isOpen()) {
            try {
                logger.info(" Object Connection is destroying  ... ");
                connection.close();
            } catch (Exception e) {
                logger.error("Destroy Object Connection is  failed with root cause  ", e);
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<Connection> pooledObject) {
        final Connection connection = pooledObject.getObject();
        return connection.isOpen();
    }

    @Override
    public void activateObject(PooledObject<Connection> pooledObject) {
        Connection connection = pooledObject.getObject();
        logger.info(" Object Connection {} is calling !", connection.getId());
    }

    @Override
    public void passivateObject(PooledObject<Connection> pooledObject) {
        Connection connection = pooledObject.getObject();
        logger.info(" Object Connection {} is returning !", connection.getId());
    }
}
