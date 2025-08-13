package model;

/**
 * Entry status enum for watchlist items
 */
public enum EntryStatus {
    EXCELLENT("🔥 EXCELLENT"),
    GOOD("✅ GOOD"), 
    FAIR("⚖️ FAIR"),
    HIGH("⚠️ HIGH"),
    AVOID("❌ AVOID");
    
    private final String displayName;
    
    EntryStatus(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
