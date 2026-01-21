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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TCP client for consuming market data feed in binary packet format
 * Packet format: 512 bytes fixed size with Little Endian byte order
 */
@Slf4j
@Component
public class TcpFeedClient {
    
    private static final int PACKET_SIZE = 512;
    private static final int SYMBOL_SIZE = 50;
    private static final int TIMESTAMP_SIZE = 20;
    private static final int EXPIRY_SIZE = 12;
    private static final int DEPTH_LEVELS = 5;
    private static final int RESERVED_SIZE = 78;
    
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
                MarketData data = deserialize(packet);
                if (data != null) {
                    dataConsumer.accept(data);
                }
            } catch (Exception e) {
                log.error("Error parsing market data packet", e);
            }
        }
    }
    
    /**
     * Deserialize 512-byte TCP packet to MarketData.
     * 
     * Structure (Total: 512 bytes):
     * - Symbol: 50 bytes (UTF-8, space-padded)
     * - Sequence Number: 8 bytes (long)
     * - UDP Reception Timestamp: 8 bytes (long, epoch millis)
     * - Publisher Timestamp: 8 bytes (long, epoch millis)
     * - Last Traded Price: 8 bytes (double)
     * - Volume: 8 bytes (double)
     * - Open Interest: 8 bytes (double)
     * - Open: 8 bytes (double)
     * - High: 8 bytes (double)
     * - Low: 8 bytes (double)
     * - Close: 8 bytes (double)
     * - PDC: 8 bytes (double)
     * - Upper Circuit: 8 bytes (double)
     * - Lower Circuit: 8 bytes (double)
     * - Last Traded Qty: 8 bytes (double)
     * - Last Traded Time: 20 bytes (UTF-8 timestamp, space-padded)
     * - Expiry: 12 bytes (UTF-8, space-padded)
     * - Buy Depth: 120 bytes (5 levels × 24 bytes each: price 8, qty 8, orders 8)
     * - Sell Depth: 120 bytes (5 levels × 24 bytes each: price 8, qty 8, orders 8)
     * - Reserved: 78 bytes (for future fields, filled with zeros)
     */
    private MarketData deserialize(byte[] data) {
        if (data == null || data.length != PACKET_SIZE) {
            throw new IllegalArgumentException(
                String.format("Expected %d bytes, got %d", PACKET_SIZE, data != null ? data.length : 0));
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // Read symbol (50 bytes)
        String symbol = readFixedString(buffer, SYMBOL_SIZE);
        
        // Read sequence number (8 bytes)
        long sequenceNumber = buffer.getLong();
        
        // Read timestamps (8 bytes each) for latency tracking
        long udpReceptionTimestamp = buffer.getLong();
        long publisherTimestamp = buffer.getLong();
        
        log.debug("Processing packet: symbol={}, seq={}, udpTs={}, pubTs={}",
                 symbol, sequenceNumber, udpReceptionTimestamp, publisherTimestamp);
        
        // Read price fields (8 bytes each, 11 doubles = 88 bytes)
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
        
        // Read last traded time (20 bytes)
        String lastTradedTime = readFixedString(buffer, TIMESTAMP_SIZE);
        
        // Read expiry (12 bytes)
        String expiry = readFixedString(buffer, EXPIRY_SIZE);
        
        // Read buy depth (120 bytes = 5 levels × 24 bytes)
        List<MarketData.DepthLevel> bids = new ArrayList<>();
        long totalBuyQty = 0;
        for (int i = 0; i < DEPTH_LEVELS; i++) {
            double price = buffer.getDouble();
            double quantity = buffer.getDouble();
            double ordersDouble = buffer.getDouble();
            int orders = (int) ordersDouble;
            
            // Non-empty level (price > 0 or qty > 0)
            if (price > 0 || quantity > 0) {
                int qty = (int) quantity;
                bids.add(new MarketData.DepthLevel(price, qty, orders));
                totalBuyQty += qty;
            }
        }
        
        // Read sell depth (120 bytes = 5 levels × 24 bytes)
        List<MarketData.DepthLevel> asks = new ArrayList<>();
        long totalSellQty = 0;
        for (int i = 0; i < DEPTH_LEVELS; i++) {
            double price = buffer.getDouble();
            double quantity = buffer.getDouble();
            double ordersDouble = buffer.getDouble();
            int orders = (int) ordersDouble;
            
            // Non-empty level (price > 0 or qty > 0)
            if (price > 0 || quantity > 0) {
                int qty = (int) quantity;
                asks.add(new MarketData.DepthLevel(price, qty, orders));
                totalSellQty += qty;
            }
        }
        
        // Skip reserved bytes (78 bytes)
        buffer.position(buffer.position() + RESERVED_SIZE);
        
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
                .lastTradedTime(lastTradedTime.isEmpty() ? null : lastTradedTime)
                .expiry(expiry.isEmpty() ? null : expiry)
                .daysChange(daysChange)
                .daysChangePercent(daysChangePercent)
                .totalBuyQuantity(totalBuyQty)
                .totalSellQuantity(totalSellQty)
                .bids(bids)
                .asks(asks)
                .build();
    }
    
    // Helper methods
    
    private String readFixedString(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
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
