package com.market.alert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for TCP feed connection
 */
@Data
@Component
@ConfigurationProperties(prefix = "market.feed.tcp")
public class TcpFeedProperties {
    
    private String host = "localhost";
    private int port = 5000;
    private int reconnectDelaySeconds = 5;
    private int bufferSize = 8192;
    private boolean autoReconnect = true;
}
