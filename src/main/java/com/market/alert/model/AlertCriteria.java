package com.market.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Alert criteria for evaluating market data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCriteria {
    
    private String criteriaId;
    private String symbol;
    
    // LTP criteria
    private BigDecimal ltpMinThreshold;
    private BigDecimal ltpMaxThreshold;
    
    // 52-week criteria
    private BigDecimal weekHigh52Threshold;
    private BigDecimal weekLow52Threshold;
    
    // Price criteria
    private BigDecimal openPriceMinThreshold;
    private BigDecimal openPriceMaxThreshold;
    private BigDecimal closePriceMinThreshold;
    private BigDecimal closePriceMaxThreshold;
    
    // Change criteria
    private BigDecimal daysChangeMinThreshold;
    private BigDecimal daysChangeMaxThreshold;
    private BigDecimal daysChangePercentMinThreshold;
    private BigDecimal daysChangePercentMaxThreshold;
    private BigDecimal intradayChangeMinThreshold;
    private BigDecimal intradayChangeMaxThreshold;
    private BigDecimal intradayChangePercentMinThreshold;
    private BigDecimal intradayChangePercentMaxThreshold;
    
    // Volume and OI criteria
    private Long volumeMinThreshold;
    private Long volumeMaxThreshold;
    private Long openInterestMinThreshold;
    private Long openInterestMaxThreshold;
    private BigDecimal oiDayChangePercentMinThreshold;
    private BigDecimal oiDayChangePercentMaxThreshold;
    
    // Trading quantity criteria
    private Long lastTradedQuantityMinThreshold;
    private Long lastTradedQuantityMaxThreshold;
    private BigDecimal averageTradedPriceMinThreshold;
    private BigDecimal averageTradedPriceMaxThreshold;
    private Long totalBuyQuantityMinThreshold;
    private Long totalBuyQuantityMaxThreshold;
    private Long totalSellQuantityMinThreshold;
    private Long totalSellQuantityMaxThreshold;
    private Long oiDayHighThreshold;
    private Long oiDayLowThreshold;
    
    // Alert action
    private String alertApiUrl;
}
