import java.util.List;
import java.util.stream.Collectors;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Service class for generating AI-powered portfolio rebalancing recommendations
 * Integrates with the existing AI service to provide portfolio-level analysis and optimization
 */
public class PortfolioRebalanceService {
    
    // Formatters
    private static final DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat percentFormat = new DecimalFormat("0.00%");
    private static final DecimalFormat amountFormat = new DecimalFormat("#,##0.0000");
    
    /**
     * Get AI-powered rebalancing recommendation for the portfolio
     * @param cryptoList Current portfolio holdings
     * @param newMoneyAmount Additional money to invest
     * @return PortfolioRebalanceRecommendation with AI analysis
     */
    public static PortfolioRebalanceRecommendation getRebalanceRecommendation(List<CryptoData> cryptoList, double newMoneyAmount) {
        LoggerUtil.info(PortfolioRebalanceService.class, 
            String.format("Generating AI rebalancing recommendation for %s additional investment", 
                priceFormat.format(newMoneyAmount)));
        
        PortfolioRebalanceRecommendation recommendation = new PortfolioRebalanceRecommendation();
        
        try {
            // Calculate current portfolio metrics
            PortfolioMetrics currentMetrics = calculatePortfolioMetrics(cryptoList);
            
            if (currentMetrics.totalValue <= 0) {
                LoggerUtil.warning(PortfolioRebalanceService.class, "Portfolio has no value, cannot generate recommendation");
                return recommendation; // Invalid recommendation
            }
            
            // Create comprehensive AI prompt for portfolio analysis
            String aiPrompt = createPortfolioAnalysisPrompt(cryptoList, newMoneyAmount, currentMetrics);
            
            // Get AI analysis
            String aiResponse = getAiPortfolioAnalysis(aiPrompt);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                // Parse AI response and create recommendation
                parseAiRecommendation(recommendation, aiResponse, cryptoList, newMoneyAmount, currentMetrics);
                
                LoggerUtil.info(PortfolioRebalanceService.class, "AI portfolio recommendation generated successfully");
            } else {
                // Fallback to rule-based recommendation
                LoggerUtil.warning(PortfolioRebalanceService.class, "AI analysis unavailable, using rule-based recommendation");
                generateRuleBasedRecommendation(recommendation, cryptoList, newMoneyAmount, currentMetrics);
            }
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioRebalanceService.class, "Error generating portfolio recommendation", e);
            // Return fallback recommendation
            generateRuleBasedRecommendation(recommendation, cryptoList, newMoneyAmount, calculatePortfolioMetrics(cryptoList));
        }
        
        return recommendation;
    }
    
    /**
     * Calculate portfolio metrics for analysis
     */
    private static PortfolioMetrics calculatePortfolioMetrics(List<CryptoData> cryptoList) {
        PortfolioMetrics metrics = new PortfolioMetrics();
        
        for (CryptoData crypto : cryptoList) {
            double value = crypto.getTotalValue();
            double profitLoss = crypto.getProfitLoss();
            
            metrics.totalValue += value;
            metrics.totalProfitLoss += profitLoss;
            
            if (value > 0) {
                metrics.holdingsCount++;
                
                // Track performance
                double profitLossPercentage = crypto.getProfitLossPercentage();
                if (profitLossPercentage > 0.1) metrics.strongPerformers++;
                else if (profitLossPercentage < -0.1) metrics.weakPerformers++;
                
                // Track entry opportunities
                if (crypto.isGoodEntryPoint()) metrics.goodEntryOpportunities++;
            }
        }
        
        // Calculate diversification score (simplified)
        if (metrics.holdingsCount > 0) {
            double maxAllocation = 0;
            for (CryptoData crypto : cryptoList) {
                if (metrics.totalValue > 0) {
                    double allocation = crypto.getTotalValue() / metrics.totalValue;
                    if (allocation > maxAllocation) {
                        maxAllocation = allocation;
                    }
                }
            }
            // Diversification score: lower max allocation = better diversification
            metrics.diversificationScore = 1.0 - maxAllocation;
        }
        
        return metrics;
    }
    
    /**
     * Create comprehensive AI prompt for portfolio analysis
     */
    private static String createPortfolioAnalysisPrompt(List<CryptoData> cryptoList, double newMoney, PortfolioMetrics metrics) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("As a professional cryptocurrency portfolio manager, analyze this portfolio and provide detailed rebalancing recommendations.\n\n");
        
        // Portfolio overview
        prompt.append("CURRENT PORTFOLIO ANALYSIS:\n");
        prompt.append(String.format("• Total Portfolio Value: %s\n", priceFormat.format(metrics.totalValue)));
        prompt.append(String.format("• Total P&L: %s (%.2f%%)\n", 
            priceFormat.format(metrics.totalProfitLoss), 
            metrics.totalValue > 0 ? (metrics.totalProfitLoss / metrics.totalValue) * 100 : 0));
        prompt.append(String.format("• Number of Holdings: %d\n", metrics.holdingsCount));
        prompt.append(String.format("• Diversification Score: %.2f (0-1, higher is better)\n", metrics.diversificationScore));
        prompt.append(String.format("• Strong Performers: %d assets\n", metrics.strongPerformers));
        prompt.append(String.format("• Weak Performers: %d assets\n", metrics.weakPerformers));
        prompt.append(String.format("• Good Entry Opportunities: %d assets\n\n", metrics.goodEntryOpportunities));
        
        // Additional investment
        prompt.append(String.format("NEW INVESTMENT AMOUNT: %s\n\n", priceFormat.format(newMoney)));
        
        // Individual holdings analysis
        prompt.append("INDIVIDUAL HOLDINGS BREAKDOWN:\n");
        
        // Sort by total value for analysis
        List<CryptoData> sortedCryptos = cryptoList.stream()
            .sorted(Comparator.comparingDouble(CryptoData::getTotalValue).reversed())
            .collect(Collectors.toList());
        
        for (CryptoData crypto : sortedCryptos) {
            if (crypto.getTotalValue() > 0) {
                double allocation = metrics.totalValue > 0 ? (crypto.getTotalValue() / metrics.totalValue) * 100 : 0;
                double profitLossPercentage = crypto.getProfitLossPercentage() * 100;
                double entryOpportunity = crypto.getEntryOpportunity() * 100;
                
                prompt.append(String.format("• %s (%s):\n", crypto.symbol.toUpperCase(), crypto.name));
                prompt.append(String.format("  - Current Value: %s (%.2f%% of portfolio)\n", 
                    priceFormat.format(crypto.getTotalValue()), allocation));
                prompt.append(String.format("  - Holdings: %s at avg cost %s\n", 
                    amountFormat.format(crypto.holdings), priceFormat.format(crypto.avgBuyPrice)));
                prompt.append(String.format("  - Current Price: %s (P&L: %.2f%%)\n", 
                    priceFormat.format(crypto.currentPrice), profitLossPercentage));
                prompt.append(String.format("  - Entry Target: %s (opportunity: %.2f%%)\n", 
                    priceFormat.format(crypto.expectedEntry), entryOpportunity));
                prompt.append(String.format("  - 3M Target: %s, Long Target: %s\n", 
                    priceFormat.format(crypto.targetPrice3Month), priceFormat.format(crypto.targetPriceLongTerm)));
                prompt.append("\n");
            }
        }
        
        // Request specific analysis
        prompt.append("REQUIRED ANALYSIS:\n");
        prompt.append("Please provide a comprehensive rebalancing recommendation with:\n\n");
        
        prompt.append("1. MARKET_OUTLOOK: Current market conditions and trends (2-3 sentences)\n\n");
        
        prompt.append("2. RISK_ASSESSMENT: Portfolio risk analysis and key risk factors (2-3 sentences)\n\n");
        
        prompt.append("3. ALLOCATION_RECOMMENDATIONS: For each asset, specify:\n");
        prompt.append("   - Action: BUY/SELL/HOLD\n");
        prompt.append("   - Amount: Specific dollar amount to allocate from new money\n");
        prompt.append("   - Target_Percentage: Recommended portfolio percentage\n");
        prompt.append("   - Reasoning: Why this allocation (1 sentence)\n\n");
        
        prompt.append("4. AI_RATIONALE: Overall strategy and reasoning for the rebalancing approach (3-4 sentences)\n\n");
        
        prompt.append("Format your response clearly with sections marked by the headers above. ");
        prompt.append("Focus on optimizing risk-adjusted returns and maintaining proper diversification. ");
        prompt.append("Consider current market conditions, asset performance, and growth potential.");
        
        return prompt.toString();
    }
    
    /**
     * Get AI analysis for portfolio rebalancing
     */
    private static String getAiPortfolioAnalysis(String prompt) {
        try {
            LoggerUtil.debug(PortfolioRebalanceService.class, "Requesting AI portfolio analysis");
            
            // Use the existing AI service for detailed analysis
            // We'll create a temporary crypto data object to use the existing service
            CryptoData tempCrypto = new CryptoData("portfolio", "Portfolio Analysis", "PORTFOLIO", 
                0, 0, 0, 0, 0, 0, 0);
            
            // Use the detailed analysis method but with our custom prompt
            String response = getCustomAiResponse(prompt);
            
            if (response != null && response.length() > 100) {
                LoggerUtil.info(PortfolioRebalanceService.class, "Received AI portfolio analysis response");
                return response;
            }
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioRebalanceService.class, "Error getting AI portfolio analysis", e);
        }
        
        return null;
    }
    
    /**
     * Get custom AI response using existing infrastructure
     */
    private static String getCustomAiResponse(String prompt) {
        try {
            // Use the AiAdviceService infrastructure but with custom prompt
            // This is a simplified version - in a real implementation, you might want to
            // create a dedicated method in AiAdviceService for portfolio analysis
            
            // For now, we'll use a rule-based approach as fallback
            // In a production environment, you would integrate this with the AI service
            LoggerUtil.warning(PortfolioRebalanceService.class, "Using rule-based analysis as AI integration placeholder");
            
            return null; // This will trigger rule-based analysis
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioRebalanceService.class, "Error in custom AI response", e);
            return null;
        }
    }
    
    /**
     * Parse AI response and populate recommendation
     */
    private static void parseAiRecommendation(PortfolioRebalanceRecommendation recommendation, 
                                            String aiResponse, List<CryptoData> cryptoList, 
                                            double newMoney, PortfolioMetrics metrics) {
        try {
            // Parse the AI response sections
            String marketOutlook = extractSection(aiResponse, "MARKET_OUTLOOK");
            String riskAssessment = extractSection(aiResponse, "RISK_ASSESSMENT");
            String aiRationale = extractSection(aiResponse, "AI_RATIONALE");
            
            // Set basic recommendation data
            recommendation.setValidRecommendation(aiResponse, newMoney, aiRationale, riskAssessment, marketOutlook);
            
            // Parse allocation recommendations
            String allocationsSection = extractSection(aiResponse, "ALLOCATION_RECOMMENDATIONS");
            if (allocationsSection != null) {
                parseAllocationRecommendations(recommendation, allocationsSection, cryptoList, newMoney);
            }
            
            LoggerUtil.info(PortfolioRebalanceService.class, "AI recommendation parsed successfully");
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioRebalanceService.class, "Error parsing AI recommendation", e);
            // Fallback to rule-based
            generateRuleBasedRecommendation(recommendation, cryptoList, newMoney, metrics);
        }
    }
    
    /**
     * Extract section from AI response
     */
    private static String extractSection(String response, String sectionHeader) {
        try {
            int startIndex = response.indexOf(sectionHeader + ":");
            if (startIndex == -1) {
                return null;
            }
            
            startIndex += sectionHeader.length() + 1;
            
            // Find next section or end of response
            int endIndex = response.length();
            String[] nextSections = {"MARKET_OUTLOOK:", "RISK_ASSESSMENT:", "ALLOCATION_RECOMMENDATIONS:", "AI_RATIONALE:"};
            
            for (String nextSection : nextSections) {
                if (!nextSection.equals(sectionHeader + ":")) {
                    int nextIndex = response.indexOf(nextSection, startIndex);
                    if (nextIndex != -1 && nextIndex < endIndex) {
                        endIndex = nextIndex;
                    }
                }
            }
            
            return response.substring(startIndex, endIndex).trim();
            
        } catch (Exception e) {
            LoggerUtil.warning(PortfolioRebalanceService.class, "Error extracting section: " + sectionHeader);
            return null;
        }
    }
    
    /**
     * Parse allocation recommendations from AI response
     */
    private static void parseAllocationRecommendations(PortfolioRebalanceRecommendation recommendation, 
                                                     String allocationsText, List<CryptoData> cryptoList, 
                                                     double newMoney) {
        // This is a simplified parser - in production, you'd want more robust parsing
        for (CryptoData crypto : cryptoList) {
            if (crypto.getTotalValue() > 0) {
                // For now, generate rule-based allocations
                String action = determineAction(crypto);
                double recommendedAmount = calculateRecommendedAmount(crypto, newMoney, cryptoList);
                double targetPercentage = calculateTargetPercentage(crypto, cryptoList);
                String reasoning = generateReasoning(crypto, action);
                
                recommendation.addAllocation(crypto.symbol, action, recommendedAmount, 
                                           crypto.getTotalValue(), targetPercentage, reasoning);
            }
        }
    }
    
    /**
     * Generate rule-based recommendation when AI is unavailable
     */
    private static void generateRuleBasedRecommendation(PortfolioRebalanceRecommendation recommendation, 
                                                       List<CryptoData> cryptoList, double newMoney, 
                                                       PortfolioMetrics metrics) {
        LoggerUtil.info(PortfolioRebalanceService.class, "Generating rule-based portfolio recommendation");
        
        // Set basic data
        String marketOutlook = "Market analysis based on current portfolio performance and price targets. " +
                              "Diversification and risk management remain key priorities.";
        
        String riskAssessment = String.format("Portfolio shows %.2f%% total return with %d holdings. " +
                                             "Diversification score: %.2f. Consider rebalancing for optimal risk distribution.",
                                             metrics.totalValue > 0 ? (metrics.totalProfitLoss / metrics.totalValue) * 100 : 0,
                                             metrics.holdingsCount, metrics.diversificationScore);
        
        String aiRationale = "Rule-based analysis focuses on balancing growth opportunities with risk management. " +
                           "Recommendations prioritize undervalued assets with good entry points while maintaining portfolio diversification. " +
                           "Strong performers may be partially rebalanced to capture profits and reduce concentration risk.";
        
        recommendation.setValidRecommendation("Rule-based portfolio analysis", newMoney, aiRationale, riskAssessment, marketOutlook);
        
        // Generate allocations for each holding
        for (CryptoData crypto : cryptoList) {
            if (crypto.getTotalValue() > 0) {
                String action = determineAction(crypto);
                double recommendedAmount = calculateRecommendedAmount(crypto, newMoney, cryptoList);
                double targetPercentage = calculateTargetPercentage(crypto, cryptoList);
                String reasoning = generateReasoning(crypto, action);
                
                recommendation.addAllocation(crypto.symbol, action, recommendedAmount, 
                                           crypto.getTotalValue(), targetPercentage, reasoning);
            }
        }
        
        LoggerUtil.info(PortfolioRebalanceService.class, "Rule-based recommendation generated successfully");
    }
    
    /**
     * Determine action for a cryptocurrency based on its metrics
     */
    private static String determineAction(CryptoData crypto) {
        double profitLoss = crypto.getProfitLossPercentage();
        double entryOpportunity = crypto.getEntryOpportunity();
        
        // Strong profit - consider taking some profits
        if (profitLoss > 0.3) return "SELL";
        
        // Good entry opportunity
        if (entryOpportunity > 0.1) return "BUY";
        
        // Moderate profit or loss
        if (profitLoss > -0.1 && profitLoss < 0.2) return "HOLD";
        
        // Significant loss but not at entry point
        if (profitLoss < -0.2 && entryOpportunity < 0.05) return "HOLD";
        
        // Default to buy if below entry target
        if (crypto.currentPrice < crypto.expectedEntry) return "BUY";
        
        return "HOLD";
    }
    
    /**
     * Calculate recommended amount for new money allocation
     */
    private static double calculateRecommendedAmount(CryptoData crypto, double newMoney, List<CryptoData> cryptoList) {
        double totalValue = cryptoList.stream().mapToDouble(CryptoData::getTotalValue).sum();
        double currentAllocation = totalValue > 0 ? crypto.getTotalValue() / totalValue : 0;
        
        // Base allocation on current performance and opportunity
        double targetAllocation = 0.1; // Default 10%
        
        // Adjust based on performance and opportunity
        if (crypto.isGoodEntryPoint()) {
            targetAllocation = Math.min(0.25, targetAllocation + 0.1); // Up to 25% for good opportunities
        }
        
        if (crypto.getProfitLossPercentage() > 0.2) {
            targetAllocation = Math.max(0.05, targetAllocation - 0.05); // Reduce allocation for high performers
        }
        
        // Calculate amount needed to reach target
        double targetValue = (totalValue + newMoney) * targetAllocation;
        double neededAmount = Math.max(0, targetValue - crypto.getTotalValue());
        
        return Math.min(neededAmount, newMoney * 0.3); // Max 30% of new money per asset
    }
    
    /**
     * Calculate target percentage for an asset
     */
    private static double calculateTargetPercentage(CryptoData crypto, List<CryptoData> cryptoList) {
        double basePercentage = 1.0 / Math.max(1, cryptoList.size()); // Equal weight base
        
        // Adjust based on opportunity and performance
        if (crypto.isGoodEntryPoint()) {
            basePercentage *= 1.5; // Increase for good opportunities
        }
        
        if (crypto.getProfitLossPercentage() > 0.3) {
            basePercentage *= 0.7; // Reduce for high performers
        }
        
        return Math.min(0.4, Math.max(0.05, basePercentage)); // Between 5% and 40%
    }
    
    /**
     * Generate reasoning for the recommendation
     */
    private static String generateReasoning(CryptoData crypto, String action) {
        double profitLoss = crypto.getProfitLossPercentage();
        double entryOpportunity = crypto.getEntryOpportunity();
        
        switch (action) {
            case "BUY":
                if (entryOpportunity > 0.1) {
                    return "Good entry opportunity with price below target entry level";
                } else {
                    return "Strategic accumulation for long-term growth potential";
                }
            case "SELL":
                return "Strong performance suggests profit-taking opportunity";
            case "HOLD":
            default:
                if (Math.abs(profitLoss) < 0.1) {
                    return "Position performing as expected, maintaining current allocation";
                } else if (profitLoss < -0.1) {
                    return "Temporary underperformance, holding for recovery potential";
                } else {
                    return "Moderate gains, maintaining position for continued growth";
                }
        }
    }
    
    /**
     * Apply the recommendation to the portfolio
     */
    public static boolean applyRecommendation(PortfolioDataManager dataManager, PortfolioRebalanceRecommendation recommendation) {
        if (!recommendation.isValid() || dataManager == null) {
            LoggerUtil.warning(PortfolioRebalanceService.class, "Cannot apply invalid recommendation or null data manager");
            return false;
        }
        
        try {
            LoggerUtil.info(PortfolioRebalanceService.class, "Applying AI portfolio recommendation");
            
            List<CryptoData> cryptoList = dataManager.getCryptoList();
            
            // Apply allocation recommendations
            for (PortfolioRebalanceRecommendation.AllocationRecommendation allocation : recommendation.getAllocations()) {
                for (CryptoData crypto : cryptoList) {
                    if (crypto.symbol.equalsIgnoreCase(allocation.getSymbol())) {
                        // Update target prices based on recommendation
                        // This is a simplified implementation - you might want more sophisticated updates
                        
                        double targetPercentage = allocation.getTargetPercentage();
                        
                        // Adjust long-term targets based on recommended allocation
                        if (targetPercentage > 0.2) { // High allocation
                            crypto.targetPriceLongTerm = crypto.currentPrice * 1.5; // Increase long-term target
                        } else if (targetPercentage < 0.1) { // Low allocation
                            crypto.targetPriceLongTerm = crypto.currentPrice * 1.2; // Conservative target
                        }
                        
                        LoggerUtil.debug(PortfolioRebalanceService.class, 
                            String.format("Applied recommendation for %s: target allocation %.2f%%", 
                                crypto.symbol, targetPercentage * 100));
                        break;
                    }
                }
            }
            
            // Save the updated portfolio
            dataManager.savePortfolioData();
            
            LoggerUtil.info(PortfolioRebalanceService.class, "Portfolio recommendation applied successfully");
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioRebalanceService.class, "Error applying portfolio recommendation", e);
            return false;
        }
    }
    
    /**
     * Portfolio metrics helper class
     */
    private static class PortfolioMetrics {
        double totalValue = 0;
        double totalProfitLoss = 0;
        int holdingsCount = 0;
        int strongPerformers = 0;
        int weakPerformers = 0;
        int goodEntryOpportunities = 0;
        double diversificationScore = 0;
    }
}
