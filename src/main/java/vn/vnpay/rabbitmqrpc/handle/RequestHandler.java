package vn.vnpay.rabbitmqrpc.handle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Autowire;
import vn.vnpay.rabbitmqrpc.annotation.Component;
import vn.vnpay.rabbitmqrpc.bean.GeneralResponse;
import vn.vnpay.rabbitmqrpc.bean.PaymentRequest;
import vn.vnpay.rabbitmqrpc.bean.ResponsePayment;
import vn.vnpay.rabbitmqrpc.common.CommonUtil;
import vn.vnpay.rabbitmqrpc.common.HttpStatus;
import vn.vnpay.rabbitmqrpc.common.RequestMethod;
import vn.vnpay.rabbitmqrpc.factory.RPCClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class RequestHandler implements HttpHandler {
    public static final ThreadLocal<String> logIdThreadLocal = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    @Autowire
    private RPCClient rpcClient;

    @Override
    public void handle(HttpExchange exchange) {
        String requestMethod = exchange.getRequestMethod();
        String logId = CommonUtil.generateLogId();
        logIdThreadLocal.set(logId);
        logger.info("[{}] - Received {} request with endpoint: {}", logId, requestMethod, exchange.getRequestURI());
        try {
            if (requestMethod.equalsIgnoreCase(RequestMethod.GET.name())) {
                this.handleGet(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.POST.name())) {
                this.handlePost(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.PUT.name())) {
                this.handlePut(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.DELETE.name())) {
                this.handleDelete(exchange);
            } else {
                sendResponse(exchange, "Method not supported", 405);
            }
        } catch (IOException e) {
            logger.error("Handle request fail ", e);
        }
    }

    private void handleGet(HttpExchange httpExchange) throws IOException {
        Map<String, List<String>> params = CommonUtil.splitQuery(httpExchange.getRequestURI().getRawQuery());
        String response = "GET request received. Params: " + params;
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }

    private void handlePost(HttpExchange httpExchange) {
        long start = System.currentTimeMillis();
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start handle request POST in RequestHandler", logId);
        GeneralResponse<Long> response;
        try {
            this.logClientIP(httpExchange);
            PaymentRequest paymentRequest = this.getPaymentRequestBody(httpExchange);
            response = this.processPaymentRequest(paymentRequest, httpExchange);
            this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
            long end = System.currentTimeMillis();
            logger.info("[{}] - Process request in RequestHandler take {} millisecond ", logId, (end - start));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            logger.error("[{}] - Error processing payment request because occur error when processRPC", logId, e);
        } catch (IOException e) {
            logger.error("[{}] - Error processing payment request because occur error when getPaymentRequestBody", logId, e);
            this.sendResponse(httpExchange, HttpStatus.INTERNAL_SERVER_ERROR.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        } catch (Exception e) {
            logger.error("[{}] - Error processing payment request", logId, e);
            this.sendResponse(httpExchange, HttpStatus.INTERNAL_SERVER_ERROR.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        } finally {
            httpExchange.close();
        }
    }

    private void logClientIP(HttpExchange httpExchange) {
        String logId = logIdThreadLocal.get();
        try {
            String clientIP = httpExchange.getRemoteAddress().getAddress().getHostAddress();
            logger.info("[{}] - Handle request with clientIp {} ", logId, clientIP);
        } catch (Exception e) {
            logger.error("[{}] - Error getting client IP", logId, e);
        }
    }

    private PaymentRequest getPaymentRequestBody(HttpExchange httpExchange) throws IOException {
        String logId = logIdThreadLocal.get();
        InputStream is = httpExchange.getRequestBody();
        byte[] bytes = CommonUtil.getBytesFromInputStream(is);
        PaymentRequest paymentRequest = CommonUtil.bytesToObject(bytes, PaymentRequest.class);
        String responseLog = CommonUtil.objectToJson(paymentRequest);
        logger.info("[{}] - Handle request with requestBody: {} ", logId, responseLog);
        return paymentRequest;
    }

    private GeneralResponse<Long> processPaymentRequest(PaymentRequest paymentRequest, HttpExchange httpExchange)
            throws JsonProcessingException, ExecutionException, InterruptedException {
        String logId = logIdThreadLocal.get();
        GeneralResponse<ResponsePayment> responseRaw = null;
        GeneralResponse<Long> response = new GeneralResponse<>();
        CompletableFuture<GeneralResponse<ResponsePayment>> future = rpcClient.processRPC(paymentRequest, 120000, httpExchange);
        responseRaw = future.get();
        if (responseRaw != null) {
            String responseLog = CommonUtil.objectToJson(responseRaw);
            logger.info("[{}] - Received response from RabbitMq successfully with response: {} ", logId, responseLog);
            response.setCode(responseRaw.getCode());
            response.setMessage(responseRaw.getMessage());
            if (responseRaw.getData() != null) {
                response.setData(responseRaw.getData().getId());
            }
        }
        String responseLog = CommonUtil.objectToJson(response);
        logger.info("[{}] - Process payment request successfully with response {} ", logId, responseLog);
        return response;
    }

    private void handlePut(HttpExchange httpExchange) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String requestBody = reader.readLine();
        String response = "PUT request received. Data: " + requestBody;
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }

    private void handleDelete(HttpExchange httpExchange) {
        String response = "DELETE request received";
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }

    private void sendResponse(HttpExchange httpExchange, Object response, int statusCode) {
        String logId = logIdThreadLocal.get();
        try {
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            String responseString = CommonUtil.objectToJson(response);
            httpExchange.sendResponseHeaders(statusCode, responseString.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(responseString.getBytes());
            os.close();
        } catch (IOException e) {
            logger.error("[{}] - Occur error when write response", logId, e);
        }
    }

}
