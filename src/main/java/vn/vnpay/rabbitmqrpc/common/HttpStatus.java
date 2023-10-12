package vn.vnpay.rabbitmqrpc.common;

public enum HttpStatus {
    SUCCESS(200),
    REQUEST_TIMEOUT(408);

    private final int code;

    HttpStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
