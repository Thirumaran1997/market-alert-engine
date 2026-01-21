package com.market.alert.client;

import com.market.alert.config.TcpFeedProperties;
import com.market.alert.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * TCP client for consuming market data feed
 * Assumes fixed format: field1|field2|field3|...
 */
@Slf4j
@Component
public class TcpFeedClient {
    
    private final TcpFeedProperties properties;
    private Socket socket;
    private BufferedReader reader;
    private volatile boolean running = false;
    private Thread readerThread;
    
    public TcpFeedClient(TcpFeedProperties properties) {
        this.properties = properties;
    }
    
    /**
     * Start consuming data from TCP feed
     */
    public void start(Consumer<MarketData> dataConsumer) {
        if (running) {
            log.warn("TCP feed client is already running");
            return;
        }
        
        running = true;
        readerThread = new Thread(() -> {
            while (running) {
                try {
                    connect();
                    readData(dataConsumer);
                } catch (Exception e) {
                    log.error("Error in TCP feed client", e);
                    closeConnection();
                    
                    if (running && properties.isAutoReconnect()) {
                        try {
                            log.info("Reconnecting in {} seconds...", properties.getReconnectDelaySeconds());
                            Thread.sleep(properties.getReconnectDelaySeconds() * 1000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        });
        readerThread.setName("tcp-feed-reader");
        readerThread.start();
        log.info("TCP feed client started");
    }
    
    /**
     * Stop consuming data
     */
    public void stop() {
        running = false;
        closeConnection();
        if (readerThread != null) {
            readerThread.interrupt();
        }
        log.info("TCP feed client stopped");
    }
    
    private void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            log.info("Connecting to TCP feed at {}:{}", properties.getHost(), properties.getPort());
            socket = new Socket(properties.getHost(), properties.getPort());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()), properties.getBufferSize());
            log.info("Connected to TCP feed");
        }
    }
    
    private void readData(Consumer<MarketData> dataConsumer) throws IOException {
        String line;
        while (running && (line = reader.readLine()) != null) {
            try {
                MarketData data = parseMarketData(line);
                if (data != null) {
                    dataConsumer.accept(data);
                }
            } catch (Exception e) {
                log.error("Error parsing market data: {}", line, e);
            }
        }
    }
    
    /**
     * Parse fixed format TCP data
     * Expected format (pipe-separated):
     * symbol|exchange|ltp|openPrice|closePrice|weekHigh52|weekLow52|daysChange|daysChangePercent|
     * intradayChange|intradayChangePercent|volume|openInterest|oiDayChangePercent|oiDayHigh|oiDayLow|
     * lastTradedQuantity|averageTradedPrice|totalBuyQuantity|totalSellQuantity
     */
    private MarketData parseMarketData(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String[] fields = line.split("\\|");
        if (fields.length < 20) {
            log.warn("Invalid data format, expected at least 20 fields, got {}", fields.length);
            return null;
        }
        
        try {
            return MarketData.builder()
                    .timestamp(LocalDateTime.now())
                    .symbol(parseString(fields[0]))
                    .exchange(parseString(fields[1]))
                    .ltp(parseBigDecimal(fields[2]))
                    .openPrice(parseBigDecimal(fields[3]))
                    .closePrice(parseBigDecimal(fields[4]))
                    .weekHigh52(parseBigDecimal(fields[5]))
                    .weekLow52(parseBigDecimal(fields[6]))
                    .daysChange(parseBigDecimal(fields[7]))
                    .daysChangePercent(parseBigDecimal(fields[8]))
                    .intradayChange(parseBigDecimal(fields[9]))
                    .intradayChangePercent(parseBigDecimal(fields[10]))
                    .volume(parseLong(fields[11]))
                    .openInterest(parseLong(fields[12]))
                    .oiDayChangePercent(parseBigDecimal(fields[13]))
                    .oiDayHigh(parseLong(fields[14]))
                    .oiDayLow(parseLong(fields[15]))
                    .lastTradedQuantity(parseLong(fields[16]))
                    .averageTradedPrice(parseBigDecimal(fields[17]))
                    .totalBuyQuantity(parseLong(fields[18]))
                    .totalSellQuantity(parseLong(fields[19]))
                    .build();
        } catch (Exception e) {
            log.error("Error building market data object", e);
            return null;
        }
    }
    
    private String parseString(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }
    
    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? new BigDecimal(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Long parseLong(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Long.parseLong(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private void closeConnection() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            log.error("Error closing reader", e);
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing socket", e);
        }
    }
}
