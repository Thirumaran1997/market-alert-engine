package com.market.alert.service;

import com.market.alert.model.AlertCriteria;
import com.market.alert.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for evaluating market data against alert criteria
 */
@Slf4j
@Service
public class CriteriaEvaluator {
    
    /**
     * Evaluate if market data meets the alert criteria
     */
    public boolean evaluate(MarketData data, AlertCriteria criteria) {
        if (data == null || criteria == null) {
            return false;
        }
        
        // Symbol must match
        if (criteria.getSymbol() != null && !criteria.getSymbol().equals(data.getSymbol())) {
            return false;
        }
        
        // Evaluate all criteria conditions
        return evaluateLtp(data, criteria) &&
               evaluateWeek52(data, criteria) &&
               evaluateOpenPrice(data, criteria) &&
               evaluateClosePrice(data, criteria) &&
               evaluateDaysChange(data, criteria) &&
               evaluateDaysChangePercent(data, criteria) &&
               evaluateIntradayChange(data, criteria) &&
               evaluateIntradayChangePercent(data, criteria) &&
               evaluateVolume(data, criteria) &&
               evaluateOpenInterest(data, criteria) &&
               evaluateOiDayChangePercent(data, criteria) &&
               evaluateLastTradedQuantity(data, criteria) &&
               evaluateAverageTradedPrice(data, criteria) &&
               evaluateTotalBuyQuantity(data, criteria) &&
               evaluateTotalSellQuantity(data, criteria) &&
               evaluateOiDayHighLow(data, criteria);
    }
    
    private boolean evaluateLtp(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getLtp(), criteria.getLtpMinThreshold(), criteria.getLtpMaxThreshold());
    }
    
    private boolean evaluateWeek52(MarketData data, AlertCriteria criteria) {
        if (criteria.getWeekHigh52Threshold() != null && data.getWeekHigh52() != null) {
            if (data.getWeekHigh52().compareTo(criteria.getWeekHigh52Threshold()) < 0) {
                return false;
            }
        }
        if (criteria.getWeekLow52Threshold() != null && data.getWeekLow52() != null) {
            if (data.getWeekLow52().compareTo(criteria.getWeekLow52Threshold()) > 0) {
                return false;
            }
        }
        return true;
    }
    
    private boolean evaluateOpenPrice(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getOpenPrice(), criteria.getOpenPriceMinThreshold(), criteria.getOpenPriceMaxThreshold());
    }
    
    private boolean evaluateClosePrice(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getClosePrice(), criteria.getClosePriceMinThreshold(), criteria.getClosePriceMaxThreshold());
    }
    
    private boolean evaluateDaysChange(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getDaysChange(), criteria.getDaysChangeMinThreshold(), criteria.getDaysChangeMaxThreshold());
    }
    
    private boolean evaluateDaysChangePercent(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getDaysChangePercent(), criteria.getDaysChangePercentMinThreshold(), criteria.getDaysChangePercentMaxThreshold());
    }
    
    private boolean evaluateIntradayChange(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getIntradayChange(), criteria.getIntradayChangeMinThreshold(), criteria.getIntradayChangeMaxThreshold());
    }
    
    private boolean evaluateIntradayChangePercent(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getIntradayChangePercent(), criteria.getIntradayChangePercentMinThreshold(), criteria.getIntradayChangePercentMaxThreshold());
    }
    
    private boolean evaluateVolume(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getVolume(), criteria.getVolumeMinThreshold(), criteria.getVolumeMaxThreshold());
    }
    
    private boolean evaluateOpenInterest(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getOpenInterest(), criteria.getOpenInterestMinThreshold(), criteria.getOpenInterestMaxThreshold());
    }
    
    private boolean evaluateOiDayChangePercent(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getOiDayChangePercent(), criteria.getOiDayChangePercentMinThreshold(), criteria.getOiDayChangePercentMaxThreshold());
    }
    
    private boolean evaluateLastTradedQuantity(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getLastTradedQuantity(), criteria.getLastTradedQuantityMinThreshold(), criteria.getLastTradedQuantityMaxThreshold());
    }
    
    private boolean evaluateAverageTradedPrice(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getAverageTradedPrice(), criteria.getAverageTradedPriceMinThreshold(), criteria.getAverageTradedPriceMaxThreshold());
    }
    
    private boolean evaluateTotalBuyQuantity(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getTotalBuyQuantity(), criteria.getTotalBuyQuantityMinThreshold(), criteria.getTotalBuyQuantityMaxThreshold());
    }
    
    private boolean evaluateTotalSellQuantity(MarketData data, AlertCriteria criteria) {
        return evaluateRange(data.getTotalSellQuantity(), criteria.getTotalSellQuantityMinThreshold(), criteria.getTotalSellQuantityMaxThreshold());
    }
    
    private boolean evaluateOiDayHighLow(MarketData data, AlertCriteria criteria) {
        if (criteria.getOiDayHighThreshold() != null && data.getOiDayHigh() != null) {
            if (data.getOiDayHigh().compareTo(criteria.getOiDayHighThreshold()) < 0) {
                return false;
            }
        }
        if (criteria.getOiDayLowThreshold() != null && data.getOiDayLow() != null) {
            if (data.getOiDayLow().compareTo(criteria.getOiDayLowThreshold()) < 0) {
                return false;
            }
        }
        return true;
    }
    
    private boolean evaluateRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (min != null && value != null && value.compareTo(min) < 0) {
            return false;
        }
        if (max != null && value != null && value.compareTo(max) > 0) {
            return false;
        }
        return true;
    }
    
    private boolean evaluateRange(Long value, Long min, Long max) {
        if (min != null && value != null && value < min) {
            return false;
        }
        if (max != null && value != null && value > max) {
            return false;
        }
        return true;
    }
}
