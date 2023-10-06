package org.example.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.annotation.CustomValue;
import org.example.bean.Student;
import org.example.common.ObjectConverter;
import org.example.common.RequestMethod;
import org.example.factory.PaymentRequest;
import org.example.service.Producer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestHandler implements HttpHandler {

    @CustomValue("charset.Name")
    private String charSet;
    @CustomValue("exchange.direct.queueName")
    private String queueName;
    @CustomValue("exchange.direct.name")
    private String exchange;
    @CustomValue("exchange.direct.routingKey")
    private String routingKey;
    @CustomValue("exchange.dead.letter.queueName")
    private String deadLetterQueueName;
    @CustomValue("consumer.prefetchCount")
    private int prefetchCount;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

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
    }

    private void handleGet(HttpExchange t) throws IOException {
        Map<String, List<String>> params = splitQuery(t.getRequestURI().getRawQuery());
        String response = "GET request received. Params: " + params.toString();
        sendResponse(t, response);
    }

    private void handlePost(HttpExchange t) throws IOException {
//TODO Get IP address
        InetSocketAddress clientAddress = t.getRemoteAddress();
        String clientIP = clientAddress.getAddress().getHostAddress();


        // Đọc dữ liệu từ request body
        InputStream is = t.getRequestBody();
        final byte[] bytes;
        bytes = getBytesFromInputStream(is);

        PaymentRequest paymentRequest = ObjectConverter.bytesToObject(bytes, PaymentRequest.class);
        paymentRequest.setToken(UUID.randomUUID().toString());


        Producer producer = new Producer();


        producer.sendMessage(bytes, routingKey, exchange, null);



        String response = ObjectConverter.objectToJson(paymentRequest);
        sendResponse(t, response);
    }

    private void handlePut(HttpExchange t) throws IOException {
        // Đọc dữ liệu từ request body
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

    private void sendResponse(HttpExchange t, String response, int statusCode) throws IOException {
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(statusCode, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void sendResponse(HttpExchange t, String response) throws IOException {
        sendResponse(t, response, 200);
    }

    private Map<String, List<String>> splitQuery(String query) {
        // Hàm này giúp bạn lấy ra các tham số từ URL (ví dụ: ?param1=value1&param2=value2)
        return null;
    }

    public byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }
}
