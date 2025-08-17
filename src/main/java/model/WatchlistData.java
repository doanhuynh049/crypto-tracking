package model;

import java.io.Serializable;

/**
 * Unified data class for cryptocurrency tracking that serves as both watchlist and portfolio.
 * Items can be watched for entry opportunities AND track actual holdings.
 */
public class WatchlistData extends CryptoData implements Serializable {
    private static final long serialVersionUID = 1L;

    // Only keep fields unique to WatchlistData
    public long dateAdded;              // When added to watchlist
    public String notes;                // User notes about this crypto
    public double entryQualityScore;    // 0-100 score based on technical analysis
    public String entrySignal;          // STRONG_BUY, BUY, NEUTRAL, WAIT, AVOID

    // AI advice (transient - not saved to file)
    public transient String aiAdvice = "Loading...";
    public transient boolean isAiGenerated = false;
    public transient String aiStatus = "LOADING"; // LOADING, AI_SUCCESS, FALLBACK, ERROR
    public transient long lastAiUpdate = 0;

    // Technical Analysis data (transient - not saved to file)
    public transient TechnicalIndicators technicalIndicators;
    public transient String technicalAnalysisStatus = "LOADING"; // LOADING, SUCCESS, ERROR
    public transient long lastTechnicalUpdate = 0;

    /**
     * Constructor for watchlist/portfolio item
     */
    public WatchlistData(String id, String name, String symbol, double currentPrice, 
                        double expectedEntry, double targetPrice3Month, double targetPriceLongTerm) {
        super(id, name, symbol, currentPrice, expectedEntry, expectedEntry, targetPrice3Month, targetPriceLongTerm, 0.0, 0.0);
        this.dateAdded = System.currentTimeMillis();
        this.notes = "";
        this.entryQualityScore = 0.0;
        this.entrySignal = "NEUTRAL";
        initializeAiFields();
    }

    /**
     * Constructor for portfolio item with holdings
     */
    public WatchlistData(String id, String name, String symbol, double currentPrice, 
                        double expectedEntry, double targetPrice3Month, double targetPriceLongTerm,
                        double holdings, double avgBuyPrice) {
        super(id, name, symbol, currentPrice, expectedEntry, expectedEntry, targetPrice3Month, targetPriceLongTerm, holdings, avgBuyPrice);
        this.dateAdded = System.currentTimeMillis();
        this.notes = "";
        this.entryQualityScore = 0.0;
        this.entrySignal = "NEUTRAL";
        initializeAiFields();
    }

    /**
     * Initialize AI and technical analysis fields (for transient data after deserialization)
     */
    public void initializeAiFields() {
        if (aiAdvice == null) aiAdvice = "Loading...";
        if (aiStatus == null) aiStatus = "LOADING";
        if (technicalAnalysisStatus == null) technicalAnalysisStatus = "LOADING";
        if (lastAiUpdate == 0) lastAiUpdate = System.currentTimeMillis();
        if (lastTechnicalUpdate == 0) lastTechnicalUpdate = System.currentTimeMillis();
    }

    /**
     * Calculate percentage difference from current price to expected entry
     */
    public double getEntryOpportunityPercentage() {
        if (getExpectedEntry() == 0) return 0;
        return (getCurrentPrice() - getExpectedEntry()) / getExpectedEntry();
    }

    /**
     * Get entry opportunity status
     */
    public String getEntryOpportunityStatus() {
        double percentage = getEntryOpportunityPercentage();
        if (percentage <= -0.10) return "🔥 EXCELLENT"; // 10%+ below target
        else if (percentage <= -0.05) return "✅ GOOD";  // 5-10% below target
        else if (percentage <= 0.05) return "⚖️ FAIR";   // Within 5% of target
        else if (percentage <= 0.15) return "⚠️ HIGH";   // 5-15% above target
        else return "❌ AVOID";                          // 15%+ above target
    }

    /**
     * Get potential upside to 3-month target
     */
    public double getPotentialUpside3Month() {
        if (getCurrentPrice() == 0) return 0;
        return (getTargetPrice3Month() - getCurrentPrice()) / getCurrentPrice();
    }

    /**
     * Get potential upside to long-term target
     */
    public double getPotentialUpsideLongTerm() {
        if (getCurrentPrice() == 0) return 0;
        return (getTargetPriceLongTerm() - getCurrentPrice()) / getCurrentPrice();
    }

    /**
     * Set AI advice with status
     */
    public void setAiAdvice(String advice, boolean isGenerated) {
        this.aiAdvice = advice;
        this.isAiGenerated = isGenerated;
        this.aiStatus = isGenerated ? "AI_SUCCESS" : "FALLBACK";
        this.lastAiUpdate = System.currentTimeMillis();
    }

    /**
     * Set AI advice error status
     */
    public void setAiAdviceError() {
        this.aiAdvice = "Error getting advice";
        this.isAiGenerated = false;
        this.aiStatus = "ERROR";
        this.lastAiUpdate = System.currentTimeMillis();
    }

    /**
     * Get AI advice with status indicator
     */
    public String getAiAdviceWithStatus() {
        if (aiStatus == null || aiStatus.equals("LOADING")) {
            return "🔄 Loading...";
        } else if (aiStatus.equals("AI_SUCCESS")) {
            return "🤖 " + aiAdvice;
        } else if (aiStatus.equals("FALLBACK")) {
            return "📊 " + aiAdvice;
        } else if (aiStatus.equals("ERROR")) {
            return "❌ " + aiAdvice;
        }
        return aiAdvice;
    }

    /**
     * Set technical analysis data
     */
    public void setTechnicalIndicators(TechnicalIndicators indicators) {
        this.technicalIndicators = indicators;
        this.technicalAnalysisStatus = "SUCCESS";
        this.lastTechnicalUpdate = System.currentTimeMillis();
        
        // Update entry quality based on technical analysis
        if (indicators != null) {
            this.entryQualityScore = indicators.getEntryQualityScore();
            this.entrySignal = indicators.getOverallEntryQuality().toString();
        }
    }

    /**
     * Set technical analysis error status
     */
    public void setTechnicalAnalysisError() {
        this.technicalIndicators = null;
        this.technicalAnalysisStatus = "ERROR";
        this.lastTechnicalUpdate = System.currentTimeMillis();
        this.entryQualityScore = 0.0;
        this.entrySignal = "UNKNOWN";
    }

    /**
     * Get technical analysis status with indicator
     */
    public String getTechnicalAnalysisStatus() {
        if (technicalAnalysisStatus == null || technicalAnalysisStatus.equals("LOADING")) {
            return "🔄 Loading...";
        } else if (technicalAnalysisStatus.equals("SUCCESS")) {
            if (technicalIndicators != null) {
                return "📊 " + technicalIndicators.getOverallEntryQuality().toString();
            }
            return "📊 Available";
        } else if (technicalAnalysisStatus.equals("ERROR")) {
            return "❌ Error";
        }
        return "❓ Unknown";
    }

    /**
     * Check if technical analysis is available
     */
    public boolean hasTechnicalAnalysis() {
        return technicalIndicators != null && technicalAnalysisStatus.equals("SUCCESS");
    }

    /**
     * Get technical analysis summary
     */
    public String getTechnicalAnalysisSummary() {
        if (hasTechnicalAnalysis()) {
            return technicalIndicators.getAnalysisSummary();
        }
        return "Technical analysis not available";
    }

    /**
     * Get entry quality color based on score
     */
    public String getEntryQualityColor() {
        if (entryQualityScore >= 80) return "#4CAF50";  // Green
        else if (entryQualityScore >= 60) return "#8BC34A";  // Light Green
        else if (entryQualityScore >= 40) return "#FFC107";  // Yellow
        else if (entryQualityScore >= 20) return "#FF9800";  // Orange
        else return "#F44336";  // Red
    }

    /**
     * Get entry opportunity score (same as entryQualityScore for compatibility)
     */
    public double getEntryOpportunityScore() {
        return entryQualityScore;
    }

    /**
     * Get days since added to watchlist
     */
    public long getDaysSinceAdded() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - dateAdded) / (1000 * 60 * 60 * 24);
    }

    /**
     * Update the current price for this item
     */
    public void updatePrice(double newPrice) {
        setCurrentPrice(newPrice);
    }

    /**
     * Check if this item has actual holdings (portfolio item) or is watchlist only
     */
    public boolean hasHoldings() {
        return getHoldings() > 0;
    }

    /**
     * Check if this is a watchlist-only item (no holdings)
     */
    public boolean isWatchlistOnly() {
        return getHoldings() == 0;
    }

    /**
     * Get total portfolio value for this holding
     */
    public double getTotalValue() {
        return getHoldings() * getCurrentPrice();
    }

    /**
     * Get profit/loss for this holding
     */
    public double getProfitLoss() {
        if (getHoldings() == 0 || getAvgBuyPrice() == 0) return 0.0;
        return getTotalValue() - (getHoldings() * getAvgBuyPrice());
    }

    /**
     * Get profit/loss percentage for this holding
     */
    public double getProfitLossPercentage() {
        if (getHoldings() == 0 || getAvgBuyPrice() == 0) return 0.0;
        return (getCurrentPrice() - getAvgBuyPrice()) / getAvgBuyPrice();
    }

    /**
     * Add to holdings (buy more)
     */
    public void addHoldings(double amount, double buyPrice) {
        if (amount <= 0) return;
        
        // Calculate new average buy price
        double totalCost = (getHoldings() * getAvgBuyPrice()) + (amount * buyPrice);
        setHoldings(getHoldings() + amount, totalCost / (getHoldings() + amount));
    }

    /**
     * Remove from holdings (sell)
     */
    public boolean removeHoldings(double amount) {
        if (amount <= 0 || amount > getHoldings()) return false;
        
        setHoldings(getHoldings() - amount, getAvgBuyPrice());
        
        // If all holdings sold, reset average buy price
        if (getHoldings() == 0) {
            setHoldings(0, 0.0);
        }
        
        return true;
    }

    /**
     * Set target entry price
     */
    public void setTargetEntryPrice(double targetPrice) {
        setExpectedEntry(targetPrice);
    }

    /**
     * Set notes for this watchlist item
     */
    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    @Override
    public String toString() {
        if (hasHoldings()) {
            return String.format("%s (%s) - Holdings: %.4f @ $%.2f, Current: $%.2f, Entry: $%.2f", 
                               getName(), getSymbol(), getHoldings(), getAvgBuyPrice(), getCurrentPrice(), getExpectedEntry());
        } else {
            return String.format("%s (%s) - Watchlist - Entry: $%.2f, Current: $%.2f, Quality: %.1f", 
                               getName(), getSymbol(), getExpectedEntry(), getCurrentPrice(), entryQualityScore);
        }
    }
}
