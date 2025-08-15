package service;

import util.LoggerUtil;

/**
 * Central coordination service for API calls to prevent rate limiting conflicts
 * between different parts of the application (Portfolio, Watchlist, etc.)
 */
public class ApiCoordinationService {
    
    private static final ApiCoordinationService INSTANCE = new ApiCoordinationService();
    private static final Object globalApiLock = new Object();
    private static volatile long lastApiCallTime = 0;
    private static final long GLOBAL_API_DELAY_MS = 1000; // Increased to 8 seconds between any API calls
    
    // Intensive operation tracking
    private static volatile boolean intensiveOperationInProgress = false;
    private static volatile String intensiveOperationSystem = null;
    
    private ApiCoordinationService() {
        // Singleton
    }
    
    public static ApiCoordinationService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Request permission to make an API call with global rate limiting and priority handling
     * This ensures all parts of the application respect the same rate limits
     */
    public boolean requestApiCall(String requestingService, String operation) {
        synchronized (globalApiLock) {
            // If an intensive operation is in progress and this isn't part of it, defer
            if (intensiveOperationInProgress && 
                !requestingService.equals(intensiveOperationSystem) &&
                !requestingService.equals("TechnicalAnalysisService")) {
                
                LoggerUtil.info(ApiCoordinationService.class, 
                    String.format("[%s] Deferring %s - intensive operation by %s in progress", 
                        requestingService, operation, intensiveOperationSystem));
                return false;
            }
            
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastApiCallTime;
            
            if (timeSinceLastCall < GLOBAL_API_DELAY_MS) {
                long waitTime = GLOBAL_API_DELAY_MS - timeSinceLastCall;
                
                LoggerUtil.info(ApiCoordinationService.class, 
                    String.format("[%s] Global rate limiting: waiting %dms for %s", 
                        requestingService, waitTime, operation));
                
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            lastApiCallTime = System.currentTimeMillis();
            
            LoggerUtil.debug(ApiCoordinationService.class, 
                String.format("[%s] API call authorized for %s", requestingService, operation));
            
            return true;
        }
    }
    
    /**
     * Check if it's safe to make an API call without actually requesting permission
     */
    public boolean canMakeApiCall() {
        synchronized (globalApiLock) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastApiCallTime;
            return timeSinceLastCall >= GLOBAL_API_DELAY_MS;
        }
    }
    
    /**
     * Check if a specific system can make an API call (considering intensive operations)
     */
    public boolean canMakeApiCall(String requestingService, String operation) {
        synchronized (globalApiLock) {
            // If an intensive operation is in progress and this isn't part of it, can't make call
            if (intensiveOperationInProgress && 
                !requestingService.equals(intensiveOperationSystem) &&
                !requestingService.equals("TechnicalAnalysisService")) {
                return false;
            }
            
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastApiCallTime;
            return timeSinceLastCall >= GLOBAL_API_DELAY_MS;
        }
    }
    
    /**
     * Get time until next API call is allowed
     */
    public long getTimeUntilNextApiCall() {
        synchronized (globalApiLock) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastApiCallTime;
            return Math.max(0, GLOBAL_API_DELAY_MS - timeSinceLastCall);
        }
    }
    
    /**
     * Notify that a system is about to start intensive API operations
     * Other systems should defer their operations
     */
    public void notifyIntensiveOperationStart(String systemName) {
        synchronized (globalApiLock) {
            intensiveOperationInProgress = true;
            intensiveOperationSystem = systemName;
        }
        LoggerUtil.info(ApiCoordinationService.class, 
            String.format("System %s starting intensive API operations - others should defer", systemName));
    }
    
    /**
     * Notify that intensive API operations have completed
     */
    public void notifyIntensiveOperationComplete(String systemName) {
        synchronized (globalApiLock) {
            if (systemName.equals(intensiveOperationSystem)) {
                intensiveOperationInProgress = false;
                intensiveOperationSystem = null;
            }
        }
        LoggerUtil.info(ApiCoordinationService.class, 
            String.format("System %s completed intensive API operations", systemName));
    }
}
