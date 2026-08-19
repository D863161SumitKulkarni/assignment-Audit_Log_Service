package com.auditlog.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonUtil {

    private final ObjectMapper objectMapper;
    private final ObjectWriter canonicalWriter;

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.canonicalWriter = objectMapper.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public String toCanonicalJson(Object value) {
        try {
            // Null values are intentionally preserved because they are part of the committed representation.
            return canonicalWriter.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Unable to serialize value to canonical JSON", exception);
        }
    }

    public Map<String, Object> fromJsonToMap(String json) {
        try {
            return objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {
                    });
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unable to deserialize JSON into a map", exception);
        }
    }
}
