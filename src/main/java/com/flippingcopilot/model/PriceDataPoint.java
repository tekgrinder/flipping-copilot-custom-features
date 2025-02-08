package com.flippingcopilot.model;

import lombok.Data;
import java.time.Instant;

@Data
public class PriceDataPoint {
    private final Instant timestamp;
    private final long avgHighPrice;
    private final long avgLowPrice;
    private final long highPriceVolume;
    private final long lowPriceVolume;
}