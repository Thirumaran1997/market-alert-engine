package com.market.alert.config;

import com.market.alert.model.AlertCriteria;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for alert criteria
 */
@Data
@Component
@ConfigurationProperties(prefix = "market.alert")
public class AlertProperties {
    
    private List<AlertCriteria> criteria = new ArrayList<>();
    private String defaultAlertApiUrl;
}
