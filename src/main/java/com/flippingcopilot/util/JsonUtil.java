package com.flippingcopilot.util;

import javax.json.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonUtil {
    public static JsonObject parseObject(String jsonStr) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonStr))) {
            return reader.readObject();
        }
    }

    public static JsonArray parseArray(String jsonStr) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonStr))) {
            return reader.readArray();
        }
    }

    public static String getString(JsonObject obj, String key) {
        return obj.getString(key, null);
    }

    public static long getLong(JsonObject obj, String key, long defaultValue) {
        try {
            JsonValue value = obj.get(key);
            if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
                return defaultValue;
            }
            return ((JsonNumber) value).longValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getInt(JsonObject obj, String key, int defaultValue) {
        try {
            return obj.getInt(key, defaultValue);
        } catch (NullPointerException | ClassCastException e) {
            return defaultValue;
        }
    }

    public static JsonObject getObject(JsonObject obj, String key) {
        try {
            return obj.getJsonObject(key);
        } catch (NullPointerException | ClassCastException e) {
            return null;
        }
    }

    public static List<String> getStringList(JsonArray array) {
        return array.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(Collectors.toList());
    }

    public static String toJsonString(Map<String, Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        map.forEach((key, value) -> {
            if (value instanceof String) {
                builder.add(key, (String) value);
            } else if (value instanceof Integer) {
                builder.add(key, (Integer) value);
            } else if (value instanceof Long) {
                builder.add(key, (Long) value);
            } else if (value instanceof Boolean) {
                builder.add(key, (Boolean) value);
            } else if (value instanceof Double) {
                builder.add(key, (Double) value);
            } else if (value instanceof List) {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                ((List<?>) value).forEach(item -> {
                    if (item instanceof String) {
                        arrayBuilder.add((String) item);
                    } else if (item instanceof Number) {
                        arrayBuilder.add(((Number) item).doubleValue());
                    }
                });
                builder.add(key, arrayBuilder);
            }
        });
        return builder.build().toString();
    }
}