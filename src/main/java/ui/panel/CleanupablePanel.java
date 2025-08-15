package ui;

/**
 * Interface for panels that need cleanup when being removed or switched away from.
 * This ensures proper cleanup of timers, threads, and other resources.
 */
public interface CleanupablePanel {
    
    /**
     * Called when the panel is being removed or switched away from.
     * Implementations should stop all timers, background threads, and cleanup resources.
     */
    void cleanup();
    
    /**
     * Called when the panel is being activated or switched to.
     * Implementations should start necessary timers and background operations.
     */
    void activate();
    
    /**
     * Returns true if this panel has active background operations that need cleanup.
     */
    default boolean hasActiveOperations() {
        return false;
    }
}
