import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;

/**
 * Data structure to hold AI-generated portfolio rebalancing recommendations
 * Contains allocation suggestions, rationale, and formatting for display
 */
public class PortfolioRebalanceRecommendation {
    
    // Recommendation data
    private boolean isValid;
    private String analysisText;
    private double newMoneyAmount;
    private List<AllocationRecommendation> allocations;
    private String aiRationale;
    private String riskAssessment;
    private String marketOutlook;
    private long generatedTime;
    
    // Formatters
    private static final DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private static final DecimalFormat amountFormat = new DecimalFormat("#,##0.0000");
    
    public PortfolioRebalanceRecommendation() {
        this.allocations = new ArrayList<>();
        this.generatedTime = System.currentTimeMillis();
        this.isValid = false;
    }
    
    /**
     * Individual asset allocation recommendation
     */
    public static class AllocationRecommendation {
        private String symbol;
        private String action; // "BUY", "SELL", "HOLD"
        private double recommendedAmount;
        private double currentValue;
        private double targetPercentage;
        private String reasoning;
        
        public AllocationRecommendation(String symbol, String action, double recommendedAmount, 
                                      double currentValue, double targetPercentage, String reasoning) {
            this.symbol = symbol;
            this.action = action;
            this.recommendedAmount = recommendedAmount;
            this.currentValue = currentValue;
            this.targetPercentage = targetPercentage;
            this.reasoning = reasoning;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public String getAction() { return action; }
        public double getRecommendedAmount() { return recommendedAmount; }
        public double getCurrentValue() { return currentValue; }
        public double getTargetPercentage() { return targetPercentage; }
        public String getReasoning() { return reasoning; }
        
        // Setters
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public void setAction(String action) { this.action = action; }
        public void setRecommendedAmount(double recommendedAmount) { this.recommendedAmount = recommendedAmount; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
        public void setTargetPercentage(double targetPercentage) { this.targetPercentage = targetPercentage; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }
    
    /**
     * Set the recommendation as valid with AI analysis
     */
    public void setValidRecommendation(String analysisText, double newMoneyAmount, String aiRationale, 
                                     String riskAssessment, String marketOutlook) {
        this.isValid = true;
        this.analysisText = analysisText;
        this.newMoneyAmount = newMoneyAmount;
        this.aiRationale = aiRationale;
        this.riskAssessment = riskAssessment;
        this.marketOutlook = marketOutlook;
    }
    
    /**
     * Add an allocation recommendation
     */
    public void addAllocation(String symbol, String action, double recommendedAmount, 
                            double currentValue, double targetPercentage, String reasoning) {
        allocations.add(new AllocationRecommendation(symbol, action, recommendedAmount, 
                                                   currentValue, targetPercentage, reasoning));
    }
    
    /**
     * Get formatted recommendation text for display
     */
    public String getFormattedRecommendation() {
        if (!isValid) {
            return "❌ Invalid recommendation data";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("🤖 AI PORTFOLIO REBALANCING RECOMMENDATION\n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Investment amount
        sb.append("💰 Additional Investment: ").append(priceFormat.format(newMoneyAmount)).append("\n");
        sb.append("📅 Generated: ").append(new java.util.Date(generatedTime).toString()).append("\n\n");
        
        // Market outlook
        if (marketOutlook != null && !marketOutlook.trim().isEmpty()) {
            sb.append("🌍 MARKET OUTLOOK:\n");
            sb.append(marketOutlook).append("\n\n");
        }
        
        // Risk assessment
        if (riskAssessment != null && !riskAssessment.trim().isEmpty()) {
            sb.append("⚠️ RISK ASSESSMENT:\n");
            sb.append(riskAssessment).append("\n\n");
        }
        
        // Allocation recommendations
        if (!allocations.isEmpty()) {
            sb.append("📊 RECOMMENDED ALLOCATIONS:\n\n");
            
            for (AllocationRecommendation allocation : allocations) {
                String actionEmoji = getActionEmoji(allocation.getAction());
                sb.append(String.format("%s %s (%s)\n", 
                    actionEmoji, allocation.getSymbol(), allocation.getAction()));
                
                if (allocation.getRecommendedAmount() > 0) {
                    sb.append(String.format("   💵 Amount: %s\n", priceFormat.format(allocation.getRecommendedAmount())));
                }
                
                sb.append(String.format("   🎯 Target Allocation: %s\n", percentFormat.format(allocation.getTargetPercentage())));
                
                if (allocation.getCurrentValue() > 0) {
                    sb.append(String.format("   📈 Current Value: %s\n", priceFormat.format(allocation.getCurrentValue())));
                }
                
                if (allocation.getReasoning() != null && !allocation.getReasoning().trim().isEmpty()) {
                    sb.append(String.format("   💡 Rationale: %s\n", allocation.getReasoning()));
                }
                sb.append("\n");
            }
        }
        
        // AI rationale
        if (aiRationale != null && !aiRationale.trim().isEmpty()) {
            sb.append("🧠 AI ANALYSIS:\n");
            sb.append(aiRationale).append("\n\n");
        }
        
        // Disclaimer
        sb.append("⚠️ IMPORTANT DISCLAIMER:\n");
        sb.append("This recommendation is generated by AI for informational purposes only.\n");
        sb.append("Always conduct your own research and consider your risk tolerance.\n");
        sb.append("Cryptocurrency investments carry significant risk of loss.\n");
        sb.append("Past performance does not guarantee future results.\n");
        
        return sb.toString();
    }
    
    private String getActionEmoji(String action) {
        switch (action.toUpperCase()) {
            case "BUY":
                return "🟢";
            case "SELL":
                return "🔴";
            case "HOLD":
                return "🟡";
            default:
                return "⚪";
        }
    }
    
    /**
     * Get a summary of the recommendation for quick display
     */
    public String getSummary() {
        if (!isValid) {
            return "Invalid recommendation";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("AI Recommendation for %s additional investment:\n", 
            priceFormat.format(newMoneyAmount)));
        
        int buyCount = 0, sellCount = 0, holdCount = 0;
        for (AllocationRecommendation allocation : allocations) {
            switch (allocation.getAction().toUpperCase()) {
                case "BUY": buyCount++; break;
                case "SELL": sellCount++; break;
                case "HOLD": holdCount++; break;
            }
        }
        
        summary.append(String.format("• %d assets to BUY, %d to SELL, %d to HOLD", 
            buyCount, sellCount, holdCount));
        
        return summary.toString();
    }
    
    // Getters and setters
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { this.isValid = valid; }
    
    public String getAnalysisText() { return analysisText; }
    public void setAnalysisText(String analysisText) { this.analysisText = analysisText; }
    
    public double getNewMoneyAmount() { return newMoneyAmount; }
    public void setNewMoneyAmount(double newMoneyAmount) { this.newMoneyAmount = newMoneyAmount; }
    
    public List<AllocationRecommendation> getAllocations() { return allocations; }
    public void setAllocations(List<AllocationRecommendation> allocations) { this.allocations = allocations; }
    
    public String getAiRationale() { return aiRationale; }
    public void setAiRationale(String aiRationale) { this.aiRationale = aiRationale; }
    
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    
    public String getMarketOutlook() { return marketOutlook; }
    public void setMarketOutlook(String marketOutlook) { this.marketOutlook = marketOutlook; }
    
    public long getGeneratedTime() { return generatedTime; }
    public void setGeneratedTime(long generatedTime) { this.generatedTime = generatedTime; }
}
