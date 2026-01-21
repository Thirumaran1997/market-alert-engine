package com.market.alert.client;

import com.market.alert.config.TcpFeedProperties;
import com.market.alert.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TcpFeedClientTest {
    
    private TcpFeedClient client;
    private TcpFeedProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new TcpFeedProperties();
        properties.setHost("localhost");
        properties.setPort(5000);
        properties.setAutoReconnect(false);
        client = new TcpFeedClient(properties);
    }
    
    @Test
    void testClientCanBeInstantiated() {
        assertNotNull(client);
    }
    
    @Test
    void testPropertiesAreSet() {
        assertEquals("localhost", properties.getHost());
        assertEquals(5000, properties.getPort());
        assertFalse(properties.isAutoReconnect());
    }
}
