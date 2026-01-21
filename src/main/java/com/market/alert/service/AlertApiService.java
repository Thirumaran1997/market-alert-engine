package com.market.alert.service;

import com.market.alert.model.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for invoking alert APIs asynchronously
 */
@Slf4j
@Service
public class AlertApiService {
    
    private final WebClient webClient;
    
    public AlertApiService() {
        this.webClient = WebClient.builder()
                .build();
    }
    
    /**
     * Invoke alert API asynchronously
     */
    @Async
    public void invokeAlert(String apiUrl, AlertEvent alertEvent) {
        log.info("Invoking alert API: {} for criteria: {}, symbol: {}", 
                apiUrl, alertEvent.getCriteriaId(), alertEvent.getSymbol());
        
        webClient.post()
                .uri(apiUrl)
                .bodyValue(alertEvent)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> 
                    log.info("Alert API invoked successfully for {}: {}", alertEvent.getSymbol(), response))
                .doOnError(error -> 
                    log.error("Error invoking alert API for {}: {}", alertEvent.getSymbol(), error.getMessage()))
                .onErrorResume(error -> Mono.empty())
                .subscribe();
    }
}
