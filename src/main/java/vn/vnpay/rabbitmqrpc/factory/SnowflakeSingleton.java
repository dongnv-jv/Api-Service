package vn.vnpay.rabbitmqrpc.factory;

public class SnowflakeSingleton {
    private static volatile Snowflake instance;

    private SnowflakeSingleton() {
        // Khởi tạo Snowflake ở đây
    }

    public static Snowflake getInstance() {
        if (instance == null) {
            synchronized (SnowflakeSingleton.class) {
                if (instance == null) {
                    instance = new Snowflake(2, 3);
                }
            }
        }
        return instance;
    }
}
