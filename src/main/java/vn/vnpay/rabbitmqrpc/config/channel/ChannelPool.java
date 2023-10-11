package vn.vnpay.rabbitmqrpc.config.channel;

import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.exception.CommonException;

import java.util.NoSuchElementException;

public class ChannelPool implements Cloneable {

    private volatile static ChannelPool instance;
    private final Logger logger = LoggerFactory.getLogger(ChannelPool.class);
    private GenericObjectPool<Channel> internalPool;


    public static void initChannelPool(int maxTotal, int minIdle, int maxIdle, boolean blockWhenExhausted, ChannelFactory factory) {

        if (instance == null) {
            synchronized (ChannelPool.class) {
                if (instance == null) {
                    instance = new ChannelPool(maxTotal, minIdle, maxIdle, blockWhenExhausted, factory);
                }
            }
        }
    }

    public static ChannelPool getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ChannelPool not initialized. Call init() before getInstance()");
        }
        return instance;
    }

    public ChannelPool(int maxTotal, int minIdle, int maxIdle, boolean blockWhenExhausted, ChannelFactory factory) {

        if (internalPool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
                logger.error("Create InternalPool fail with root cause ", e);
            }
        }
        GenericObjectPoolConfig<Channel> defaultConfig = new GenericObjectPoolConfig<>();
        defaultConfig.setMaxTotal(maxTotal);
        defaultConfig.setMinIdle(minIdle);
        defaultConfig.setMaxIdle(maxIdle);
        defaultConfig.setBlockWhenExhausted(blockWhenExhausted);
        internalPool = new GenericObjectPool<>(factory, defaultConfig);
        try {
            internalPool.preparePool(); // Tạo ra số lượng đối tượng bằng minIdle và đưa chúng vào pool
        } catch (Exception e) {
            logger.error("Create InternalPool fail with root cause ", e);
        }
        logger.info("Create InternalPool with {} Channel in Pool", internalPool.getNumIdle()); //todo
    }

    private void closeInternalPool() {
        try {
            internalPool.close();
            logger.info("InternalPool is close !");
        } catch (Exception e) {
            logger.error("Could not destroy the pool", e);
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                internalPool.returnObject(channel);
            } else {
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            logger.error("Could not return the Channel {} to the pool", channel.getChannelNumber(), e);
        }
    }

    public Channel getChannel() {
        try {
            return internalPool.borrowObject(5000);
        } catch (NoSuchElementException nse) {
            logger.error("The exception was caused by an exhausted pool", nse);
            throw new CommonException("Could not get a resource from the pool", nse);
        } catch (Exception e) {
            logger.error("An error occurred during getChannel", e);
            throw new CommonException("Could not get a resource from the pool", e);
        }
    }

}
