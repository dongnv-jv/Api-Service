package vn.vnpay.rabbitmqrpc.bean;

public class ResponsePayment {
    private long id;
    private String token;

    public long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
}
