package com.flippingcopilot.controller;

import com.flippingcopilot.model.PriceDataPoint;
import com.flippingcopilot.util.HttpUtil;
import com.flippingcopilot.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class PriceHistoryService {
    private static final String API_BASE_URL = "https://prices.runescape.wiki/api/v1/osrs/timeseries";
    private static final int CACHE_DURATION_MINUTES = 5;
    
    private final Map<Integer, CachedPriceData> priceCache;

    @Inject
    public PriceHistoryService() {
        this.priceCache = new HashMap<>();
    }

    public List<PriceDataPoint> getPriceHistory(int itemId) {
        log.debug("Getting price history for item {}", itemId);
        
        // Check cache first
        CachedPriceData cachedData = priceCache.get(itemId);
        if (cachedData != null && !cachedData.isExpired()) {
            log.debug("Returning cached data for item {}", itemId);
            return cachedData.getData();
        }

        // Fetch new data
        try {
            log.debug("Fetching new price data for item {}", itemId);
            List<PriceDataPoint> data = fetchPriceHistory(itemId);
            log.debug("Fetched {} price points for item {}", data.size(), itemId);
            priceCache.put(itemId, new CachedPriceData(data));
            return data;
        } catch (Exception e) {
            log.error("Failed to fetch price history for item " + itemId, e);
            if (cachedData != null) {
                log.debug("Returning expired cached data for item {}", itemId);
                return cachedData.getData();
            }
            log.debug("No cached data available for item {}", itemId);
            return new ArrayList<>();
        }
    }

    private List<PriceDataPoint> fetchPriceHistory(int itemId) throws IOException, InterruptedException {
        // Get price data with 5-minute intervals
        String url = API_BASE_URL + "?timestep=5m&id=" + itemId;
        log.debug("Fetching price data from URL: {}", url);

        String jsonStr = HttpUtil.get(url);
        log.debug("Received JSON response: {}", jsonStr);
        
        JsonObject json = JsonUtil.parseObject(jsonStr);
        
        // Check if we have data
        JsonValue dataValue = json.get("data");
        if (dataValue == null || dataValue.getValueType() == JsonValue.ValueType.NULL) {
            log.debug("No data found in response for item {}", itemId);
            return new ArrayList<>();
        }

        List<PriceDataPoint> priceHistory = new ArrayList<>();
        long eighteenHoursAgo = System.currentTimeMillis() / 1000 - (18 * 60 * 60);

        if (dataValue.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonObject data = dataValue.asJsonObject();
            for (Map.Entry<String, JsonValue> entry : data.entrySet()) {
                try {
                    long timestamp = Long.parseLong(entry.getKey());
                    if (timestamp >= eighteenHoursAgo) {
                        JsonObject point = entry.getValue().asJsonObject();
                        priceHistory.add(new PriceDataPoint(
                            Instant.ofEpochSecond(timestamp),
                            JsonUtil.getLong(point, "avgHighPrice", 0),
                            JsonUtil.getLong(point, "avgLowPrice", 0),
                            JsonUtil.getLong(point, "highPriceVolume", 0),
                            JsonUtil.getLong(point, "lowPriceVolume", 0)
                        ));
                    }
                } catch (Exception e) {
                    log.error("Error parsing price point: {}", entry, e);
                }
            }
        } else if (dataValue.getValueType() == JsonValue.ValueType.ARRAY) {
            JsonArray dataArray = dataValue.asJsonArray();
            for (JsonValue element : dataArray) {
                try {
                    JsonObject point = element.asJsonObject();
                    long timestamp = JsonUtil.getLong(point, "timestamp", 0);
                    if (timestamp >= eighteenHoursAgo) {
                        priceHistory.add(new PriceDataPoint(
                            Instant.ofEpochSecond(timestamp),
                            JsonUtil.getLong(point, "avgHighPrice", 0),
                            JsonUtil.getLong(point, "avgLowPrice", 0),
                            JsonUtil.getLong(point, "highPriceVolume", 0),
                            JsonUtil.getLong(point, "lowPriceVolume", 0)
                        ));
                    }
                } catch (Exception e) {
                    log.error("Error parsing price point from array: {}", element, e);
                }
            }
        }

        return priceHistory;
    }

    private static class CachedPriceData {
        private final List<PriceDataPoint> data;
        private final Instant timestamp;

        public CachedPriceData(List<PriceDataPoint> data) {
            this.data = data;
            this.timestamp = Instant.now();
        }

        public List<PriceDataPoint> getData() {
            return data;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(timestamp.plus(CACHE_DURATION_MINUTES, ChronoUnit.MINUTES));
        }
    }
}