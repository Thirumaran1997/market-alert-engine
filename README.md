# Market Alert Engine

A Spring Boot application that consumes market data from TCP feeds and triggers alerts based on configurable criteria.

## Features

- **TCP Feed Consumer**: Connects to TCP feeds and consumes fixed-format market data
- **Multi-Criteria Evaluation**: Supports evaluation against multiple market criteria:
  - LTP (Last Traded Price)
  - 52-week High and Low
  - Open Price and Close Price
  - Day's Change and Day's Change %
  - Intraday Change and Intraday Change %
  - Volume
  - Open Interest (OI) and OI Day Change %
  - OI Day High and Low
  - Last Traded Quantity
  - Average Traded Price
  - Total Buy and Sell Quantities
- **Async Alert API Invocation**: Triggers HTTP API calls asynchronously when criteria are met
- **Auto-Reconnect**: Automatically reconnects to TCP feed on connection loss

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- A running TCP feed server (e.g., from combined-mkt-depth project)

## Configuration

Configure the application by editing `src/main/resources/application.yml`:

### TCP Feed Configuration

```yaml
market:
  feed:
    tcp:
      host: localhost          # TCP feed server host
      port: 5000              # TCP feed server port
      reconnect-delay-seconds: 5
      buffer-size: 8192
      auto-reconnect: true
```

### Alert Criteria Configuration

Add alert criteria in the configuration file:

```yaml
market:
  alert:
    default-alert-api-url: http://localhost:8081/api/alerts
    criteria:
      - criteria-id: "high-volume-alert"
        symbol: "AAPL"
        volume-min-threshold: 1000000
        alert-api-url: "http://localhost:8081/api/alerts/volume"
      
      - criteria-id: "price-spike-alert"
        symbol: "GOOGL"
        days-change-percent-min-threshold: 5.0
        alert-api-url: "http://localhost:8081/api/alerts/price-spike"
```

## TCP Feed Data Format

The application expects TCP feed data in **binary packet format** (402 bytes per packet):

### Packet Structure (Little Endian)

| Offset | Size | Field | Type | Description |
|--------|------|-------|------|-------------|
| 0 | 50 | Symbol | UTF-8 String | Space-padded symbol name |
| 50 | 8 | Sequence Number | Long | Packet sequence number |
| 58 | 8 | UDP Reception Timestamp | Long | UDP reception timestamp |
| 66 | 8 | Publisher Timestamp | Long | Publisher timestamp |
| 74 | 8 | LTP | Double | Last Traded Price |
| 82 | 8 | Volume | Double | Trading volume |
| 90 | 8 | OI | Double | Open Interest |
| 98 | 8 | Open | Double | Opening price |
| 106 | 8 | High | Double | Day's high |
| 114 | 8 | Low | Double | Day's low |
| 122 | 8 | Close | Double | Closing price |
| 130 | 8 | PDC | Double | Previous day close |
| 138 | 8 | Upper Circuit | Double | Upper circuit limit |
| 146 | 8 | Lower Circuit | Double | Lower circuit limit |
| 154 | 8 | Last Traded Qty | Double | Last traded quantity |
| 162 | 20 | Last Traded Time | - | (Skipped) |
| 182 | 12 | Expiry | - | (Skipped) |
| 194 | 120 | Buy Depth | 5 Levels | 5 bid levels (24 bytes each) |
| 314 | 120 | Sell Depth | 5 Levels | 5 ask levels (24 bytes each) |

### Depth Level Structure (24 bytes)
- **Price** (8 bytes, Double): Price level
- **Quantity** (8 bytes, Double): Quantity at this level
- **Orders** (8 bytes, Double): Number of orders

Note: Zero/invalid prices in depth levels are filtered out as they are just fillers.

### Example Usage
The TCP client automatically reads 402-byte packets, parses them using ByteBuffer with Little Endian byte order, and converts them into MarketData objects for criteria evaluation.

## Building the Application

```bash
mvn clean package
```

## Running the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/market-alert-engine-1.0.0-SNAPSHOT.jar
```

## Alert Criteria Fields

All threshold fields are optional. If not specified, the criteria won't evaluate that field.

- **criteriaId**: Unique identifier for the criteria
- **symbol**: Stock symbol to match (optional, if not specified matches all symbols)
- **ltpMinThreshold/ltpMaxThreshold**: LTP range
- **weekHigh52Threshold**: 52-week high threshold
- **weekLow52Threshold**: 52-week low threshold
- **openPriceMinThreshold/openPriceMaxThreshold**: Open price range
- **closePriceMinThreshold/closePriceMaxThreshold**: Close price range
- **daysChangeMinThreshold/daysChangeMaxThreshold**: Day's change range
- **daysChangePercentMinThreshold/daysChangePercentMaxThreshold**: Day's change % range
- **intradayChangeMinThreshold/intradayChangeMaxThreshold**: Intraday change range
- **intradayChangePercentMinThreshold/intradayChangePercentMaxThreshold**: Intraday change % range
- **volumeMinThreshold/volumeMaxThreshold**: Volume range
- **openInterestMinThreshold/openInterestMaxThreshold**: OI range
- **oiDayChangePercentMinThreshold/oiDayChangePercentMaxThreshold**: OI day change % range
- **lastTradedQuantityMinThreshold/lastTradedQuantityMaxThreshold**: Last traded quantity range
- **averageTradedPriceMinThreshold/averageTradedPriceMaxThreshold**: Average traded price range
- **totalBuyQuantityMinThreshold/totalBuyQuantityMaxThreshold**: Total buy quantity range
- **totalSellQuantityMinThreshold/totalSellQuantityMaxThreshold**: Total sell quantity range
- **oiDayHighThreshold**: OI day high threshold
- **oiDayLowThreshold**: OI day low threshold
- **alertApiUrl**: API URL to invoke when criteria is met

## Architecture

### Components

1. **TcpFeedClient**: Manages TCP connection and data consumption
2. **MarketData**: Domain model for market data
3. **AlertCriteria**: Configuration model for alert criteria
4. **CriteriaEvaluator**: Evaluates market data against criteria
5. **AlertApiService**: Invokes alert APIs asynchronously
6. **MarketAlertService**: Orchestrates the entire alert process

### Flow

1. Application starts and connects to TCP feed
2. Market data is continuously consumed from TCP feed
3. Each data point is evaluated against all configured criteria
4. When criteria is met, an alert event is created
5. Alert API is invoked asynchronously with the alert event

## Testing

Run tests with:

```bash
mvn test
```

## Logging

Configure logging levels in `application.yml`:

```yaml
logging:
  level:
    com.market.alert: DEBUG
    root: INFO
```

## License

MIT License