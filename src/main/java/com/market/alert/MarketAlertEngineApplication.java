package com.market.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class MarketAlertEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketAlertEngineApplication.class, args);
    }
}
