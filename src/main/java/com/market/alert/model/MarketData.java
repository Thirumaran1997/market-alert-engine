package com.market.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Market data model containing all fields for alert criteria evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    
    // Instrument identification
    private String symbol;
    
    // Sequence and timestamps
    private Long sequenceNumber;
    private Long udpReceptionTimestamp;
    private Long publisherTimestamp;
    private LocalDateTime timestamp;
    
    // Price fields from packet
    private BigDecimal ltp; // Last Traded Price
    private Long volume;
    private Long openInterest;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal previousDayClose;
    private BigDecimal upperCircuit;
    private BigDecimal lowerCircuit;
    private Long lastTradedQuantity;
    
    // Time and expiry fields
    private String lastTradedTime;
    private String expiry;
    
    // Calculated fields for alert criteria
    private BigDecimal weekHigh52;
    private BigDecimal weekLow52;
    private BigDecimal daysChange;
    private BigDecimal daysChangePercent;
    private BigDecimal intradayChange;
    private BigDecimal intradayChangePercent;
    private BigDecimal oiDayChangePercent;
    private Long oiDayHigh;
    private Long oiDayLow;
    private BigDecimal averageTradedPrice;
    private Long totalBuyQuantity;
    private Long totalSellQuantity;
    
    // Market depth
    private List<DepthLevel> bids;
    private List<DepthLevel> asks;
    
    /**
     * Depth level for bid/ask
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepthLevel {
        private double price;
        private int quantity;
        private int orders;
    }
}
