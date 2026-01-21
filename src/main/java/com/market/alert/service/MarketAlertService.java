package com.market.alert.service;

import com.market.alert.client.TcpFeedClient;
import com.market.alert.config.AlertProperties;
import com.market.alert.model.AlertCriteria;
import com.market.alert.model.AlertEvent;
import com.market.alert.model.MarketData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Main orchestration service for market alert engine
 */
@Slf4j
@Service
public class MarketAlertService {
    
    private final TcpFeedClient tcpFeedClient;
    private final CriteriaEvaluator criteriaEvaluator;
    private final AlertApiService alertApiService;
    private final AlertProperties alertProperties;
    
    public MarketAlertService(
            TcpFeedClient tcpFeedClient,
            CriteriaEvaluator criteriaEvaluator,
            AlertApiService alertApiService,
            AlertProperties alertProperties) {
        this.tcpFeedClient = tcpFeedClient;
        this.criteriaEvaluator = criteriaEvaluator;
        this.alertApiService = alertApiService;
        this.alertProperties = alertProperties;
    }
    
    @PostConstruct
    public void start() {
        log.info("Starting Market Alert Engine...");
        tcpFeedClient.start(this::processMarketData);
        log.info("Market Alert Engine started with {} criteria", alertProperties.getCriteria().size());
    }
    
    @PreDestroy
    public void stop() {
        log.info("Stopping Market Alert Engine...");
        tcpFeedClient.stop();
        log.info("Market Alert Engine stopped");
    }
    
    /**
     * Process incoming market data and evaluate against all criteria
     */
    private void processMarketData(MarketData data) {
        log.debug("Processing market data for symbol: {}", data.getSymbol());
        
        List<AlertCriteria> criteriaList = alertProperties.getCriteria();
        
        for (AlertCriteria criteria : criteriaList) {
            try {
                if (criteriaEvaluator.evaluate(data, criteria)) {
                    triggerAlert(data, criteria);
                }
            } catch (Exception e) {
                log.error("Error evaluating criteria {} for symbol {}", 
                        criteria.getCriteriaId(), data.getSymbol(), e);
            }
        }
    }
    
    /**
     * Trigger alert by invoking API
     */
    private void triggerAlert(MarketData data, AlertCriteria criteria) {
        log.info("Alert triggered for criteria: {}, symbol: {}", criteria.getCriteriaId(), data.getSymbol());
        
        AlertEvent alertEvent = AlertEvent.builder()
                .criteriaId(criteria.getCriteriaId())
                .symbol(data.getSymbol())
                .triggeredAt(LocalDateTime.now())
                .marketData(data)
                .message(buildAlertMessage(data, criteria))
                .build();
        
        String apiUrl = criteria.getAlertApiUrl() != null ? 
                criteria.getAlertApiUrl() : alertProperties.getDefaultAlertApiUrl();
        
        if (apiUrl != null && !apiUrl.isEmpty()) {
            alertApiService.invokeAlert(apiUrl, alertEvent);
        } else {
            log.warn("No alert API URL configured for criteria: {}", criteria.getCriteriaId());
        }
    }
    
    private String buildAlertMessage(MarketData data, AlertCriteria criteria) {
        return String.format("Alert: %s - Symbol: %s, LTP: %s, Volume: %s", 
                criteria.getCriteriaId(), 
                data.getSymbol(), 
                data.getLtp(), 
                data.getVolume());
    }
}
