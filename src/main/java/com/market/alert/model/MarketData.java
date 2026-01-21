package com.market.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String exchange;
    
    // Timestamp
    private LocalDateTime timestamp;
    
    // Price fields
    private BigDecimal ltp; // Last Traded Price
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal weekHigh52;
    private BigDecimal weekLow52;
    
    // Change metrics
    private BigDecimal daysChange;
    private BigDecimal daysChangePercent;
    private BigDecimal intradayChange;
    private BigDecimal intradayChangePercent;
    
    // Volume and OI (Open Interest)
    private Long volume;
    private Long openInterest;
    private BigDecimal oiDayChangePercent;
    private Long oiDayHigh;
    private Long oiDayLow;
    
    // Trading quantities
    private Long lastTradedQuantity;
    private BigDecimal averageTradedPrice;
    private Long totalBuyQuantity;
    private Long totalSellQuantity;
}
