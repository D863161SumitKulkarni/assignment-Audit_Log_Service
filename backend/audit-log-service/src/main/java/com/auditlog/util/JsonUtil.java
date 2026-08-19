package com.auditlog.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String toCanonicalJson(Object value) {
        try {
            // Null values are intentionally preserved because they are part of the committed representation.
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Unable to serialize value to canonical JSON", exception);
        }
    }

    public Map<String, Object> fromJsonToMap(String json) {
        try {
            return objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {
                    });
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unable to deserialize JSON into a map", exception);
        }
    }
}
