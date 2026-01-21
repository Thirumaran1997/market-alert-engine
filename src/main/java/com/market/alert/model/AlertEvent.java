package com.market.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Alert event triggered when criteria is met
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    
    private String criteriaId;
    private String symbol;
    private LocalDateTime triggeredAt;
    private MarketData marketData;
    private String message;
}
