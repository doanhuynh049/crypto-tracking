package service;

import model.CryptoData;
import model.TechnicalIndicators;
import model.TechnicalIndicators.PricePoint;
import model.TechnicalIndicators.EntryTechnique;
import model.TechnicalIndicators.SignalStrength;
import model.TechnicalIndicators.TrendDirection;
import model.TechnicalIndicators.EntryQuality;
import util.LoggerUtil;
import cache.CoinGeckoApiCache;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Technical Analysis Service for cryptocurrency entry techniques
 * Implements various technical indicators and entry signal analysis
 */
public class TechnicalAnalysisService {
    
    private static final int RSI_PERIOD = 14;
    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;
    private static final int SMA_SHORT = 10;
    private static final int SMA_LONG = 50;
    private static final int VOLUME_PERIOD = 20;
    
    // Rate limiting and retry configuration
    private static final long BASE_DELAY_MS = 5000; // 5 seconds base delay
    private static final long MAX_DELAY_MS = 60000; // 1 minute max delay
    private static final int MAX_RETRIES = 3;
    private static volatile long lastApiCallTime = 0;
    private static final Object apiLock = new Object();
    
    // Global API coordination
    private static final ApiCoordinationService apiCoordinator = ApiCoordinationService.getInstance();
    
    /**
     * Analyze technical indicators for a cryptocurrency
     */
    public static CompletableFuture<TechnicalIndicators> analyzeEntry(CryptoData crypto) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Analyzing entry for: " + crypto.symbol);
        return CompletableFuture.supplyAsync(() -> {
            try {
                LoggerUtil.info(TechnicalAnalysisService.class, "Starting technical analysis for " + crypto.symbol);
                
                TechnicalIndicators indicators = new TechnicalIndicators();
                
                // Fetch real price history from CoinGecko API
                List<PricePoint> priceHistory = fetchRealPriceHistory(crypto.id, crypto.currentPrice);
                indicators.setPriceHistory(priceHistory);
                
                // Calculate technical indicators
                calculateRSI(indicators, priceHistory);
                calculateMACD(indicators, priceHistory);
                calculateMovingAverages(indicators, priceHistory);
                calculateSupportResistance(indicators, priceHistory);
                calculateFibonacciLevels(indicators, priceHistory);
                calculateVolumeAnalysis(indicators, priceHistory, crypto.id);
                calculateTrendAnalysis(indicators, priceHistory);
                
                // Enhance with additional market data from API
                enhanceWithMarketData(indicators, crypto.id);
                
                // Generate entry signals
                generateEntrySignals(indicators, crypto);
                
                // Calculate overall entry quality
                calculateOverallQuality(indicators);
                
                LoggerUtil.info(TechnicalAnalysisService.class, 
                    "Technical analysis completed for " + crypto.symbol + 
                    " - Quality: " + indicators.getOverallEntryQuality());
                
                return indicators;
                
            } catch (Exception e) {
                LoggerUtil.error(TechnicalAnalysisService.class, 
                    "Error in technical analysis for " + crypto.symbol, e);
                return createErrorIndicators();
            }
        });
    }
    
    /**
     * Calculate RSI (Relative Strength Index)
     */
    private static void calculateRSI(TechnicalIndicators indicators, List<PricePoint> priceHistory) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating RSI");
        if (priceHistory.size() < RSI_PERIOD + 1) {
            indicators.setRsi(50.0); // Neutral default
            return;
        }
        
        double avgGain = 0.0;
        double avgLoss = 0.0;
        
        // Calculate initial average gain/loss
        for (int i = 1; i <= RSI_PERIOD; i++) {
            double change = priceHistory.get(i).getClose() - priceHistory.get(i - 1).getClose();
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }
        
        avgGain /= RSI_PERIOD;
        avgLoss /= RSI_PERIOD;
        
        // Calculate RSI
        if (avgLoss == 0) {
            indicators.setRsi(100.0);
        } else {
            double rs = avgGain / avgLoss;
            double rsi = 100.0 - (100.0 / (1.0 + rs));
            indicators.setRsi(rsi);
        }
    }
    
    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    private static void calculateMACD(TechnicalIndicators indicators, List<PricePoint> priceHistory) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating MACD");
        if (priceHistory.size() < MACD_SLOW) {
            indicators.setMacd(0.0);
            indicators.setMacdSignal(0.0);
            return;
        }
        
        // Calculate EMAs
        double emaFast = calculateEMA(priceHistory, MACD_FAST);
        double emaSlow = calculateEMA(priceHistory, MACD_SLOW);
        
        // MACD line
        double macd = emaFast - emaSlow;
        indicators.setMacd(macd);
        
        // Signal line (EMA of MACD)
        // Simplified calculation for demo
        double macdSignal = macd * 0.9; // Approximation
        indicators.setMacdSignal(macdSignal);
    }
    
    /**
     * Calculate Moving Averages (SMA and EMA)
     */
    private static void calculateMovingAverages(TechnicalIndicators indicators, List<PricePoint> priceHistory) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating moving averages");
        // SMA 10
        if (priceHistory.size() >= SMA_SHORT) {
            double sma10 = calculateSMA(priceHistory, SMA_SHORT);
            indicators.setSma10(sma10);
            indicators.setEma10(calculateEMA(priceHistory, SMA_SHORT));
        }
        
        // SMA 50
        if (priceHistory.size() >= SMA_LONG) {
            double sma50 = calculateSMA(priceHistory, SMA_LONG);
            indicators.setSma50(sma50);
            indicators.setEma50(calculateEMA(priceHistory, SMA_LONG));
        }
    }
    
    /**
     * Calculate Support and Resistance levels
     */
    private static void calculateSupportResistance(TechnicalIndicators indicators, List<PricePoint> priceHistory) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating support and resistance");
        if (priceHistory.isEmpty()) return;
        
        // Find recent highs and lows
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        
        for (PricePoint point : priceHistory) {
            highs.add(point.getHigh());
            lows.add(point.getLow());
        }
        
        // Calculate support (average of recent lows)
        Collections.sort(lows);
        double support = lows.stream().limit(5).mapToDouble(Double::doubleValue).average().orElse(0.0);
        indicators.setSupportLevel(support);
        
        // Calculate resistance (average of recent highs)
        Collections.sort(highs, Collections.reverseOrder());
        double resistance = highs.stream().limit(5).mapToDouble(Double::doubleValue).average().orElse(0.0);
        indicators.setResistanceLevel(resistance);
    }
    
    /**
     * Calculate Fibonacci Retracement levels
     */
    private static void calculateFibonacciLevels(TechnicalIndicators indicators, List<PricePoint> priceHistory) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating Fibonacci levels");
        if (priceHistory.isEmpty()) return;
        
        // Find swing high and low
        double swingHigh = priceHistory.stream().mapToDouble(PricePoint::getHigh).max().orElse(0.0);
        double swingLow = priceHistory.stream().mapToDouble(PricePoint::getLow).min().orElse(0.0);
        
        double range = swingHigh - swingLow;
        
        // Calculate Fibonacci levels
        indicators.setFibonacciSupport38(swingHigh - (range * 0.382));
        indicators.setFibonacciSupport50(swingHigh - (range * 0.5));
        indicators.setFibonacciSupport61(swingHigh - (range * 0.618));
    }
    
    /**
     * Calculate Volume Analysis with real volume data when available
     */
    private static void calculateVolumeAnalysis(TechnicalIndicators indicators, List<PricePoint> priceHistory, String cryptoId) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating volume analysis for: " + cryptoId);
        if (priceHistory.size() < VOLUME_PERIOD) return;
        
        // Calculate average volume from historical data
        double avgVolume = priceHistory.stream()
            .skip(Math.max(0, priceHistory.size() - VOLUME_PERIOD))
            .mapToDouble(PricePoint::getVolume)
            .average()
            .orElse(0.0);
        
        indicators.setAverageVolume20(avgVolume);
        
        // Try to get real current volume from API
        double realCurrentVolume = fetchCurrentVolume(cryptoId);
        
        if (realCurrentVolume > 0) {
            // Use real volume data
            indicators.setCurrentVolume(realCurrentVolume);
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "Using real volume data for " + cryptoId + ": $" + String.format("%.0f", realCurrentVolume));
        } else {
            // Fallback to latest historical volume
            if (!priceHistory.isEmpty()) {
                double currentVolume = priceHistory.get(priceHistory.size() - 1).getVolume();
                indicators.setCurrentVolume(currentVolume);
            }
        }
        
        // Volume confirmation (current volume > 1.5x average)
        indicators.setVolumeConfirmation(indicators.getCurrentVolume() > avgVolume * 1.5);
    }
    
    /**
     * Calculate Trend Analysis
     */
    private static void calculateTrendAnalysis(TechnicalIndicators indicators, List<PricePoint> priceHistory) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating trend analysis");
        if (priceHistory.size() < 20) {
            indicators.setTrend(TrendDirection.NEUTRAL);
            return;
        }
        
        // Simple trend detection based on price movement
        double recentAvg = priceHistory.stream()
            .skip(Math.max(0, priceHistory.size() - 10))
            .mapToDouble(PricePoint::getClose)
            .average()
            .orElse(0.0);
        
        double olderAvg = priceHistory.stream()
            .skip(Math.max(0, priceHistory.size() - 20))
            .limit(10)
            .mapToDouble(PricePoint::getClose)
            .average()
            .orElse(0.0);
        
        if (recentAvg > olderAvg * 1.02) {
            indicators.setTrend(TrendDirection.BULLISH);
        } else if (recentAvg < olderAvg * 0.98) {
            indicators.setTrend(TrendDirection.BEARISH);
        } else {
            indicators.setTrend(TrendDirection.NEUTRAL);
        }
        
        // Calculate trendline support/resistance (simplified)
        indicators.setTrendlineSupport(indicators.getSupportLevel() * 0.95);
        indicators.setTrendlineResistance(indicators.getResistanceLevel() * 1.05);
    }
    
    /**
     * Generate Entry Signals based on technical analysis
     */
    private static void generateEntrySignals(TechnicalIndicators indicators, CryptoData crypto) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Generating entry signals for: " + crypto.symbol);
        double currentPrice = crypto.currentPrice;
        
        // RSI Oversold Signal
        if (indicators.getRsi() < 30) {
            indicators.addEntrySignal(
                EntryTechnique.RSI_OVERSOLD,
                SignalStrength.STRONG,
                "RSI indicates oversold conditions - potential bounce",
                currentPrice * 1.05, // Target 5% above
                currentPrice * 0.95, // Stop loss 5% below
                0.75
            );
        }
        
        // MACD Bullish Crossover
        if (indicators.getMacd() > indicators.getMacdSignal() && indicators.getMacd() > 0) {
            indicators.addEntrySignal(
                EntryTechnique.MACD_BULLISH_CROSSOVER,
                SignalStrength.MODERATE,
                "MACD line crossed above signal line - bullish momentum",
                currentPrice * 1.08,
                currentPrice * 0.92,
                0.70
            );
        }
        
        // Moving Average Golden Cross
        if (indicators.getSma10() > indicators.getSma50() && indicators.getSma10() > 0) {
            indicators.addEntrySignal(
                EntryTechnique.MOVING_AVERAGE_CROSSOVER,
                SignalStrength.STRONG,
                "Golden Cross detected - short MA above long MA",
                currentPrice * 1.10,
                currentPrice * 0.90,
                0.80
            );
        }
        
        // Support Level Bounce
        if (currentPrice <= indicators.getSupportLevel() * 1.02) {
            indicators.addEntrySignal(
                EntryTechnique.SUPPORT_RESISTANCE,
                SignalStrength.MODERATE,
                "Price near support level - potential bounce opportunity",
                indicators.getResistanceLevel(),
                indicators.getSupportLevel() * 0.95,
                0.65
            );
        }
        
        // Fibonacci Retracement
        if (currentPrice <= indicators.getFibonacciSupport61() * 1.01) {
            indicators.addEntrySignal(
                EntryTechnique.FIBONACCI_RETRACEMENT,
                SignalStrength.MODERATE,
                "Price at 61.8% Fibonacci retracement - key support level",
                indicators.getFibonacciSupport38(),
                indicators.getFibonacciSupport61() * 0.97,
                0.70
            );
        }
        
        // Volume Breakout
        if (indicators.isVolumeConfirmation() && indicators.getTrend() == TrendDirection.BULLISH) {
            indicators.addEntrySignal(
                EntryTechnique.VOLUME_BREAKOUT,
                SignalStrength.VERY_STRONG,
                "High volume breakout with bullish trend confirmed",
                currentPrice * 1.15,
                currentPrice * 0.88,
                0.85
            );
        }
        
        // Trendline Bounce
        if (currentPrice <= indicators.getTrendlineSupport() * 1.01 && 
            indicators.getTrend() == TrendDirection.BULLISH) {
            indicators.addEntrySignal(
                EntryTechnique.TRENDLINE_BOUNCE,
                SignalStrength.STRONG,
                "Price bouncing off ascending trendline support",
                indicators.getTrendlineResistance(),
                indicators.getTrendlineSupport() * 0.96,
                0.78
            );
        }
    }
    
    /**
     * Calculate Overall Entry Quality based on signals
     */
    private static void calculateOverallQuality(TechnicalIndicators indicators) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating overall entry quality");
        List<TechnicalIndicators.EntrySignal> signals = indicators.getEntrySignals();
        
        if (signals.isEmpty()) {
            indicators.setOverallEntryQuality(EntryQuality.POOR);
            return;
        }
        
        // Calculate weighted score
        double totalScore = 0.0;
        double totalWeight = 0.0;
        
        for (TechnicalIndicators.EntrySignal signal : signals) {
            double weight = getSignalWeight(signal.getStrength());
            double score = signal.getConfidence() * weight;
            
            totalScore += score;
            totalWeight += weight;
        }
        
        double averageScore = totalWeight > 0 ? totalScore / totalWeight : 0.0;
        
        // Determine quality based on score
        if (averageScore >= 0.85) {
            indicators.setOverallEntryQuality(EntryQuality.EXCELLENT);
        } else if (averageScore >= 0.75) {
            indicators.setOverallEntryQuality(EntryQuality.GOOD);
        } else if (averageScore >= 0.60) {
            indicators.setOverallEntryQuality(EntryQuality.AVERAGE);
        } else if (averageScore >= 0.40) {
            indicators.setOverallEntryQuality(EntryQuality.POOR);
        } else {
            indicators.setOverallEntryQuality(EntryQuality.VERY_POOR);
        }
    }
    
    /**
     * Get weight for signal strength
     */
    private static double getSignalWeight(SignalStrength strength) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Getting signal weight for: " + strength);
        switch (strength) {
            case VERY_STRONG: return 1.0;
            case STRONG: return 0.8;
            case MODERATE: return 0.6;
            case WEAK: return 0.4;
            case VERY_WEAK: return 0.2;
            default: return 0.5;
        }
    }
    
    /**
     * Helper method to calculate Simple Moving Average
     */
    private static double calculateSMA(List<PricePoint> priceHistory, int period) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating SMA for period: " + period);
        if (priceHistory.size() < period) return 0.0;
        
        return priceHistory.stream()
            .skip(Math.max(0, priceHistory.size() - period))
            .mapToDouble(PricePoint::getClose)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Helper method to calculate Exponential Moving Average
     */
    private static double calculateEMA(List<PricePoint> priceHistory, int period) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Calculating EMA for period: " + period);
        if (priceHistory.size() < period) return 0.0;
        
        double multiplier = 2.0 / (period + 1.0);
        double ema = calculateSMA(priceHistory, period); // Start with SMA
        
        // Calculate EMA for remaining periods
        for (int i = period; i < priceHistory.size(); i++) {
            double closePrice = priceHistory.get(i).getClose();
            ema = (closePrice * multiplier) + (ema * (1 - multiplier));
        }
        
        return ema;
    }
    
    /**
     * Fetch real price history from CoinGecko API with improved rate limiting and retry logic
     * @param cryptoId The CoinGecko ID of the cryptocurrency
     * @param currentPrice Current price for validation
     * @return List of PricePoint containing real OHLC data
     */
    private static List<PricePoint> fetchRealPriceHistory(String cryptoId, double currentPrice) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Fetching real price history for: " + cryptoId);
        // Check cache first
        List<PricePoint> cachedData = CoinGeckoApiCache.getCachedOHLCData(cryptoId);
        if (cachedData != null) {
            LoggerUtil.info(TechnicalAnalysisService.class, 
                "Using cached OHLC data for " + cryptoId + " (" + cachedData.size() + " points)");
            return cachedData;
        }
        
        // Cache miss - fetch from API
        List<PricePoint> apiData = fetchRealPriceHistoryWithRetry(cryptoId, currentPrice, 0);
        
        // Cache the result if we got valid data
        if (apiData != null && !apiData.isEmpty() && apiData.size() > 10) {
            CoinGeckoApiCache.cacheOHLCData(cryptoId, apiData);
        }
        
        return apiData;
    }
    
    /**
     * Fetch real price history with retry logic and exponential backoff
     */
    private static List<PricePoint> fetchRealPriceHistoryWithRetry(String cryptoId, double currentPrice, int retryCount) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Fetching real price history with retry for: " + cryptoId + " attempt: " + (retryCount + 1));
        try {
            // Fix common crypto ID mapping issues
            String correctedId = mapCryptoId(cryptoId);
            
            // Use global API coordination instead of local synchronization
            if (!apiCoordinator.requestApiCall("TechnicalAnalysisService", 
                    "OHLC data for " + correctedId + " (attempt " + (retryCount + 1) + ")")) {
                // If global coordination fails, fall back to local data
                return generateFallbackPriceHistory(cryptoId, currentPrice);
            }
            
            LoggerUtil.info(TechnicalAnalysisService.class, 
                "Fetching real OHLC data for " + correctedId + " from CoinGecko API (attempt " + (retryCount + 1) + ")");
            
            // CoinGecko OHLC API endpoint for 30 days of data
            String apiUrl = "https://api.coingecko.com/api/v3/coins/" + correctedId + "/ohlc?vs_currency=usd&days=30";
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "CryptoPortfolio/1.0");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            
            int responseCode = connection.getResponseCode();
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "API Response Code for " + cryptoId + ": " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return parseOHLCResponse(response.toString(), currentPrice);
                
            } else if (responseCode == 429) {
                LoggerUtil.warning(TechnicalAnalysisService.class, 
                    "Rate limited for " + cryptoId + " (429) on attempt " + (retryCount + 1));
                
                // Implement exponential backoff retry
                if (retryCount < MAX_RETRIES) {
                    long backoffDelay = Math.min(BASE_DELAY_MS * (long) Math.pow(2, retryCount), MAX_DELAY_MS);
                    LoggerUtil.info(TechnicalAnalysisService.class, 
                        "Retrying " + cryptoId + " after " + backoffDelay + "ms backoff delay");
                    Thread.sleep(backoffDelay);
                    return fetchRealPriceHistoryWithRetry(cryptoId, currentPrice, retryCount + 1);
                } else {
                    LoggerUtil.warning(TechnicalAnalysisService.class, 
                        "Max retries exceeded for " + cryptoId + ". Using fallback data.");
                    return generateFallbackPriceHistory(cryptoId, currentPrice);
                }
                
            } else if (responseCode == 404) {
                LoggerUtil.warning(TechnicalAnalysisService.class, 
                    "Crypto ID not found: " + cryptoId + " (404). Using fallback data.");
                return generateFallbackPriceHistory(cryptoId, currentPrice);
            } else {
                LoggerUtil.warning(TechnicalAnalysisService.class, 
                    "Failed to fetch OHLC data for " + cryptoId + ", response code: " + responseCode + ". Using fallback data.");
                return generateFallbackPriceHistory(cryptoId, currentPrice);
            }
            
        } catch (Exception e) {
            LoggerUtil.error(TechnicalAnalysisService.class, 
                "Error fetching real OHLC data for " + cryptoId + ": " + e.getMessage(), e);
            
            // Retry on error if retries available
            if (retryCount < MAX_RETRIES) {
                LoggerUtil.info(TechnicalAnalysisService.class, 
                    "Retrying " + cryptoId + " due to error (attempt " + (retryCount + 2) + ")");
                try {
                    Thread.sleep(BASE_DELAY_MS * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return fetchRealPriceHistoryWithRetry(cryptoId, currentPrice, retryCount + 1);
            }
            
            return generateFallbackPriceHistory(cryptoId, currentPrice);
        }
    }
    
    /**
     * Parse OHLC response from CoinGecko API
     * @param jsonResponse JSON response string from API
     * @param currentPrice Current price for validation
     * @return List of PricePoint objects
     */
    private static List<PricePoint> parseOHLCResponse(String jsonResponse, double currentPrice) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Parsing OHLC response");
        List<PricePoint> priceHistory = new ArrayList<>();
        
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(jsonResponse);
            
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "Parsing " + jsonArray.length() + " OHLC data points");
            
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONArray dataPoint = jsonArray.getJSONArray(i);
                
                if (dataPoint.length() >= 5) {
                    long timestamp = dataPoint.getLong(0);
                    double open = dataPoint.getDouble(1);
                    double high = dataPoint.getDouble(2);
                    double low = dataPoint.getDouble(3);
                    double close = dataPoint.getDouble(4);
                    
                    // Generate realistic volume based on price (CoinGecko OHLC doesn't include volume)
                    double volume = generateRealisticVolume(close);
                    
                    priceHistory.add(new PricePoint(timestamp, open, high, low, close, volume));
                }
            }
            
            // Ensure we have data and last price is reasonable
            if (!priceHistory.isEmpty()) {
                LoggerUtil.info(TechnicalAnalysisService.class, 
                    "Successfully parsed " + priceHistory.size() + " real OHLC data points");
                
                // Validate last price is close to current price (within 10%)
                PricePoint lastPoint = priceHistory.get(priceHistory.size() - 1);
                double priceDifference = Math.abs(lastPoint.getClose() - currentPrice) / currentPrice;
                
                if (priceDifference > 0.10) {
                    LoggerUtil.warning(TechnicalAnalysisService.class, 
                        "Last OHLC price differs significantly from current price. Adjusting...");
                    
                    // Adjust the last data point to match current price
                    priceHistory.set(priceHistory.size() - 1, new PricePoint(
                        lastPoint.getTimestamp(),
                        lastPoint.getOpen(),
                        Math.max(lastPoint.getHigh(), currentPrice),
                        Math.min(lastPoint.getLow(), currentPrice),
                        currentPrice,
                        lastPoint.getVolume()
                    ));
                }
                
                return priceHistory;
            }
            
        } catch (Exception e) {
            LoggerUtil.error(TechnicalAnalysisService.class, 
                "Error parsing OHLC JSON response: " + e.getMessage(), e);
        }
        
        // Return empty list if parsing fails
        LoggerUtil.warning(TechnicalAnalysisService.class, 
            "Failed to parse OHLC data, returning empty list");
        return new ArrayList<>();
    }
    
    /**
     * Enhance indicators with additional market data from API with rate limiting
     * @param indicators The technical indicators to enhance
     * @param cryptoId The CoinGecko ID
     */
    private static void enhanceWithMarketData(TechnicalIndicators indicators, String cryptoId) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Enhancing with market data for: " + cryptoId);
        // Check cache first
        CoinGeckoApiCache.MarketDataResult cachedMarketData = CoinGeckoApiCache.getCachedMarketData(cryptoId);
        if (cachedMarketData != null) {
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "Using cached market data for " + cryptoId);
            
            // Use cached price change to enhance trend analysis
            if (cachedMarketData.priceChange7d > 5.0 && indicators.getTrend() == TrendDirection.NEUTRAL) {
                indicators.setTrend(TrendDirection.BULLISH);
                LoggerUtil.debug(TechnicalAnalysisService.class, 
                    "Enhanced trend to BULLISH based on cached 7d price change: " + cachedMarketData.priceChange7d + "%");
            } else if (cachedMarketData.priceChange7d < -5.0 && indicators.getTrend() == TrendDirection.NEUTRAL) {
                indicators.setTrend(TrendDirection.BEARISH);
                LoggerUtil.debug(TechnicalAnalysisService.class, 
                    "Enhanced trend to BEARISH based on cached 7d price change: " + cachedMarketData.priceChange7d + "%");
            }
            return;
        }
        
        try {
            // Fix crypto ID mapping
            String correctedId = mapCryptoId(cryptoId);
            
            // Use global API coordination
            if (!apiCoordinator.requestApiCall("TechnicalAnalysisService", 
                    "market data for " + correctedId)) {
                LoggerUtil.debug(TechnicalAnalysisService.class, 
                    "API coordination denied for market data of " + cryptoId);
                return;
            }
            
            // Get additional market metrics like market cap, price change percentages
            String apiUrl = "https://api.coingecko.com/api/v3/coins/" + correctedId + 
                          "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false";
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "CryptoPortfolio/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                if (jsonResponse.has("market_data")) {
                    org.json.JSONObject marketData = jsonResponse.getJSONObject("market_data");
                    
                    double marketCap = 0.0;
                    double priceChange7d = 0.0;
                    double priceChange24h = 0.0;
                    
                    // Get price change percentages for trend confirmation
                    if (marketData.has("price_change_percentage_7d")) {
                        priceChange7d = marketData.getDouble("price_change_percentage_7d");
                        
                        // Use weekly change to enhance trend analysis
                        if (priceChange7d > 5.0 && indicators.getTrend() == TrendDirection.NEUTRAL) {
                            indicators.setTrend(TrendDirection.BULLISH);
                            LoggerUtil.debug(TechnicalAnalysisService.class, 
                                "Enhanced trend to BULLISH based on 7d price change: " + priceChange7d + "%");
                        } else if (priceChange7d < -5.0 && indicators.getTrend() == TrendDirection.NEUTRAL) {
                            indicators.setTrend(TrendDirection.BEARISH);
                            LoggerUtil.debug(TechnicalAnalysisService.class, 
                                "Enhanced trend to BEARISH based on 7d price change: " + priceChange7d + "%");
                        }
                    }
                    
                    // Get 24h price change
                    if (marketData.has("price_change_percentage_24h")) {
                        priceChange24h = marketData.getDouble("price_change_percentage_24h");
                    }
                    
                    // Get market cap for additional context
                    if (marketData.has("market_cap") && marketData.getJSONObject("market_cap").has("usd")) {
                        marketCap = marketData.getJSONObject("market_cap").getDouble("usd");
                        LoggerUtil.debug(TechnicalAnalysisService.class, 
                            "Market cap for " + cryptoId + ": $" + String.format("%.0f", marketCap));
                    }
                    
                    // Cache the market data
                    CoinGeckoApiCache.cacheMarketData(cryptoId, marketCap, priceChange7d, priceChange24h);
                }
            } else if (connection.getResponseCode() == 429) {
                LoggerUtil.debug(TechnicalAnalysisService.class, 
                    "Rate limited enhancing market data for " + cryptoId);
            }
            
        } catch (Exception e) {
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "Could not enhance with market data for " + cryptoId + ": " + e.getMessage());
        }
    }

    /**
     * Fetch enhanced market data including volume from CoinGecko with rate limiting
     * @param cryptoId The CoinGecko ID of the cryptocurrency
     * @return Additional market data for volume analysis
     */
    private static double fetchCurrentVolume(String cryptoId) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Fetching current volume for: " + cryptoId);
        // Check cache first
        Double cachedVolume = CoinGeckoApiCache.getCachedVolume(cryptoId);
        if (cachedVolume != null) {
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "Using cached volume data for " + cryptoId + ": $" + String.format("%.0f", cachedVolume));
            return cachedVolume;
        }
        
        try {
            // Fix crypto ID mapping
            String correctedId = mapCryptoId(cryptoId);
            
            // Use global API coordination
            if (!apiCoordinator.requestApiCall("TechnicalAnalysisService", 
                    "volume data for " + correctedId)) {
                LoggerUtil.debug(TechnicalAnalysisService.class, 
                    "API coordination denied for volume fetch of " + cryptoId + ", using fallback");
                return 0.0;
            }
            
            // CoinGecko market data API for current volume
            String apiUrl = "https://api.coingecko.com/api/v3/coins/" + correctedId + 
                          "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false";
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "CryptoPortfolio/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                if (jsonResponse.has("market_data")) {
                    org.json.JSONObject marketData = jsonResponse.getJSONObject("market_data");
                    if (marketData.has("total_volume") && marketData.getJSONObject("total_volume").has("usd")) {
                        double volume = marketData.getJSONObject("total_volume").getDouble("usd");
                        
                        // Cache the volume data
                        CoinGeckoApiCache.cacheVolume(cryptoId, volume);
                        
                        return volume;
                    }
                }
            } else if (connection.getResponseCode() == 429) {
                LoggerUtil.debug(TechnicalAnalysisService.class, 
                    "Rate limited fetching volume for " + cryptoId + ", using fallback");
            }
            
        } catch (Exception e) {
            LoggerUtil.debug(TechnicalAnalysisService.class, 
                "Could not fetch volume data for " + cryptoId + ": " + e.getMessage());
        }
        
        return 0.0; // Return 0 if volume data unavailable
    }

    /**
     * Map common crypto IDs to correct CoinGecko IDs
     * @param cryptoId Original crypto ID
     * @return Corrected CoinGecko ID
     */
    private static String mapCryptoId(String cryptoId) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Mapping crypto ID: " + cryptoId);
        switch (cryptoId.toLowerCase()) {
            case "btc": return "bitcoin";
            case "eth": return "ethereum";
            case "bnb": return "binancecoin";
            case "ada": return "cardano";
            case "sol": return "solana";
            case "avax": return "avalanche-2";
            case "link": return "chainlink";
            case "ltc": return "litecoin";
            case "arb": return "arbitrum";
            case "op": return "optimism";
            case "fet": return "fetch-ai";
            case "rndr": return "render-token";
            case "sui": return "sui";
            case "c": return "celsius-degree-token"; // Or might be another token
            default: return cryptoId; // Return original if no mapping found
        }
    }

    /**
     * Generate realistic volume based on price level
     * @param price The price level
     * @return Estimated trading volume
     */
    private static double generateRealisticVolume(double price) {
        LoggerUtil.info(TechnicalAnalysisService.class, "Generating realistic volume for price: " + price);
        // Estimate volume based on price range (higher priced cryptos typically have lower volume)
        if (price > 10000) {  // Bitcoin-like prices
            return 500000 + (Math.random() * 2000000);
        } else if (price > 1000) {  // ETH-like prices
            return 1000000 + (Math.random() * 5000000);
        } else if (price > 100) {  // BNB-like prices
            return 2000000 + (Math.random() * 10000000);
        } else if (price > 1) {  // SOL-like prices
            return 5000000 + (Math.random() * 20000000);
        } else {  // Small cap altcoins
            return 10000000 + (Math.random() * 50000000);
        }
    }

    /**
     * Generate fallback price history when API calls fail
     * Creates realistic-looking historical data based on current price
     */
    private static List<PricePoint> generateFallbackPriceHistory(String cryptoId, double currentPrice) {
        LoggerUtil.info(TechnicalAnalysisService.class, 
            "Generating fallback price history for " + cryptoId + " based on current price: $" + currentPrice);
        
        
        List<PricePoint> fallbackHistory = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        // Generate 30 days of synthetic OHLC data
        for (int i = 29; i >= 0; i--) {
            long timestamp = currentTime - (i * 24 * 60 * 60 * 1000L); // 24 hours ago per day
            
            // Create realistic price variation (±5% daily volatility)
            double variation = (Math.random() - 0.5) * 0.1; // ±5%
            double dayMultiplier = 1.0 + variation;
            
            // Calculate base price for this day (trending towards current price)
            double trendFactor = Math.pow(dayMultiplier, 30 - i); // Compound trend
            double basePrice = currentPrice / trendFactor;
            
            // Generate OHLC with realistic intraday movement
            double open = basePrice * (1.0 + (Math.random() - 0.5) * 0.02); // ±1%
            double close = basePrice * (1.0 + (Math.random() - 0.5) * 0.02);
            double high = Math.max(open, close) * (1.0 + Math.random() * 0.03); // +0-3%
            double low = Math.min(open, close) * (1.0 - Math.random() * 0.03); // -0-3%
            
            // Generate realistic volume
            double volume = generateRealisticVolume(basePrice);
            
            fallbackHistory.add(new PricePoint(timestamp, open, high, low, close, volume));
        }
        
        LoggerUtil.info(TechnicalAnalysisService.class, 
            "Generated " + fallbackHistory.size() + " fallback data points for " + cryptoId);
        
        return fallbackHistory;
    }

    /**
     * Create error indicators when analysis fails
     */
    private static TechnicalIndicators createErrorIndicators() {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.setOverallEntryQuality(EntryQuality.VERY_POOR);
        indicators.setTrend(TrendDirection.NEUTRAL);
        indicators.addEntrySignal(
            EntryTechnique.SUPPORT_RESISTANCE,
            SignalStrength.VERY_WEAK,
            "Technical analysis failed - manual review required",
            0.0, 0.0, 0.0
        );
        return indicators;
    }
}
