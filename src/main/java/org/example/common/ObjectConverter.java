package org.example.common;

import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ObjectConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ObjectConverter() {
    }

    public static byte[] objectToBytes(Object object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }

    public static <T> T bytesToObject(byte[] bytes, Class<T> clazz) throws IOException {
        return objectMapper.readValue(bytes, clazz);
    }

    public static String objectToJson(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    public static LocalDateTime convertLongToLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
