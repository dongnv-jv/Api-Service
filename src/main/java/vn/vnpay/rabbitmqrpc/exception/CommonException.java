package vn.vnpay.rabbitmqrpc.exception;

public class CommonException extends RuntimeException {
    public CommonException(String message, Throwable cause) {
        super(message, cause);
    }
}
