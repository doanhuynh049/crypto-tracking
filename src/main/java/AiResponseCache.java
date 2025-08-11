import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cache system for AI responses to reduce API calls and avoid rate limiting
 * Automatically expires cache entries after 12 hours
 */
public class AiResponseCache {
    
    // Cache storage
    private static final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    
    // Cache configuration
    private static final long CACHE_DURATION_HOURS = 12;
    private static final long CACHE_DURATION_MS = CACHE_DURATION_HOURS * 60 * 60 * 1000;
    private static final String CACHE_FILE_PATH = "cache/ai_responses.dat";
    
    // Cleanup scheduler
    private static final ScheduledExecutorService cleanupScheduler = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AI-Cache-Cleanup");
            t.setDaemon(true);
            return t;
        });
    
    // Static initialization
    static {
        loadCacheFromDisk();
        startCleanupScheduler();
        
        // Shutdown hook to save cache
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveCacheToDisk();
            cleanupScheduler.shutdown();
        }));
    }
    
    /**
     * Cached response data structure
     */
    private static class CachedResponse implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final String response;
        final long timestamp;
        final String cryptoSymbol;
        final LocalDateTime createdAt;
        
        CachedResponse(String response, String cryptoSymbol) {
            this.response = response;
            this.cryptoSymbol = cryptoSymbol;
            this.timestamp = System.currentTimeMillis();
            this.createdAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_DURATION_MS;
        }
        
        long getAgeInMinutes() {
            return (System.currentTimeMillis() - timestamp) / (60 * 1000);
        }
        
        String getFormattedAge() {
            long ageMinutes = getAgeInMinutes();
            if (ageMinutes < 60) {
                return ageMinutes + " minutes ago";
            } else {
                long ageHours = ageMinutes / 60;
                return ageHours + " hours ago";
            }
        }
    }
    
    /**
     * Get cached AI response for a cryptocurrency
     * @param cryptoSymbol The cryptocurrency symbol
     * @return Cached response or null if not found/expired
     */
    public static String getCachedResponse(String cryptoSymbol) {
        if (cryptoSymbol == null || cryptoSymbol.trim().isEmpty()) {
            return null;
        }
        
        String key = createCacheKey(cryptoSymbol);
        CachedResponse cached = cache.get(key);
        
        if (cached == null) {
            LoggerUtil.debug(AiResponseCache.class, "No cached response found for " + cryptoSymbol);
            return null;
        }
        
        if (cached.isExpired()) {
            LoggerUtil.info(AiResponseCache.class, "Cached response expired for " + cryptoSymbol + " (age: " + cached.getFormattedAge() + ")");
            cache.remove(key);
            saveCacheToDisk(); // Save immediately after removal
            return null;
        }
        
        LoggerUtil.info(AiResponseCache.class, "Using cached AI response for " + cryptoSymbol + " (cached " + cached.getFormattedAge() + ")");
        return cached.response;
    }
    
    /**
     * Cache a successful AI response
     * @param cryptoSymbol The cryptocurrency symbol
     * @param response The AI response to cache
     */
    public static void cacheResponse(String cryptoSymbol, String response) {
        if (cryptoSymbol == null || cryptoSymbol.trim().isEmpty() || 
            response == null || response.trim().isEmpty()) {
            LoggerUtil.warning(AiResponseCache.class, "Cannot cache empty response for " + cryptoSymbol);
            return;
        }
        
        String key = createCacheKey(cryptoSymbol);
        CachedResponse cachedResponse = new CachedResponse(response, cryptoSymbol);
        
        cache.put(key, cachedResponse);
        LoggerUtil.info(AiResponseCache.class, "Cached AI response for " + cryptoSymbol + " (expires in " + CACHE_DURATION_HOURS + " hours)");
        
        // Save to disk asynchronously
        saveCacheToDisk();
    }
    
    /**
     * Manually clear cache for a specific cryptocurrency (refresh functionality)
     * @param cryptoSymbol The cryptocurrency symbol
     */
    public static void clearCache(String cryptoSymbol) {
        if (cryptoSymbol == null || cryptoSymbol.trim().isEmpty()) {
            return;
        }
        
        String key = createCacheKey(cryptoSymbol);
        CachedResponse removed = cache.remove(key);
        
        if (removed != null) {
            LoggerUtil.info(AiResponseCache.class, "Manually cleared cache for " + cryptoSymbol);
            saveCacheToDisk();
        } else {
            LoggerUtil.debug(AiResponseCache.class, "No cache to clear for " + cryptoSymbol);
        }
    }
    
    /**
     * Clear all cached responses
     */
    public static void clearAllCache() {
        int size = cache.size();
        cache.clear();
        LoggerUtil.info(AiResponseCache.class, "Cleared all cached AI responses (" + size + " entries)");
        saveCacheToDisk();
    }
    
    /**
     * Get cache statistics
     */
    public static CacheStats getCacheStats() {
        int totalEntries = cache.size();
        int expiredEntries = 0;
        long oldestEntryAge = 0;
        long newestEntryAge = Long.MAX_VALUE;
        
        for (CachedResponse cached : cache.values()) {
            if (cached.isExpired()) {
                expiredEntries++;
            }
            long age = cached.getAgeInMinutes();
            if (age > oldestEntryAge) {
                oldestEntryAge = age;
            }
            if (age < newestEntryAge) {
                newestEntryAge = age;
            }
        }
        
        if (totalEntries == 0) {
            newestEntryAge = 0;
        }
        
        return new CacheStats(totalEntries, expiredEntries, oldestEntryAge, newestEntryAge);
    }
    
    /**
     * Check if cache exists for a cryptocurrency
     */
    public static boolean hasCachedResponse(String cryptoSymbol) {
        if (cryptoSymbol == null || cryptoSymbol.trim().isEmpty()) {
            return false;
        }
        
        String key = createCacheKey(cryptoSymbol);
        CachedResponse cached = cache.get(key);
        return cached != null && !cached.isExpired();
    }
    
    /**
     * Get formatted cache info for a cryptocurrency
     */
    public static String getCacheInfo(String cryptoSymbol) {
        if (cryptoSymbol == null || cryptoSymbol.trim().isEmpty()) {
            return "No cache info available";
        }
        
        String key = createCacheKey(cryptoSymbol);
        CachedResponse cached = cache.get(key);
        
        if (cached == null) {
            return "No cached data";
        }
        
        if (cached.isExpired()) {
            return "Cache expired (" + cached.getFormattedAge() + ")";
        }
        
        return "Cached " + cached.getFormattedAge();
    }
    
    // Private helper methods
    
    private static String createCacheKey(String cryptoSymbol) {
        return cryptoSymbol.toLowerCase().trim() + "_detailed_analysis";
    }
    
    private static void startCleanupScheduler() {
        // Run cleanup every hour
        cleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredEntries();
            } catch (Exception e) {
                LoggerUtil.error(AiResponseCache.class, "Error during cache cleanup: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }
    
    private static void cleanupExpiredEntries() {
        int removedCount = 0;
        
        for (Map.Entry<String, CachedResponse> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            LoggerUtil.info(AiResponseCache.class, "Cleaned up " + removedCount + " expired cache entries");
            saveCacheToDisk();
        }
    }
    
    private static void saveCacheToDisk() {
        try {
            // Create cache directory if it doesn't exist
            File cacheFile = new File(CACHE_FILE_PATH);
            File cacheDir = cacheFile.getParentFile();
            if (cacheDir != null && !cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            // Save cache to file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                oos.writeObject(cache);
                LoggerUtil.debug(AiResponseCache.class, "Saved cache to disk (" + cache.size() + " entries)");
            }
        } catch (Exception e) {
            LoggerUtil.error(AiResponseCache.class, "Failed to save cache to disk: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void loadCacheFromDisk() {
        try {
            File cacheFile = new File(CACHE_FILE_PATH);
            if (!cacheFile.exists()) {
                LoggerUtil.debug(AiResponseCache.class, "No cache file found, starting with empty cache");
                return;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
                Map<String, CachedResponse> loadedCache = (Map<String, CachedResponse>) ois.readObject();
                
                // Filter out expired entries during load
                int loadedCount = 0;
                int expiredCount = 0;
                
                for (Map.Entry<String, CachedResponse> entry : loadedCache.entrySet()) {
                    if (!entry.getValue().isExpired()) {
                        cache.put(entry.getKey(), entry.getValue());
                        loadedCount++;
                    } else {
                        expiredCount++;
                    }
                }
                
                LoggerUtil.info(AiResponseCache.class, "Loaded cache from disk: " + loadedCount + " valid entries, " + expiredCount + " expired entries discarded");
                
                // Save back to disk if we removed expired entries
                if (expiredCount > 0) {
                    saveCacheToDisk();
                }
            }
        } catch (Exception e) {
            LoggerUtil.warning(AiResponseCache.class, "Failed to load cache from disk: " + e.getMessage() + " - starting with empty cache");
            cache.clear();
        }
    }
    
    /**
     * Cache statistics data class
     */
    public static class CacheStats {
        public final int totalEntries;
        public final int expiredEntries;
        public final long oldestEntryAgeMinutes;
        public final long newestEntryAgeMinutes;
        
        CacheStats(int totalEntries, int expiredEntries, long oldestEntryAgeMinutes, long newestEntryAgeMinutes) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
            this.oldestEntryAgeMinutes = oldestEntryAgeMinutes;
            this.newestEntryAgeMinutes = newestEntryAgeMinutes;
        }
        
        @Override
        public String toString() {
            return String.format("Cache Stats: %d total, %d expired, oldest: %d min, newest: %d min", 
                totalEntries, expiredEntries, oldestEntryAgeMinutes, newestEntryAgeMinutes);
        }
    }
}
