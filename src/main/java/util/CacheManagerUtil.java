package util;

import cache.CoinGeckoApiCache;

/**
 * Utility class for managing and displaying CoinGecko cache statistics and operations
 */
public class CacheManagerUtil {
    
    /**
     * Log comprehensive cache statistics
     */
    public static void logCacheStatistics() {
        CoinGeckoApiCache.CacheStatistics stats = CoinGeckoApiCache.getCacheStatistics();
        
        LoggerUtil.info(CacheManagerUtil.class, "=== CoinGecko Cache Statistics ===");
        LoggerUtil.info(CacheManagerUtil.class, String.format("Total Requests: %d", stats.totalRequests));
        LoggerUtil.info(CacheManagerUtil.class, String.format("Cache Hits: %d (%.1f%%)", 
            stats.totalHits, stats.getHitRatio() * 100));
        LoggerUtil.info(CacheManagerUtil.class, String.format("Cache Misses: %d (%.1f%%)", 
            stats.totalMisses, stats.getMissRatio() * 100));
        LoggerUtil.info(CacheManagerUtil.class, "--- Cache Sizes ---");
        LoggerUtil.info(CacheManagerUtil.class, String.format("Price Cache: %d entries", stats.priceCacheSize));
        LoggerUtil.info(CacheManagerUtil.class, String.format("OHLC Cache: %d entries", stats.ohlcCacheSize));
        LoggerUtil.info(CacheManagerUtil.class, String.format("Market Data Cache: %d entries", stats.marketCacheSize));
        LoggerUtil.info(CacheManagerUtil.class, String.format("Volume Cache: %d entries", stats.volumeCacheSize));
        
        // Calculate total cache entries
        int totalEntries = stats.priceCacheSize + stats.ohlcCacheSize + stats.marketCacheSize + stats.volumeCacheSize;
        LoggerUtil.info(CacheManagerUtil.class, String.format("Total Cached Entries: %d", totalEntries));
        
        // Estimate performance improvement
        if (stats.totalRequests > 0) {
            double apiCallsAvoided = stats.totalHits;
            double timeSavedSeconds = apiCallsAvoided * 1.5; // Assume 1.5 seconds per API call on average
            LoggerUtil.info(CacheManagerUtil.class, String.format("Estimated API calls avoided: %.0f", apiCallsAvoided));
            LoggerUtil.info(CacheManagerUtil.class, String.format("Estimated time saved: %.1f seconds", timeSavedSeconds));
        }
        LoggerUtil.info(CacheManagerUtil.class, "================================");
    }
    
    /**
     * Get cache efficiency as a percentage
     * @return Cache hit ratio as percentage (0-100)
     */
    public static double getCacheEfficiency() {
        CoinGeckoApiCache.CacheStatistics stats = CoinGeckoApiCache.getCacheStatistics();
        return stats.getHitRatio() * 100;
    }
    
    /**
     * Get total number of cached entries across all cache types
     * @return Total number of cached entries
     */
    public static int getTotalCachedEntries() {
        CoinGeckoApiCache.CacheStatistics stats = CoinGeckoApiCache.getCacheStatistics();
        return stats.priceCacheSize + stats.ohlcCacheSize + stats.marketCacheSize + stats.volumeCacheSize;
    }
    
    /**
     * Clear all caches for a specific cryptocurrency
     * @param cryptoId The cryptocurrency ID to clear from all caches
     */
    public static void clearCryptoCaches(String cryptoId) {
        CoinGeckoApiCache.clearAllCaches(cryptoId);
        LoggerUtil.info(CacheManagerUtil.class, "Cleared all caches for cryptocurrency: " + cryptoId);
    }
    
    /**
     * Clear all expired cache entries
     */
    public static void clearExpiredCaches() {
        CoinGeckoApiCache.clearExpiredEntries();
        LoggerUtil.info(CacheManagerUtil.class, "Cleared all expired cache entries");
    }
    
    /**
     * Get a formatted string with cache statistics for UI display
     * @return Formatted cache statistics string
     */
    public static String getFormattedCacheStats() {
        CoinGeckoApiCache.CacheStatistics stats = CoinGeckoApiCache.getCacheStatistics();
        
        if (stats.totalRequests == 0) {
            return "No cache activity yet";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Cache Efficiency: %.1f%% ", stats.getHitRatio() * 100));
        sb.append(String.format("(%d hits / %d requests)", stats.totalHits, stats.totalRequests));
        sb.append(String.format(" | Entries: %d", getTotalCachedEntries()));
        
        return sb.toString();
    }
    
    /**
     * Get cache performance recommendations based on current statistics
     * @return List of recommendations to improve cache performance
     */
    public static String[] getCacheRecommendations() {
        CoinGeckoApiCache.CacheStatistics stats = CoinGeckoApiCache.getCacheStatistics();
        
        if (stats.totalRequests < 10) {
            return new String[]{"Cache is warming up - more data needed for recommendations"};
        }
        
        double hitRatio = stats.getHitRatio();
        
        if (hitRatio > 0.8) {
            return new String[]{
                "✅ Excellent cache performance! Hit ratio: " + String.format("%.1f%%", hitRatio * 100),
                "Cache is effectively reducing API calls and improving performance"
            };
        } else if (hitRatio > 0.6) {
            return new String[]{
                "✅ Good cache performance. Hit ratio: " + String.format("%.1f%%", hitRatio * 100),
                "Consider increasing cache TTL for less volatile data to improve hit ratio"
            };
        } else if (hitRatio > 0.4) {
            return new String[]{
                "⚠️ Moderate cache performance. Hit ratio: " + String.format("%.1f%%", hitRatio * 100),
                "Consider reviewing cache TTL settings",
                "Check if data is being frequently invalidated unnecessarily"
            };
        } else {
            return new String[]{
                "❌ Poor cache performance. Hit ratio: " + String.format("%.1f%%", hitRatio * 100),
                "Review cache configuration and TTL settings",
                "Consider increasing cache duration for stable data",
                "Check for cache invalidation issues"
            };
        }
    }
    
    /**
     * Estimate bandwidth and time savings from caching
     * @return Formatted string with savings estimates
     */
    public static String getCacheSavingsEstimate() {
        CoinGeckoApiCache.CacheStatistics stats = CoinGeckoApiCache.getCacheStatistics();
        
        if (stats.totalHits == 0) {
            return "No cache hits yet - savings data not available";
        }
        
        // Conservative estimates
        double avgResponseSizeKB = 2.0; // Average API response size in KB
        double avgResponseTimeSeconds = 1.5; // Average API response time
        double avgCpuCostPercent = 0.1; // Approximate CPU cost of processing API response
        
        double bandwidthSavedKB = stats.totalHits * avgResponseSizeKB;
        double timeSavedSeconds = stats.totalHits * avgResponseTimeSeconds;
        double cpuSavedPercent = stats.totalHits * avgCpuCostPercent;
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Estimated savings from %d cache hits:\n", stats.totalHits));
        sb.append(String.format("• Bandwidth: %.1f KB\n", bandwidthSavedKB));
        sb.append(String.format("• Time: %.1f seconds\n", timeSavedSeconds));
        sb.append(String.format("• Reduced API load: %d requests", stats.totalHits));
        
        return sb.toString();
    }
}
