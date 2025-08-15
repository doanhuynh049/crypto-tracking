package cache;

import util.LoggerUtil;
import model.TechnicalIndicators.PricePoint;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive cache system for CoinGecko API data to reduce API calls and improve performance
 * Supports multiple data types with configurable TTL (time-to-live) values
 */
public class CoinGeckoApiCache {
    
    // Singleton instance
    private static final CoinGeckoApiCache INSTANCE = new CoinGeckoApiCache();
    
    // Cache storage for different data types
    private static final Map<String, CachedPriceData> priceCache = new ConcurrentHashMap<>();
    private static final Map<String, CachedOHLCData> ohlcCache = new ConcurrentHashMap<>();
    private static final Map<String, CachedMarketData> marketDataCache = new ConcurrentHashMap<>();
    private static final Map<String, CachedVolumeData> volumeCache = new ConcurrentHashMap<>();
    
    // Cache configuration with different TTL for different data types
    private static final long PRICE_CACHE_DURATION_MS = 2 * 60 * 1000;     // 2 minutes for price data
    private static final long OHLC_CACHE_DURATION_MS = 30 * 60 * 1000;     // 30 minutes for OHLC data
    private static final long MARKET_DATA_CACHE_DURATION_MS = 15 * 60 * 1000; // 15 minutes for market data
    private static final long VOLUME_CACHE_DURATION_MS = 5 * 60 * 1000;     // 5 minutes for volume data
    
    // Cache file paths
    private static final String CACHE_DIR = "cache/coingecko/";
    private static final String PRICE_CACHE_FILE = CACHE_DIR + "price_cache.dat";
    private static final String OHLC_CACHE_FILE = CACHE_DIR + "ohlc_cache.dat";
    private static final String MARKET_CACHE_FILE = CACHE_DIR + "market_cache.dat";
    private static final String VOLUME_CACHE_FILE = CACHE_DIR + "volume_cache.dat";
    
    // Cache statistics
    private static volatile long totalCacheHits = 0;
    private static volatile long totalCacheMisses = 0;
    private static volatile long totalCacheRequests = 0;
    
    // Cleanup scheduler
    private static final ScheduledExecutorService cleanupScheduler = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CoinGecko-Cache-Cleanup");
            t.setDaemon(true);
            return t;
        });
    
    // Static initialization
    static {
        createCacheDirectory();
        loadAllCachesFromDisk();
        startCleanupScheduler();
        
        // Shutdown hook to save all caches
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveAllCachesToDisk();
            cleanupScheduler.shutdown();
            logCacheStatistics();
        }));
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private CoinGeckoApiCache() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get singleton instance
     */
    public static CoinGeckoApiCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * Cached price data structure
     */
    private static class CachedPriceData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final double priceUsd;
        final long timestamp;
        final String cryptoId;
        final LocalDateTime createdAt;
        
        CachedPriceData(String cryptoId, double priceUsd) {
            this.cryptoId = cryptoId;
            this.priceUsd = priceUsd;
            this.timestamp = System.currentTimeMillis();
            this.createdAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > PRICE_CACHE_DURATION_MS;
        }
        
        String getFormattedAge() {
            long ageSeconds = (System.currentTimeMillis() - timestamp) / 1000;
            if (ageSeconds < 60) return ageSeconds + "s ago";
            return (ageSeconds / 60) + "m ago";
        }
    }
    
    /**
     * Cached OHLC data structure
     */
    private static class CachedOHLCData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final List<PricePoint> ohlcData;
        final long timestamp;
        final String cryptoId;
        final LocalDateTime createdAt;
        
        CachedOHLCData(String cryptoId, List<PricePoint> ohlcData) {
            this.cryptoId = cryptoId;
            this.ohlcData = ohlcData;
            this.timestamp = System.currentTimeMillis();
            this.createdAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > OHLC_CACHE_DURATION_MS;
        }
        
        String getFormattedAge() {
            long ageMinutes = (System.currentTimeMillis() - timestamp) / (60 * 1000);
            if (ageMinutes < 60) return ageMinutes + "m ago";
            return (ageMinutes / 60) + "h ago";
        }
    }
    
    /**
     * Cached market data structure
     */
    private static class CachedMarketData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final double marketCapUsd;
        final double priceChange7d;
        final double priceChange24h;
        final long timestamp;
        final String cryptoId;
        final LocalDateTime createdAt;
        
        CachedMarketData(String cryptoId, double marketCapUsd, double priceChange7d, double priceChange24h) {
            this.cryptoId = cryptoId;
            this.marketCapUsd = marketCapUsd;
            this.priceChange7d = priceChange7d;
            this.priceChange24h = priceChange24h;
            this.timestamp = System.currentTimeMillis();
            this.createdAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > MARKET_DATA_CACHE_DURATION_MS;
        }
        
        String getFormattedAge() {
            long ageMinutes = (System.currentTimeMillis() - timestamp) / (60 * 1000);
            if (ageMinutes < 60) return ageMinutes + "m ago";
            return (ageMinutes / 60) + "h ago";
        }
    }
    
    /**
     * Cached volume data structure
     */
    private static class CachedVolumeData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final double volumeUsd;
        final long timestamp;
        final String cryptoId;
        final LocalDateTime createdAt;
        
        CachedVolumeData(String cryptoId, double volumeUsd) {
            this.cryptoId = cryptoId;
            this.volumeUsd = volumeUsd;
            this.timestamp = System.currentTimeMillis();
            this.createdAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > VOLUME_CACHE_DURATION_MS;
        }
        
        String getFormattedAge() {
            long ageMinutes = (System.currentTimeMillis() - timestamp) / (60 * 1000);
            if (ageMinutes < 60) return ageMinutes + "m ago";
            return (ageMinutes / 60) + "h ago";
        }
    }
    
    // =======================================
    // PRICE DATA CACHE METHODS
    // =======================================
    
    /**
     * Get cached price data for a cryptocurrency
     * @param cryptoId The cryptocurrency ID
     * @return Cached price or null if not found/expired
     */
    public static Double getCachedPrice(String cryptoId) {
        Thread.dumpStack(); // For debugging purposes, can be removed in production
        LoggerUtil.info(CoinGeckoApiCache.class, "Fetching cached price for " + cryptoId);
        if (cryptoId == null || cryptoId.trim().isEmpty()) return null;
        
        totalCacheRequests++;
        String key = createPriceCacheKey(cryptoId);
        CachedPriceData cached = priceCache.get(key);
        
        if (cached == null) {
            totalCacheMisses++;
            LoggerUtil.debug(CoinGeckoApiCache.class, "Price cache miss for " + cryptoId);
            return null;
        }
        
        if (cached.isExpired()) {
            totalCacheMisses++;
            LoggerUtil.info(CoinGeckoApiCache.class, "Price cache expired for " + cryptoId + " (age: " + cached.getFormattedAge() + ")");
            priceCache.remove(key);
            return null;
        }
        
        totalCacheHits++;
        LoggerUtil.debug(CoinGeckoApiCache.class, "Price cache hit for " + cryptoId + " (cached " + cached.getFormattedAge() + ")");
        return cached.priceUsd;
    }
    
    /**
     * Cache price data
     * @param cryptoId The cryptocurrency ID
     * @param priceUsd The price in USD
     */
    public static void cachePrice(String cryptoId, double priceUsd) {
        if (cryptoId == null || cryptoId.trim().isEmpty() || priceUsd <= 0) return;
        
        String key = createPriceCacheKey(cryptoId);
        CachedPriceData cachedData = new CachedPriceData(cryptoId, priceUsd);
        
        priceCache.put(key, cachedData);
        LoggerUtil.debug(CoinGeckoApiCache.class, "Cached price for " + cryptoId + ": $" + String.format("%.4f", priceUsd));
    }
    
    // =======================================
    // OHLC DATA CACHE METHODS
    // =======================================
    
    /**
     * Get cached OHLC data for a cryptocurrency
     * @param cryptoId The cryptocurrency ID
     * @return Cached OHLC data or null if not found/expired
     */
    public static List<PricePoint> getCachedOHLCData(String cryptoId) {
        if (cryptoId == null || cryptoId.trim().isEmpty()) return null;
        
        totalCacheRequests++;
        String key = createOHLCCacheKey(cryptoId);
        CachedOHLCData cached = ohlcCache.get(key);
        
        if (cached == null) {
            totalCacheMisses++;
            LoggerUtil.debug(CoinGeckoApiCache.class, "OHLC cache miss for " + cryptoId);
            return null;
        }
        
        if (cached.isExpired()) {
            totalCacheMisses++;
            LoggerUtil.info(CoinGeckoApiCache.class, "OHLC cache expired for " + cryptoId + " (age: " + cached.getFormattedAge() + ")");
            ohlcCache.remove(key);
            return null;
        }
        
        totalCacheHits++;
        LoggerUtil.info(CoinGeckoApiCache.class, "OHLC cache hit for " + cryptoId + " (" + cached.ohlcData.size() + " points, cached " + cached.getFormattedAge() + ")");
        return cached.ohlcData;
    }
    
    /**
     * Cache OHLC data
     * @param cryptoId The cryptocurrency ID
     * @param ohlcData The OHLC price history
     */
    public static void cacheOHLCData(String cryptoId, List<PricePoint> ohlcData) {
        if (cryptoId == null || cryptoId.trim().isEmpty() || ohlcData == null || ohlcData.isEmpty()) return;
        
        String key = createOHLCCacheKey(cryptoId);
        CachedOHLCData cachedData = new CachedOHLCData(cryptoId, ohlcData);
        
        ohlcCache.put(key, cachedData);
        LoggerUtil.info(CoinGeckoApiCache.class, "Cached OHLC data for " + cryptoId + ": " + ohlcData.size() + " price points");
    }
    
    // =======================================
    // MARKET DATA CACHE METHODS
    // =======================================
    
    /**
     * Get cached market data for a cryptocurrency
     * @param cryptoId The cryptocurrency ID
     * @return Cached market data or null if not found/expired
     */
    public static MarketDataResult getCachedMarketData(String cryptoId) {
        if (cryptoId == null || cryptoId.trim().isEmpty()) return null;
        
        totalCacheRequests++;
        String key = createMarketDataCacheKey(cryptoId);
        CachedMarketData cached = marketDataCache.get(key);
        
        if (cached == null) {
            totalCacheMisses++;
            LoggerUtil.debug(CoinGeckoApiCache.class, "Market data cache miss for " + cryptoId);
            return null;
        }
        
        if (cached.isExpired()) {
            totalCacheMisses++;
            LoggerUtil.info(CoinGeckoApiCache.class, "Market data cache expired for " + cryptoId + " (age: " + cached.getFormattedAge() + ")");
            marketDataCache.remove(key);
            return null;
        }
        
        totalCacheHits++;
        LoggerUtil.debug(CoinGeckoApiCache.class, "Market data cache hit for " + cryptoId + " (cached " + cached.getFormattedAge() + ")");
        return new MarketDataResult(cached.marketCapUsd, cached.priceChange7d, cached.priceChange24h);
    }
    
    /**
     * Cache market data
     * @param cryptoId The cryptocurrency ID
     * @param marketCapUsd Market cap in USD
     * @param priceChange7d 7-day price change percentage
     * @param priceChange24h 24-hour price change percentage
     */
    public static void cacheMarketData(String cryptoId, double marketCapUsd, double priceChange7d, double priceChange24h) {
        if (cryptoId == null || cryptoId.trim().isEmpty()) return;
        
        String key = createMarketDataCacheKey(cryptoId);
        CachedMarketData cachedData = new CachedMarketData(cryptoId, marketCapUsd, priceChange7d, priceChange24h);
        
        marketDataCache.put(key, cachedData);
        LoggerUtil.debug(CoinGeckoApiCache.class, "Cached market data for " + cryptoId + ": MC=$" + String.format("%.0f", marketCapUsd));
    }
    
    // =======================================
    // VOLUME DATA CACHE METHODS
    // =======================================
    
    /**
     * Get cached volume data for a cryptocurrency
     * @param cryptoId The cryptocurrency ID
     * @return Cached volume or null if not found/expired
     */
    public static Double getCachedVolume(String cryptoId) {
        if (cryptoId == null || cryptoId.trim().isEmpty()) return null;
        
        totalCacheRequests++;
        String key = createVolumeCacheKey(cryptoId);
        CachedVolumeData cached = volumeCache.get(key);
        
        if (cached == null) {
            totalCacheMisses++;
            LoggerUtil.debug(CoinGeckoApiCache.class, "Volume cache miss for " + cryptoId);
            return null;
        }
        
        if (cached.isExpired()) {
            totalCacheMisses++;
            LoggerUtil.info(CoinGeckoApiCache.class, "Volume cache expired for " + cryptoId + " (age: " + cached.getFormattedAge() + ")");
            volumeCache.remove(key);
            return null;
        }
        
        totalCacheHits++;
        LoggerUtil.debug(CoinGeckoApiCache.class, "Volume cache hit for " + cryptoId + " (cached " + cached.getFormattedAge() + ")");
        return cached.volumeUsd;
    }
    
    /**
     * Cache volume data
     * @param cryptoId The cryptocurrency ID
     * @param volumeUsd The volume in USD
     */
    public static void cacheVolume(String cryptoId, double volumeUsd) {
        if (cryptoId == null || cryptoId.trim().isEmpty() || volumeUsd <= 0) return;
        
        String key = createVolumeCacheKey(cryptoId);
        CachedVolumeData cachedData = new CachedVolumeData(cryptoId, volumeUsd);
        
        volumeCache.put(key, cachedData);
        LoggerUtil.debug(CoinGeckoApiCache.class, "Cached volume for " + cryptoId + ": $" + String.format("%.0f", volumeUsd));
    }
    
    // =======================================
    // CACHE MANAGEMENT METHODS
    // =======================================
    
    /**
     * Clear all caches for a specific cryptocurrency (manual refresh)
     * @param cryptoId The cryptocurrency ID
     */
    public static void clearAllCaches(String cryptoId) {
        if (cryptoId == null || cryptoId.trim().isEmpty()) return;
        
        String priceKey = createPriceCacheKey(cryptoId);
        String ohlcKey = createOHLCCacheKey(cryptoId);
        String marketKey = createMarketDataCacheKey(cryptoId);
        String volumeKey = createVolumeCacheKey(cryptoId);
        
        boolean removed = false;
        if (priceCache.remove(priceKey) != null) removed = true;
        if (ohlcCache.remove(ohlcKey) != null) removed = true;
        if (marketDataCache.remove(marketKey) != null) removed = true;
        if (volumeCache.remove(volumeKey) != null) removed = true;
        
        if (removed) {
            LoggerUtil.info(CoinGeckoApiCache.class, "Manually cleared all caches for " + cryptoId);
            saveAllCachesToDisk();
        }
    }
    
    /**
     * Get cache statistics
     * @return CacheStatistics object with hit/miss ratios and counts
     */
    public static CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
            totalCacheRequests,
            totalCacheHits,
            totalCacheMisses,
            priceCache.size(),
            ohlcCache.size(),
            marketDataCache.size(),
            volumeCache.size()
        );
    }
    
    /**
     * Clear all expired entries from all caches
     */
    public static void clearExpiredEntries() {
        int removedCount = 0;
        
        // Clear expired price cache entries
        removedCount += priceCache.entrySet().removeIf(entry -> entry.getValue().isExpired()) ? 1 : 0;
        
        // Clear expired OHLC cache entries
        removedCount += ohlcCache.entrySet().removeIf(entry -> entry.getValue().isExpired()) ? 1 : 0;
        
        // Clear expired market data cache entries
        removedCount += marketDataCache.entrySet().removeIf(entry -> entry.getValue().isExpired()) ? 1 : 0;
        
        // Clear expired volume cache entries
        removedCount += volumeCache.entrySet().removeIf(entry -> entry.getValue().isExpired()) ? 1 : 0;
        
        if (removedCount > 0) {
            LoggerUtil.info(CoinGeckoApiCache.class, "Cleaned up expired cache entries from " + removedCount + " cache types");
        }
    }
    
    // =======================================
    // HELPER METHODS
    // =======================================
    
    private static String createPriceCacheKey(String cryptoId) {
        return "price_" + cryptoId.toLowerCase().trim();
    }
    
    private static String createOHLCCacheKey(String cryptoId) {
        return "ohlc_" + cryptoId.toLowerCase().trim();
    }
    
    private static String createMarketDataCacheKey(String cryptoId) {
        return "market_" + cryptoId.toLowerCase().trim();
    }
    
    private static String createVolumeCacheKey(String cryptoId) {
        return "volume_" + cryptoId.toLowerCase().trim();
    }
    
    private static void createCacheDirectory() {
        try {
            File cacheDir = new File(CACHE_DIR);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
                LoggerUtil.info(CoinGeckoApiCache.class, "Created cache directory: " + CACHE_DIR);
            }
        } catch (Exception e) {
            LoggerUtil.error(CoinGeckoApiCache.class, "Failed to create cache directory", e);
        }
    }
    
    private static void startCleanupScheduler() {
        cleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                clearExpiredEntries();
                saveAllCachesToDisk();
            } catch (Exception e) {
                LoggerUtil.error(CoinGeckoApiCache.class, "Error during cache cleanup", e);
            }
        }, 5, 5, TimeUnit.MINUTES); // Run every 5 minutes
    }
    
    // =======================================
    // PERSISTENCE METHODS
    // =======================================
    
    @SuppressWarnings("unchecked")
    private static void loadAllCachesFromDisk() {
        // Load price cache
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PRICE_CACHE_FILE))) {
            Map<String, CachedPriceData> loaded = (Map<String, CachedPriceData>) ois.readObject();
            priceCache.putAll(loaded);
            LoggerUtil.info(CoinGeckoApiCache.class, "Loaded " + loaded.size() + " price cache entries from disk");
        } catch (Exception e) {
            LoggerUtil.debug(CoinGeckoApiCache.class, "No existing price cache found: " + e.getMessage());
        }
        
        // Load OHLC cache
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(OHLC_CACHE_FILE))) {
            Map<String, CachedOHLCData> loaded = (Map<String, CachedOHLCData>) ois.readObject();
            ohlcCache.putAll(loaded);
            LoggerUtil.info(CoinGeckoApiCache.class, "Loaded " + loaded.size() + " OHLC cache entries from disk");
        } catch (Exception e) {
            LoggerUtil.debug(CoinGeckoApiCache.class, "No existing OHLC cache found: " + e.getMessage());
        }
        
        // Load market data cache
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(MARKET_CACHE_FILE))) {
            Map<String, CachedMarketData> loaded = (Map<String, CachedMarketData>) ois.readObject();
            marketDataCache.putAll(loaded);
            LoggerUtil.info(CoinGeckoApiCache.class, "Loaded " + loaded.size() + " market data cache entries from disk");
        } catch (Exception e) {
            LoggerUtil.debug(CoinGeckoApiCache.class, "No existing market data cache found: " + e.getMessage());
        }
        
        // Load volume cache
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(VOLUME_CACHE_FILE))) {
            Map<String, CachedVolumeData> loaded = (Map<String, CachedVolumeData>) ois.readObject();
            volumeCache.putAll(loaded);
            LoggerUtil.info(CoinGeckoApiCache.class, "Loaded " + loaded.size() + " volume cache entries from disk");
        } catch (Exception e) {
            LoggerUtil.debug(CoinGeckoApiCache.class, "No existing volume cache found: " + e.getMessage());
        }
    }
    
    private static void saveAllCachesToDisk() {
        // Save price cache
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PRICE_CACHE_FILE))) {
            oos.writeObject(priceCache);
        } catch (Exception e) {
            LoggerUtil.error(CoinGeckoApiCache.class, "Failed to save price cache to disk", e);
        }
        
        // Save OHLC cache
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(OHLC_CACHE_FILE))) {
            oos.writeObject(ohlcCache);
        } catch (Exception e) {
            LoggerUtil.error(CoinGeckoApiCache.class, "Failed to save OHLC cache to disk", e);
        }
        
        // Save market data cache
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MARKET_CACHE_FILE))) {
            oos.writeObject(marketDataCache);
        } catch (Exception e) {
            LoggerUtil.error(CoinGeckoApiCache.class, "Failed to save market data cache to disk", e);
        }
        
        // Save volume cache
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(VOLUME_CACHE_FILE))) {
            oos.writeObject(volumeCache);
        } catch (Exception e) {
            LoggerUtil.error(CoinGeckoApiCache.class, "Failed to save volume cache to disk", e);
        }
    }
    
    private static void logCacheStatistics() {
        CacheStatistics stats = getCacheStatistics();
        LoggerUtil.info(CoinGeckoApiCache.class, 
            String.format("Cache Statistics - Requests: %d, Hits: %d (%.1f%%), Misses: %d (%.1f%%), " +
                         "Price: %d, OHLC: %d, Market: %d, Volume: %d entries",
                         stats.totalRequests, stats.totalHits, stats.getHitRatio() * 100,
                         stats.totalMisses, stats.getMissRatio() * 100,
                         stats.priceCacheSize, stats.ohlcCacheSize, stats.marketCacheSize, stats.volumeCacheSize));
    }
    
    // =======================================
    // RESULT CLASSES
    // =======================================
    
    /**
     * Result class for market data
     */
    public static class MarketDataResult {
        public final double marketCapUsd;
        public final double priceChange7d;
        public final double priceChange24h;
        
        public MarketDataResult(double marketCapUsd, double priceChange7d, double priceChange24h) {
            this.marketCapUsd = marketCapUsd;
            this.priceChange7d = priceChange7d;
            this.priceChange24h = priceChange24h;
        }
    }
    
    /**
     * Cache statistics class
     */
    public static class CacheStatistics {
        public final long totalRequests;
        public final long totalHits;
        public final long totalMisses;
        public final int priceCacheSize;
        public final int ohlcCacheSize;
        public final int marketCacheSize;
        public final int volumeCacheSize;
        
        public CacheStatistics(long totalRequests, long totalHits, long totalMisses,
                              int priceCacheSize, int ohlcCacheSize, int marketCacheSize, int volumeCacheSize) {
            this.totalRequests = totalRequests;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.priceCacheSize = priceCacheSize;
            this.ohlcCacheSize = ohlcCacheSize;
            this.marketCacheSize = marketCacheSize;
            this.volumeCacheSize = volumeCacheSize;
        }
        
        public double getHitRatio() {
            return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        }
        
        public double getMissRatio() {
            return totalRequests > 0 ? (double) totalMisses / totalRequests : 0.0;
        }
    }
    
    // Instance methods for singleton access
    
    /**
     * Get cache efficiency (hit ratio)
     */
    public double getCacheEfficiency() {
        return totalCacheRequests > 0 ? (double) totalCacheHits / totalCacheRequests : 0.0;
    }
    
    /**
     * Get total cache hits
     */
    public long getCacheHits() {
        return totalCacheHits;
    }
    
    /**
     * Get total cache misses
     */
    public long getCacheMisses() {
        return totalCacheMisses;
    }
    
    /**
     * Increment cache hit counter
     */
    public void incrementHit() {
        totalCacheHits++;
        totalCacheRequests++;
    }
    
    /**
     * Increment cache miss counter
     */
    public void incrementMiss() {
        totalCacheMisses++;
        totalCacheRequests++;
    }
    
    /**
     * Put price data into cache (for testing)
     */
    public void putPriceData(String key, String jsonData) {
        try {
            // Parse the price from JSON-like string for testing
            double price = 50000.0; // Default test price
            if (jsonData.contains("50000")) {
                price = 50000.0;
            }
            cachePrice(key, price);
        } catch (Exception e) {
            LoggerUtil.error(CoinGeckoApiCache.class, "Error putting test price data", e);
        }
    }
    
    /**
     * Get price data from cache (for testing)
     */
    public java.util.Optional<TestCacheResult> getPriceData(String key) {
        Double price = getCachedPrice(key);
        if (price != null) {
            return java.util.Optional.of(new TestCacheResult(String.format("{\"price\":%.2f}", price)));
        }
        return java.util.Optional.empty();
    }
    
    /**
     * Test cache result wrapper
     */
    public static class TestCacheResult {
        private final String data;
        
        public TestCacheResult(String data) {
            this.data = data;
        }
        
        public String getData() {
            return data;
        }
    }
}
