package model;

import java.io.Serializable;

/**
 * Unified data class for cryptocurrency tracking that serves as both watchlist and portfolio.
 * Items can be watched for entry opportunities AND track actual holdings.
 */
public class WatchlistData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Basic crypto information
    public String id;
    public String name;
    public String symbol;
    public double currentPrice;
    
    // Portfolio data (actual holdings)
    public double holdings = 0.0;           // Amount owned (0 = watchlist only)
    public double avgBuyPrice = 0.0;        // Average purchase price
    
    // Target prices and entry analysis
    public double expectedEntry;        // Target entry price
    public double targetPrice3Month;    // 3-month target
    public double targetPriceLongTerm;  // Long-term target
    public long dateAdded;              // When added to watchlist
    public String notes;                // User notes about this crypto
    
    // Entry quality assessment
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
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.expectedEntry = expectedEntry;
        this.targetPrice3Month = targetPrice3Month;
        this.targetPriceLongTerm = targetPriceLongTerm;
        this.dateAdded = System.currentTimeMillis();
        this.notes = "";
        this.entryQualityScore = 0.0;
        this.entrySignal = "NEUTRAL";
        
        // Portfolio fields - default to watchlist only
        this.holdings = 0.0;
        this.avgBuyPrice = 0.0;
        
        // Initialize transient AI fields
        initializeAiFields();
    }
    
    /**
     * Constructor for portfolio item with holdings
     */
    public WatchlistData(String id, String name, String symbol, double currentPrice, 
                        double expectedEntry, double targetPrice3Month, double targetPriceLongTerm,
                        double holdings, double avgBuyPrice) {
        this(id, name, symbol, currentPrice, expectedEntry, targetPrice3Month, targetPriceLongTerm);
        this.holdings = holdings;
        this.avgBuyPrice = avgBuyPrice;
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
        if (expectedEntry == 0) return 0;
        return (currentPrice - expectedEntry) / expectedEntry;
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
        if (currentPrice == 0) return 0;
        return (targetPrice3Month - currentPrice) / currentPrice;
    }
    
    /**
     * Get potential upside to long-term target
     */
    public double getPotentialUpsideLongTerm() {
        if (currentPrice == 0) return 0;
        return (targetPriceLongTerm - currentPrice) / currentPrice;
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
     * Get the cryptocurrency symbol
     */
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * Get the current price
     */
    public double getCurrentPrice() {
        return currentPrice;
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
        this.currentPrice = newPrice;
        // Update entry opportunity when price changes
        updateEntryOpportunity();
    }
    
    /**
     * Check if this item has actual holdings (portfolio item) or is watchlist only
     */
    public boolean hasHoldings() {
        return holdings > 0;
    }
    
    /**
     * Check if this is a watchlist-only item (no holdings)
     */
    public boolean isWatchlistOnly() {
        return holdings == 0;
    }
    
    /**
     * Get total portfolio value for this holding
     */
    public double getTotalValue() {
        return holdings * currentPrice;
    }
    
    /**
     * Get profit/loss for this holding
     */
    public double getProfitLoss() {
        if (holdings == 0 || avgBuyPrice == 0) return 0.0;
        return getTotalValue() - (holdings * avgBuyPrice);
    }
    
    /**
     * Get profit/loss percentage for this holding
     */
    public double getProfitLossPercentage() {
        if (holdings == 0 || avgBuyPrice == 0) return 0.0;
        return (currentPrice - avgBuyPrice) / avgBuyPrice;
    }
    
    /**
     * Add to holdings (buy more)
     */
    public void addHoldings(double amount, double buyPrice) {
        if (amount <= 0) return;
        
        // Calculate new average buy price
        double totalCost = (holdings * avgBuyPrice) + (amount * buyPrice);
        holdings += amount;
        avgBuyPrice = totalCost / holdings;
    }
    
    /**
     * Remove from holdings (sell)
     */
    public boolean removeHoldings(double amount) {
        if (amount <= 0 || amount > holdings) return false;
        
        holdings -= amount;
        
        // If all holdings sold, reset average buy price
        if (holdings == 0) {
            avgBuyPrice = 0.0;
        }
        
        return true;
    }
    
    /**
     * Set holdings directly (for portfolio import/editing)
     */
    public void setHoldings(double holdings, double avgBuyPrice) {
        this.holdings = Math.max(0, holdings);
        this.avgBuyPrice = holdings > 0 ? avgBuyPrice : 0.0;
    }
    
    /**
     * Convert to CryptoData for analysis purposes or legacy compatibility
     */
    public CryptoData toCryptoData() {
        CryptoData cryptoData = new CryptoData(
            this.id,
            this.name, 
            this.symbol,
            this.currentPrice,
            this.expectedEntry, // Use expected entry as expected price
            this.expectedEntry,
            this.targetPrice3Month,
            this.targetPriceLongTerm,
            this.holdings,    // Include actual holdings
            this.avgBuyPrice  // Include average buy price
        );
        
        // Copy technical analysis if available
        if (this.technicalIndicators != null) {
            cryptoData.technicalIndicators = this.technicalIndicators;
            cryptoData.technicalAnalysisStatus = this.technicalAnalysisStatus;
            cryptoData.lastTechnicalUpdate = this.lastTechnicalUpdate;
        }
        
        // Copy AI advice if available  
        if (this.aiAdvice != null) {
            cryptoData.aiAdvice = this.aiAdvice;
            cryptoData.isAiGenerated = this.isAiGenerated;
            cryptoData.aiStatus = this.aiStatus;
            cryptoData.lastAiUpdate = this.lastAiUpdate;
        }
        
        return cryptoData;
    }
    
    /**
     * Update entry opportunity assessment based on current market conditions
     */
    public void updateEntryOpportunity() {
        // Calculate entry opportunity score based on price vs target
        double priceRatio = getCurrentPrice() / expectedEntry;
        double opportunityScore = 100.0;
        
        if (priceRatio <= 0.90) {
            // 10%+ below target - excellent opportunity
            opportunityScore = 90.0 + (10.0 * (0.90 - priceRatio) / 0.10);
        } else if (priceRatio <= 0.95) {
            // 5-10% below target - good opportunity  
            opportunityScore = 80.0 + (10.0 * (0.95 - priceRatio) / 0.05);
        } else if (priceRatio <= 1.05) {
            // Within 5% of target - fair opportunity
            opportunityScore = 60.0 + (20.0 * (1.05 - priceRatio) / 0.10);
        } else if (priceRatio <= 1.15) {
            // 5-15% above target - poor opportunity
            opportunityScore = 20.0 + (40.0 * (1.15 - priceRatio) / 0.10);
        } else {
            // 15%+ above target - avoid
            opportunityScore = Math.max(0.0, 20.0 * (1.30 - priceRatio) / 0.15);
        }
        
        this.entryQualityScore = Math.max(0.0, Math.min(100.0, opportunityScore));
        
        // Update entry signal based on score
        if (this.entryQualityScore >= 85) {
            this.entrySignal = "STRONG_BUY";
        } else if (this.entryQualityScore >= 70) {
            this.entrySignal = "BUY";
        } else if (this.entryQualityScore >= 40) {
            this.entrySignal = "NEUTRAL";
        } else if (this.entryQualityScore >= 20) {
            this.entrySignal = "WAIT";
        } else {
            this.entrySignal = "AVOID";
        }
    }
    
    /**
     * Get entry status enum
     */
    public EntryStatus getEntryStatus() {
        double percentage = getEntryOpportunityPercentage();
        if (percentage <= -0.10) return EntryStatus.EXCELLENT;
        else if (percentage <= -0.05) return EntryStatus.GOOD;
        else if (percentage <= 0.05) return EntryStatus.FAIR;
        else if (percentage <= 0.15) return EntryStatus.HIGH;
        else return EntryStatus.AVOID;
    }
    
    /**
     * Set target entry price
     */
    public void setTargetEntryPrice(double targetPrice) {
        this.expectedEntry = targetPrice;
        updateEntryOpportunity(); // Recalculate opportunity when target changes
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
                               name, symbol, holdings, avgBuyPrice, currentPrice, expectedEntry);
        } else {
            return String.format("%s (%s) - Watchlist - Entry: $%.2f, Current: $%.2f, Quality: %.1f", 
                               name, symbol, expectedEntry, currentPrice, entryQualityScore);
        }
    }
}
