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
     * Get entry status emoji
     */
    public String getEntryStatusEmoji() {
        double opportunity = getEntryOpportunity();
        if (opportunity > 0.1) return "🔥"; // Great entry (10%+ below expected)
        else if (opportunity > 0.05) return "💚"; // Good entry (5-10% below expected)
        else if (opportunity > 0) return "✅"; // Fair entry (near expected)
        else if (opportunity > -0.05) return "⚠️"; // Slightly above expected
        else return "🔴"; // Well above expected entry
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Current: $%.2f, Holdings: %.4f", 
                           name, symbol, currentPrice, holdings);
    }
}
