package com.flippingcopilot.controller;

import com.flippingcopilot.model.PriceDataPoint;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class PriceHistoryService {
    private static final String API_BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
    private static final int CACHE_DURATION_MINUTES = 5;
    
    private final OkHttpClient httpClient;
    private final Map<Integer, CachedPriceData> priceCache;

    @Inject
    public PriceHistoryService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
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
        } catch (IOException e) {
            log.error("Failed to fetch price history for item " + itemId, e);
            if (cachedData != null) {
                log.debug("Returning expired cached data for item {}", itemId);
                return cachedData.getData();
            }
            log.debug("No cached data available for item {}", itemId);
            return new ArrayList<>();
        }
    }

    private List<PriceDataPoint> fetchPriceHistory(int itemId) throws IOException {
        // Get 6-hour data for the last week
        String url = API_BASE_URL + "/timeseries?timestep=6h&id=" + itemId;
        log.debug("Fetching price data from URL: {}", url);

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Flipping Copilot - Price History Graph")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected response " + response);
            }

            String jsonStr = response.body().string();
            JsonElement element = JsonParser.parseString(jsonStr);
            JsonObject json = element.getAsJsonObject();
            JsonObject data = json.getAsJsonObject("data");

            List<PriceDataPoint> priceHistory = new ArrayList<>();
            data.entrySet().forEach(entry -> {
                JsonObject point = entry.getValue().getAsJsonObject();
                priceHistory.add(new PriceDataPoint(
                    Instant.ofEpochSecond(Long.parseLong(entry.getKey())),
                    point.get("avgHighPrice").getAsLong(),
                    point.get("avgLowPrice").getAsLong(),
                    point.get("highPriceVolume").getAsLong(),
                    point.get("lowPriceVolume").getAsLong()
                ));
            });

            return priceHistory;
        }
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