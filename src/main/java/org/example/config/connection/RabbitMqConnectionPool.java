package org.example.config.connection;

import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

public class RabbitMqConnectionPool implements Cloneable {

    int maxTotal;
    int minIdle;
    int maxIdle;
    boolean blockWhenExhausted;
    RabbitMqConnectionFactory rabbitMqConnectionFactory;
    private volatile static RabbitMqConnectionPool instance;
    private final Logger logger = LoggerFactory.getLogger(RabbitMqConnectionPool.class);
    private GenericObjectPool<Connection> internalPool;

    public static void initConnectionPool(
            int maxTotal, int minIdle, int maxIdle, boolean blockWhenExhausted,
            RabbitMqConnectionFactory rabbitMqConnectionFactory) {
        if (instance == null) {
            synchronized (RabbitMqConnectionPool.class) {
                if (instance == null) {
                    instance = new RabbitMqConnectionPool(maxTotal, minIdle, maxIdle, blockWhenExhausted, rabbitMqConnectionFactory);
                }
            }
        }
    }

    public static RabbitMqConnectionPool getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RabbitMqConnectionPool not initialized. Call init() before getInstance()");
        }
        return instance;
    }

    private void configPoolObject() {
        GenericObjectPoolConfig<Connection> defaultConfig = new GenericObjectPoolConfig<>();
        defaultConfig.setMaxTotal(maxTotal);
        defaultConfig.setMaxIdle(maxIdle);
        defaultConfig.setMinIdle(minIdle);
        defaultConfig.setBlockWhenExhausted(blockWhenExhausted);
        if (this.internalPool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
                logger.error("Create InternalPool fail with root cause ", e);
            }
        }
        internalPool = new GenericObjectPool<>(rabbitMqConnectionFactory, defaultConfig);
        try {
            internalPool.preparePool(); // Tạo ra số lượng đối tượng bằng minIdle và đưa chúng vào pool
        } catch (Exception e) {
            logger.error("Create InternalPool fail with root cause ", e);
        }
        logger.info("Create InternalPool with {} Connection in Pool", internalPool.getNumIdle());
    }

    public RabbitMqConnectionPool(int maxTotal, int minIdle, int maxIdle, boolean blockWhenExhausted, RabbitMqConnectionFactory rabbitMqConnectionFactory) {
        this.maxTotal = maxTotal;
        this.minIdle = minIdle;
        this.maxIdle = maxIdle;
        this.blockWhenExhausted = blockWhenExhausted;
        this.rabbitMqConnectionFactory = rabbitMqConnectionFactory;
        this.configPoolObject();
    }

    private void closeInternalPool() {
        try {
            internalPool.close();
        } catch (Exception e) {
            logger.error("Could not destroy the pool", e);
        }
    }

    public void returnConnection(Connection connection) {
        try {
            if (connection.isOpen()) {
                logger.info(" Quantity connection Active in Connection pool: {}", internalPool.getNumActive());
                internalPool.returnObject(connection);
            } else {
                internalPool.invalidateObject(connection);
            }
        } catch (Exception e) {
            logger.error("Could not return the Connection {} to the pool", connection.getId(), e);
        }
    }

    public Connection getConnection() throws IOException, TimeoutException {
        try {
            return internalPool.borrowObject(5000);
        } catch (NoSuchElementException nse) {
            logger.error("The exception was caused by an exhausted pool", nse);
            return rabbitMqConnectionFactory.connectionFactory().newConnection();
        } catch (Exception e) {
            logger.error("An error occurred during getConnection", e);
            return rabbitMqConnectionFactory.connectionFactory().newConnection();
        }
    }

}
