package vn.vnpay.rabbitmqrpc.common;

public enum HttpStatus {
    SUCESS(200),
    REQUEST_TIMEOUT(408);

    private int code;

    HttpStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
