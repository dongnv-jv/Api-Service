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
import vn.vnpay.rabbitmqrpc.common.HttpStatus;
import vn.vnpay.rabbitmqrpc.common.ObjectConverter;
import vn.vnpay.rabbitmqrpc.common.RequestMethod;
import vn.vnpay.rabbitmqrpc.factory.RPCClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class RequestHandler implements HttpHandler {
    static Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    @Autowire
    private RPCClient rpcClient;

    @Override
    public void handle(HttpExchange exchange) {
        String requestMethod = exchange.getRequestMethod();
        logger.info("Received {} request with endpoint: {}", requestMethod, exchange.getRequestURI());
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
        Map<String, List<String>> params = ObjectConverter.splitQuery(httpExchange.getRequestURI().getRawQuery());
        String response = "GET request received. Params: " + params.toString();
        sendResponse(httpExchange, response);
    }


    private void handlePost(HttpExchange httpExchange) {
        long start = System.currentTimeMillis();
        logger.info("Start handle request POST in RequestHandler");
        try {
            String clientIP = this.getClientIP(httpExchange);
            logger.info("Handle request with clientIp {} ", clientIP);
            PaymentRequest paymentRequest = this.getPaymentRequestBody(httpExchange);
            GeneralResponse<Long> response = this.processPaymentRequest(paymentRequest, httpExchange);
            this.sendResponse(httpExchange, response);
            long end = System.currentTimeMillis();
            logger.info("Process request in RequestHandler take {} millisecond", (end - start));
        } catch (Exception e) {
            logger.error("Error processing payment request", e);
        } finally {
            httpExchange.close();
        }
    }

    private String getClientIP(HttpExchange httpExchange) {
        InetSocketAddress clientAddress = httpExchange.getRemoteAddress();
        return clientAddress.getAddress().getHostAddress();
    }

    private PaymentRequest getPaymentRequestBody(HttpExchange httpExchange) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        byte[] bytes = ObjectConverter.getBytesFromInputStream(is);
        PaymentRequest paymentRequest = ObjectConverter.bytesToObject(bytes, PaymentRequest.class);
        logger.info("Handle request with requestBody: {} ", ObjectConverter.objectToJson(paymentRequest));
        return paymentRequest;
    }

    private GeneralResponse<Long> processPaymentRequest(PaymentRequest paymentRequest, HttpExchange httpExchange) throws JsonProcessingException {
        GeneralResponse<ResponsePayment> responseRaw;
        GeneralResponse<Long> response = new GeneralResponse<>();
            CompletableFuture<GeneralResponse<ResponsePayment>> future = rpcClient.processRPC(paymentRequest, 120000, httpExchange);
            responseRaw = future.join();
        if (responseRaw != null && responseRaw.getData() != null) {
            response.setCode(responseRaw.getCode());
            response.setMessage(responseRaw.getMessage());
            response.setData(responseRaw.getData().getId());
            logger.info("Received response from RabbitMq successfully with correlationId: {} response: {} ",
                    responseRaw.getData().getToken(), ObjectConverter.objectToJson(response));
        } else {
            logger.info("Received response from Rabbit failed");
        }
        return response;
    }

    private void handlePut(HttpExchange t) throws IOException {
        InputStream is = t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String requestBody = reader.readLine();
        String response = "PUT request received. Data: " + requestBody;
        sendResponse(t, response);
    }

    private void handleDelete(HttpExchange t) throws IOException {
        String response = "DELETE request received";
        sendResponse(t, response);
    }

    private void sendResponse(HttpExchange httpExchange, Object response, int statusCode) throws IOException {
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        String responseString = ObjectConverter.objectToJson(response);
        httpExchange.sendResponseHeaders(statusCode, responseString.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseString.getBytes());
        os.close();
    }

    private void sendResponse(HttpExchange httpExchange, Object response) throws IOException {
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }
}
