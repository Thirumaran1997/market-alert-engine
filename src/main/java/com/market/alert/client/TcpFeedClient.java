package com.market.alert.client;

import com.market.alert.config.TcpFeedProperties;
import com.market.alert.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TCP client for consuming market data feed in binary packet format
 */
@Slf4j
@Component
public class TcpFeedClient {
    
    private static final int PACKET_SIZE = 402; // Total packet size in bytes
    
    private final TcpFeedProperties properties;
    private Socket socket;
    private DataInputStream inputStream;
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
            inputStream = new DataInputStream(socket.getInputStream());
            log.info("Connected to TCP feed");
        }
    }
    
    private void readData(Consumer<MarketData> dataConsumer) throws IOException {
        byte[] packet = new byte[PACKET_SIZE];
        
        while (running) {
            int totalRead = 0;
            while (totalRead < PACKET_SIZE) {
                int bytesRead = inputStream.read(packet, totalRead, PACKET_SIZE - totalRead);
                if (bytesRead == -1) {
                    throw new IOException("End of stream reached");
                }
                totalRead += bytesRead;
            }
            
            try {
                MarketData data = parsePacket(packet);
                if (data != null) {
                    dataConsumer.accept(data);
                }
            } catch (Exception e) {
                log.error("Error parsing market data packet", e);
            }
        }
    }
    
    /**
     * Parse binary packet format
     */
    private MarketData parsePacket(byte[] packet) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(packet);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // Read symbol (50 bytes, UTF-8, space-padded)
            byte[] symbolBytes = new byte[50];
            buffer.get(symbolBytes);
            String symbol = new String(symbolBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            
            // Read sequence number (8 bytes)
            long sequenceNumber = buffer.getLong();
            
            // Read timestamps (8 bytes each)
            long udpReceptionTimestamp = buffer.getLong();
            long publisherTimestamp = buffer.getLong();
            
            log.debug("Processing packet: symbol={}, seq={}, udpTs={}, pubTs={}",
                     symbol, sequenceNumber, udpReceptionTimestamp, publisherTimestamp);
            
            // Read price fields (88 bytes = 11 doubles)
            double ltp = buffer.getDouble();
            double volume = buffer.getDouble();
            double oi = buffer.getDouble();
            double open = buffer.getDouble();
            double high = buffer.getDouble();
            double low = buffer.getDouble();
            double close = buffer.getDouble();
            double pdc = buffer.getDouble();
            double upperCircuit = buffer.getDouble();
            double lowerCircuit = buffer.getDouble();
            double lastTradedQty = buffer.getDouble();
            
            // Skip last traded time (20 bytes)
            buffer.position(buffer.position() + 20);
            
            // Skip expiry (12 bytes)
            buffer.position(buffer.position() + 12);
            
            // Read buy depth (5 levels, 24 bytes each: price 8, qty 8, orders 8)
            List<MarketData.DepthLevel> bids = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double price = buffer.getDouble();
                double quantity = buffer.getDouble();
                double ordersDouble = buffer.getDouble();
                int orders = (int) ordersDouble;
                
                // Filter out zero/invalid prices (these are just fillers)
                if (price > 0) {
                    bids.add(new MarketData.DepthLevel(price, (int) quantity, orders));
                }
            }
            
            // Read sell depth (5 levels, 24 bytes each: price 8, qty 8, orders 8)
            List<MarketData.DepthLevel> asks = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double price = buffer.getDouble();
                double quantity = buffer.getDouble();
                double ordersDouble = buffer.getDouble();
                int orders = (int) ordersDouble;
                
                // Filter out zero/invalid prices (these are just fillers)
                if (price > 0) {
                    asks.add(new MarketData.DepthLevel(price, (int) quantity, orders));
                }
            }
            
            // Calculate derived fields
            BigDecimal ltpBd = toBigDecimal(ltp);
            BigDecimal pdcBd = toBigDecimal(pdc);
            BigDecimal daysChange = null;
            BigDecimal daysChangePercent = null;
            
            if (ltpBd != null && pdcBd != null && pdcBd.compareTo(BigDecimal.ZERO) != 0) {
                daysChange = ltpBd.subtract(pdcBd);
                daysChangePercent = daysChange.divide(pdcBd, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            // Calculate total buy and sell quantities from depth
            long totalBuyQty = bids.stream().mapToLong(MarketData.DepthLevel::getQuantity).sum();
            long totalSellQty = asks.stream().mapToLong(MarketData.DepthLevel::getQuantity).sum();
            
            return MarketData.builder()
                    .timestamp(LocalDateTime.now())
                    .symbol(symbol)
                    .sequenceNumber(sequenceNumber)
                    .udpReceptionTimestamp(udpReceptionTimestamp)
                    .publisherTimestamp(publisherTimestamp)
                    .ltp(ltpBd)
                    .volume(toLong(volume))
                    .openInterest(toLong(oi))
                    .openPrice(toBigDecimal(open))
                    .highPrice(toBigDecimal(high))
                    .lowPrice(toBigDecimal(low))
                    .closePrice(toBigDecimal(close))
                    .previousDayClose(pdcBd)
                    .upperCircuit(toBigDecimal(upperCircuit))
                    .lowerCircuit(toBigDecimal(lowerCircuit))
                    .lastTradedQuantity(toLong(lastTradedQty))
                    .daysChange(daysChange)
                    .daysChangePercent(daysChangePercent)
                    .totalBuyQuantity(totalBuyQty)
                    .totalSellQuantity(totalSellQty)
                    .bids(bids)
                    .asks(asks)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing packet", e);
            return null;
        }
    }
    
    private BigDecimal toBigDecimal(double value) {
        if (value == 0.0 || Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }
    
    private Long toLong(double value) {
        if (value == 0.0 || Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return (long) value;
    }
    
    private void closeConnection() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            log.error("Error closing input stream", e);
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
