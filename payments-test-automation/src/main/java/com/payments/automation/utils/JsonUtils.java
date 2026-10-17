package com.payments.automation.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** JSON (de)serialisation + reading JSON test data from the classpath. */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Map<String, Object> toMap(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T readResource(String classpathResource, Class<T> type) {
        try (InputStream in = resource(classpathResource)) {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Map<String, Object>> readResourceAsList(String classpathResource) {
        try (InputStream in = resource(classpathResource)) {
            return MAPPER.readValue(in, new TypeReference<>() { });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String pretty(String json) {
        return toJson(readTree(json));
    }

    private static InputStream resource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return in;
    }
}
