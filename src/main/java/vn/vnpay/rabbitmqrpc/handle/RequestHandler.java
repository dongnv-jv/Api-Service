package vn.vnpay.rabbitmqrpc.handle;

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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class RequestHandler implements HttpHandler {
    static Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    @Autowire
    private RPCClient rpcClient;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        try {
            if (requestMethod.equalsIgnoreCase(RequestMethod.GET.name())) {
                handleGet(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.POST.name())) {
                handlePost(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.PUT.name())) {
                handlePut(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.DELETE.name())) {
                handleDelete(exchange);
            } else {
                sendResponse(exchange, "Method not supported", 405);
            }
        } catch (IllegalAccessException e) {
            logger.error("Handle request fail ", e);
        }


    }

    private void handleGet(HttpExchange httpExchange) throws IOException {
        Map<String, List<String>> params = ObjectConverter.splitQuery(httpExchange.getRequestURI().getRawQuery());
        String response = "GET request received. Params: " + params.toString();
        sendResponse(httpExchange, response);
    }

    private void handlePost(HttpExchange httpExchange) throws IllegalAccessException {
        GeneralResponse<ResponsePayment> responseRaw = new GeneralResponse<>();
        GeneralResponse<Long> response = new GeneralResponse<>();
        long start = System.currentTimeMillis();
        logger.info("Start handle request POST in RequestHandler");
        try {
            InetSocketAddress clientAddress = httpExchange.getRemoteAddress();
            String clientIP = clientAddress.getAddress().getHostAddress();
            logger.info("Handle request with clientIp {} ", clientIP);
            InputStream is = httpExchange.getRequestBody();
            byte[] bytes = ObjectConverter.getBytesFromInputStream(is);
            PaymentRequest paymentRequest = ObjectConverter.bytesToObject(bytes, PaymentRequest.class);
            paymentRequest.setToken(UUID.randomUUID().toString());
            logger.info("Handle request with requestBody: {} ", ObjectConverter.objectToJson(paymentRequest));
            CompletableFuture<GeneralResponse<ResponsePayment>> future = rpcClient.processRPC(paymentRequest, 120000, httpExchange);
            responseRaw = future.join();
            if (responseRaw != null && responseRaw.getData() != null) {
                response.setCode(responseRaw.getCode());
                response.setMessage(responseRaw.getMessage());
                response.setData(responseRaw.getData().getId());
            }
            this.sendResponse(httpExchange, response);
            long end = System.currentTimeMillis();
            logger.info("Process request in RequestHandler take {} millisecond", (end - start));
        } catch (Exception e) {
            logger.error("Error processing payment request", e);
        } finally {
            httpExchange.close();
        }
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
