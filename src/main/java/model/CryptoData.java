package model;

import java.io.Serializable;

/**
 * Data class to hold cryptocurrency information
 * Contains all the financial and target price data for a cryptocurrency
 */
public class CryptoData implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id;
    public String name;
    public String symbol;
    public double currentPrice;
    public double expectedPrice; // Keeping for backward compatibility
    public double expectedEntry; // New field for expected entry price
    public double targetPrice3Month;
    public double targetPriceLongTerm;
    public double holdings;
    public double avgBuyPrice;
    public transient String aiAdvice; // AI-generated three-word advice (not saved to file)
    public transient boolean isAiGenerated = false; // Track if advice is from AI or rule-based (not saved to file)
    public transient String aiStatus = "LOADING"; // LOADING, AI_SUCCESS, FALLBACK, ERROR (not saved to file)
    public transient long lastAiUpdate = 0; // Timestamp of last AI update (not saved to file)
    
    // Technical Analysis data (transient - not saved to file)
    public transient TechnicalIndicators technicalIndicators; // Technical analysis data
    public transient String technicalAnalysisStatus = "LOADING"; // LOADING, SUCCESS, ERROR
    public transient long lastTechnicalUpdate = 0; // Timestamp of last technical analysis update
    
    // Constructor with all fields including expected entry
    public CryptoData(String id, String name, String symbol, double currentPrice, double expectedPrice, 
                     double expectedEntry, double targetPrice3Month, double targetPriceLongTerm, 
                     double holdings, double avgBuyPrice) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.expectedPrice = expectedPrice;
        this.expectedEntry = expectedEntry;
        this.targetPrice3Month = targetPrice3Month;
        this.targetPriceLongTerm = targetPriceLongTerm;
        this.holdings = holdings;
        this.avgBuyPrice = avgBuyPrice;
        // Initialize transient AI fields
        initializeAiFields();
    }
    
    // Legacy constructor for backward compatibility
    public CryptoData(String id, String name, String symbol, double currentPrice, double expectedPrice, 
                     double holdings, double avgBuyPrice) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.expectedPrice = expectedPrice;
        this.expectedEntry = expectedPrice * 0.9; // Default to 10% below expected price
        this.targetPrice3Month = expectedPrice;
        this.targetPriceLongTerm = expectedPrice;
        this.holdings = holdings;
        this.avgBuyPrice = avgBuyPrice;
        // Initialize transient AI fields
        initializeAiFields();
    }
    
    // Constructor with target fields but without expected entry
    public CryptoData(String id, String name, String symbol, double currentPrice, double expectedPrice, 
                     double targetPrice3Month, double targetPriceLongTerm, double holdings, double avgBuyPrice) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.expectedPrice = expectedPrice;
        this.expectedEntry = expectedPrice * 0.9; // Default to 10% below expected price
        this.targetPrice3Month = targetPrice3Month;
        this.targetPriceLongTerm = targetPriceLongTerm;
        this.holdings = holdings;
        this.avgBuyPrice = avgBuyPrice;
        // Initialize transient AI fields
        initializeAiFields();
    }
    
    /**
     * Calculate percentage change from expected price
     */
    public double getPercentageChange() {
        if (expectedPrice == 0) return 0;
        return (currentPrice - expectedPrice) / expectedPrice;
    }
    
    /**
     * Get total value of holdings
     */
    public double getTotalValue() {
        return holdings * currentPrice;
    }
    
    /**
     * Calculate profit/loss based on average buy price
     */
    public double getProfitLoss() {
        return holdings * (currentPrice - avgBuyPrice);
    }
    
    /**
     * Calculate profit/loss percentage
     */
    public double getProfitLossPercentage() {
        if (avgBuyPrice <= 0) return 0;
        return (currentPrice - avgBuyPrice) / avgBuyPrice;
    }
    
    /**
     * Get entry opportunity indicator
     * Returns positive if current price is near or below expected entry
     */
    public double getEntryOpportunity() {
        if (expectedEntry == 0) return 0;
        return (expectedEntry - currentPrice) / expectedEntry;
    }
    
    /**
     * Check if current price is a good entry point
     */
    public boolean isGoodEntryPoint() {
        return currentPrice <= expectedEntry * 1.05; // Within 5% of expected entry
    }
    
    /**
     * Get AI-generated three-word advice
     */
    public String getAiAdvice() {
        return aiAdvice != null ? aiAdvice : "Loading...";
    }
    
    /**
     * Set AI-generated advice
     */
    public void setAiAdvice(String advice) {
        this.aiAdvice = advice;
        this.lastAiUpdate = System.currentTimeMillis();
    }
    
    /**
     * Set AI advice from AI API with success status
     */
    public void setAiAdviceFromAI(String advice) {
        this.aiAdvice = advice;
        this.isAiGenerated = true;
        this.aiStatus = "AI_SUCCESS";
        this.lastAiUpdate = System.currentTimeMillis();
    }
    
    /**
     * Set AI advice from fallback with fallback status
     */
    public void setAiAdviceFromFallback(String advice) {
        this.aiAdvice = advice;
        this.isAiGenerated = false;
        this.aiStatus = "FALLBACK";
        this.lastAiUpdate = System.currentTimeMillis();
    }
    
    /**
     * Set AI advice error status
     */
    public void setAiAdviceError() {
        this.aiAdvice = "Error";
        this.isAiGenerated = false;
        this.aiStatus = "ERROR";
        this.lastAiUpdate = System.currentTimeMillis();
    }
    
    /**
     * Set AI advice from cache with AI_CACHE status
     */
    public void setAiAdviceFromCache(String advice) {
        this.aiAdvice = advice;
        this.isAiGenerated = false;
        this.aiStatus = "AI_CACHE";
        this.lastAiUpdate = System.currentTimeMillis();
    }
    
    /**
     * Get AI advice with status indicator
     */
    public String getAiAdviceWithStatus() {
        // Handle null aiStatus for backward compatibility
        if (aiStatus == null) {
            aiStatus = "LOADING";
        }
        
        // Handle null aiAdvice
        if (aiAdvice == null) {
            aiAdvice = "Loading...";
        }
        
        if (aiStatus.equals("LOADING")) {
            return "🔄 Loading...";
        } else if (aiStatus.equals("AI_SUCCESS")) {
            return "🤖 " + aiAdvice;
        } else if (aiStatus.equals("FALLBACK")) {
            return "📊 " + aiAdvice;
        } else if (aiStatus.equals("ERROR")) {
            return "❌ " + aiAdvice;
        } else if (aiStatus.equals("AI_CACHE")) {
            return "💾 " + aiAdvice; // Indicate cached advice
        }
        return aiAdvice;
    }

    /**
     * Initialize AI status fields for backward compatibility
     * Called when loading old data that doesn't have these fields
     */
    public void initializeAiFields() {
        if (aiStatus == null) {
            aiStatus = "LOADING";
        }
        if (aiAdvice == null) {
            aiAdvice = "Loading...";
        }
        if (lastAiUpdate == 0) {
            lastAiUpdate = System.currentTimeMillis();
        }
        
        // Initialize technical analysis fields
        if (technicalAnalysisStatus == null) {
            technicalAnalysisStatus = "LOADING";
        }
        if (lastTechnicalUpdate == 0) {
            lastTechnicalUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Set technical analysis data
     */
    public void setTechnicalIndicators(TechnicalIndicators indicators) {
        this.technicalIndicators = indicators;
        this.technicalAnalysisStatus = "SUCCESS";
        this.lastTechnicalUpdate = System.currentTimeMillis();
    }

    /**
     * Set technical analysis error status
     */
    public void setTechnicalAnalysisError() {
        this.technicalIndicators = null;
        this.technicalAnalysisStatus = "ERROR";
        this.lastTechnicalUpdate = System.currentTimeMillis();
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

    @Override
    public String toString() {
        return String.format("%s (%s) - Current: $%.2f, Holdings: %.4f", 
                           name, symbol, currentPrice, holdings);
    }
}
