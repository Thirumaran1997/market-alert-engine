package com.market.alert.service;

import com.market.alert.model.AlertCriteria;
import com.market.alert.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CriteriaEvaluatorTest {
    
    private CriteriaEvaluator evaluator;
    
    @BeforeEach
    void setUp() {
        evaluator = new CriteriaEvaluator();
    }
    
    @Test
    void testEvaluateLtpInRange() {
        MarketData data = MarketData.builder()
                .symbol("AAPL")
                .ltp(new BigDecimal("150.00"))
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("AAPL")
                .ltpMinThreshold(new BigDecimal("140.00"))
                .ltpMaxThreshold(new BigDecimal("160.00"))
                .build();
        
        assertTrue(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateLtpOutOfRange() {
        MarketData data = MarketData.builder()
                .symbol("AAPL")
                .ltp(new BigDecimal("170.00"))
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("AAPL")
                .ltpMinThreshold(new BigDecimal("140.00"))
                .ltpMaxThreshold(new BigDecimal("160.00"))
                .build();
        
        assertFalse(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateVolumeThreshold() {
        MarketData data = MarketData.builder()
                .symbol("GOOGL")
                .volume(2000000L)
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("GOOGL")
                .volumeMinThreshold(1000000L)
                .build();
        
        assertTrue(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateVolumeBelowThreshold() {
        MarketData data = MarketData.builder()
                .symbol("GOOGL")
                .volume(500000L)
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("GOOGL")
                .volumeMinThreshold(1000000L)
                .build();
        
        assertFalse(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateDaysChangePercent() {
        MarketData data = MarketData.builder()
                .symbol("MSFT")
                .daysChangePercent(new BigDecimal("5.5"))
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("MSFT")
                .daysChangePercentMinThreshold(new BigDecimal("5.0"))
                .build();
        
        assertTrue(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateSymbolMismatch() {
        MarketData data = MarketData.builder()
                .symbol("AAPL")
                .ltp(new BigDecimal("150.00"))
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("GOOGL")
                .ltpMinThreshold(new BigDecimal("100.00"))
                .build();
        
        assertFalse(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateMultipleCriteria() {
        MarketData data = MarketData.builder()
                .symbol("AAPL")
                .ltp(new BigDecimal("150.00"))
                .volume(2000000L)
                .daysChangePercent(new BigDecimal("3.0"))
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("AAPL")
                .ltpMinThreshold(new BigDecimal("140.00"))
                .volumeMinThreshold(1000000L)
                .daysChangePercentMinThreshold(new BigDecimal("2.0"))
                .build();
        
        assertTrue(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateWithNullValues() {
        MarketData data = MarketData.builder()
                .symbol("AAPL")
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("AAPL")
                .build();
        
        assertTrue(evaluator.evaluate(data, criteria));
    }
    
    @Test
    void testEvaluateOpenInterestThresholds() {
        MarketData data = MarketData.builder()
                .symbol("TSLA")
                .openInterest(150000L)
                .oiDayHigh(160000L)
                .oiDayLow(140000L)
                .build();
        
        AlertCriteria criteria = AlertCriteria.builder()
                .symbol("TSLA")
                .openInterestMinThreshold(100000L)
                .oiDayHighThreshold(150000L)
                .oiDayLowThreshold(130000L)
                .build();
        
        assertTrue(evaluator.evaluate(data, criteria));
    }
}
