import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Service class for getting AI-generated cryptocurrency advice
 * Uses Hugging Face's free inference API
 */
public class AiAdviceService {
    private static final String API_URL = "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium";
    private static final String API_KEY = "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"; // Optional: Add your Hugging Face API key for better rate limits
    
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    
    /**
     * Get AI advice for a cryptocurrency asynchronously
     * @param crypto The cryptocurrency data
     * @return CompletableFuture containing three-word advice
     */
    public static CompletableFuture<String> getAdviceAsync(CryptoData crypto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAdvice(crypto);
            } catch (Exception e) {
                System.err.println("Error getting AI advice for " + crypto.symbol + ": " + e.getMessage());
                return getSimpleAdvice(crypto); // Fallback to simple rule-based advice
            }
        }, executor);
    }
    
    /**
     * Get AI advice for a cryptocurrency
     * @param crypto The cryptocurrency data
     * @return Three-word advice string
     */
    public static String getAdvice(CryptoData crypto) {
        try {
            // Create a prompt for the AI
            String prompt = createPrompt(crypto);
            
            // Make API request
            String response = makeApiRequest(prompt);
            
            // Parse and clean the response
            String advice = parseResponse(response);
            
            // Ensure it's exactly three words
            return formatToThreeWords(advice);
            
        } catch (Exception e) {
            System.err.println("Error getting AI advice: " + e.getMessage());
            return getSimpleAdvice(crypto);
        }
    }
    
    /**
     * Create a prompt for the AI model
     */
    private static String createPrompt(CryptoData crypto) {
        double profitLossPercentage = crypto.getProfitLossPercentage() * 100;
        double entryOpportunity = crypto.getEntryOpportunity() * 100;
        
        return String.format(
            "Cryptocurrency %s (%s): Current price $%.2f, Target $%.2f, Holdings %.4f, " +
            "Profit/Loss %.1f%%, Entry opportunity %.1f%%. " +
            "Give exactly three-word investment advice:",
            crypto.name, crypto.symbol, crypto.currentPrice, crypto.expectedPrice, 
            crypto.holdings, profitLossPercentage, entryOpportunity
        );
    }
    
    /**
     * Make API request to Hugging Face
     */
    private static String makeApiRequest(String prompt) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        if (!API_KEY.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        
        // Create request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("inputs", prompt);
        
        JSONObject parameters = new JSONObject();
        parameters.put("max_length", 50);
        parameters.put("temperature", 0.7);
        parameters.put("do_sample", true);
        requestBody.put("parameters", parameters);
        
        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = connection.getResponseCode();
        InputStream inputStream = (responseCode == 200) ? 
            connection.getInputStream() : connection.getErrorStream();
            
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        if (responseCode != 200) {
            throw new IOException("API request failed with code " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * Parse the API response to extract advice
     */
    private static String parseResponse(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() > 0) {
                JSONObject firstResult = jsonArray.getJSONObject(0);
                if (firstResult.has("generated_text")) {
                    String fullText = firstResult.getString("generated_text");
                    // Extract advice after the prompt
                    String[] parts = fullText.split("Give exactly three-word investment advice:");
                    if (parts.length > 1) {
                        return parts[1].trim();
                    }
                }
            }
            throw new RuntimeException("Invalid response format");
        } catch (Exception e) {
            // If JSON parsing fails, try to extract meaningful words from response
            return extractKeyWords(response);
        }
    }
    
    /**
     * Extract key words from response text
     */
    private static String extractKeyWords(String text) {
        // Simple extraction of investment-related words
        String[] words = text.toLowerCase()
            .replaceAll("[^a-z\\s]", "")
            .split("\\s+");
        
        StringBuilder result = new StringBuilder();
        int wordCount = 0;
        
        for (String word : words) {
            if (wordCount >= 3) break;
            if (isInvestmentWord(word)) {
                if (wordCount > 0) result.append(" ");
                result.append(capitalizeFirst(word));
                wordCount++;
            }
        }
        
        return wordCount == 3 ? result.toString() : "Hold And Wait";
    }
    
    /**
     * Check if a word is investment-related
     */
    private static boolean isInvestmentWord(String word) {
        String[] investmentWords = {
            "buy", "sell", "hold", "wait", "bullish", "bearish", "strong", "weak",
            "good", "bad", "positive", "negative", "pump", "dump", "moon", "crash",
            "rise", "fall", "up", "down", "high", "low", "volatile", "stable",
            "risky", "safe", "accumulate", "distribute", "enter", "exit", "target",
            "support", "resistance", "breakout", "correction", "rally", "dip"
        };
        
        for (String investWord : investmentWords) {
            if (word.equals(investWord)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Format text to exactly three words
     */
    private static String formatToThreeWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Hold And Wait";
        }
        
        String[] words = text.trim().split("\\s+");
        if (words.length >= 3) {
            return capitalizeFirst(words[0]) + " " + 
                   capitalizeFirst(words[1]) + " " + 
                   capitalizeFirst(words[2]);
        } else if (words.length == 2) {
            return capitalizeFirst(words[0]) + " " + 
                   capitalizeFirst(words[1]) + " Now";
        } else if (words.length == 1) {
            return capitalizeFirst(words[0]) + " And Wait";
        }
        
        return "Hold And Wait";
    }
    
    /**
     * Capitalize first letter of a word
     */
    private static String capitalizeFirst(String word) {
        if (word == null || word.isEmpty()) return word;
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
    
    /**
     * Fallback rule-based advice when AI API fails
     */
    private static String getSimpleAdvice(CryptoData crypto) {
        double profitLoss = crypto.getProfitLossPercentage();
        double entryOpportunity = crypto.getEntryOpportunity();
        
        if (profitLoss > 0.20) return "Take Profit";
        else if (profitLoss < -0.20) return "Cut Loss";
        else if (entryOpportunity > 0.1) return "Buy The Dip";
        else if (entryOpportunity < -0.1) return "Wait For Dip";
        else if (crypto.currentPrice > crypto.targetPriceLongTerm) return "Sell High";
        else if (crypto.currentPrice < crypto.expectedEntry) return "Good Entry";
        else return "Hold Position";
    }
    
    /**
     * Shutdown the executor service
     */
    public static void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Generate detailed AI analysis for a cryptocurrency
     * @param crypto The cryptocurrency data
     * @return Detailed analysis string
     */
    public static String getDetailedAnalysis(CryptoData crypto) {
        StringBuilder analysis = new StringBuilder();
        
        // Market Position Analysis
        analysis.append("📊 MARKET POSITION ANALYSIS\n\n");
        
        double profitLoss = crypto.getProfitLossPercentage() * 100;
        double entryOpportunity = crypto.getEntryOpportunity() * 100;
        double currentToTarget = ((crypto.targetPriceLongTerm - crypto.currentPrice) / crypto.currentPrice) * 100;
        
        analysis.append(String.format("Current Price: $%.2f\n", crypto.currentPrice));
        analysis.append(String.format("Your Average Cost: $%.2f\n", crypto.avgBuyPrice));
        analysis.append(String.format("Holdings: %.4f %s\n\n", crypto.holdings, crypto.symbol));
        
        // Performance Analysis
        analysis.append("💰 PERFORMANCE METRICS\n\n");
        if (profitLoss > 0) {
            analysis.append(String.format("✅ Current Profit: +%.2f%%\n", profitLoss));
        } else {
            analysis.append(String.format("❌ Current Loss: %.2f%%\n", profitLoss));
        }
        
        analysis.append(String.format("🎯 Potential to Long Target: %.2f%%\n", currentToTarget));
        analysis.append(String.format("📈 Entry Opportunity: %.2f%%\n\n", entryOpportunity));
        
        // Risk Assessment
        analysis.append("⚠️ RISK ASSESSMENT\n\n");
        String riskLevel = getRiskLevel(crypto);
        analysis.append(String.format("Risk Level: %s\n", riskLevel));
        analysis.append(String.format("%s\n\n", getRiskExplanation(crypto)));
        
        // Strategic Recommendations
        analysis.append("🎯 STRATEGIC RECOMMENDATIONS\n\n");
        analysis.append(getStrategicRecommendations(crypto));
        
        // Technical Indicators
        analysis.append("\n📈 TECHNICAL SIGNALS\n\n");
        analysis.append(getTechnicalSignals(crypto));
        
        return analysis.toString();
    }
    
    /**
     * Get risk level for a cryptocurrency
     */
    private static String getRiskLevel(CryptoData crypto) {
        double profitLoss = Math.abs(crypto.getProfitLossPercentage() * 100);
        double priceDeviation = Math.abs(crypto.currentPrice - crypto.expectedPrice) / crypto.expectedPrice * 100;
        
        if (profitLoss > 30 || priceDeviation > 50) return "🔴 HIGH RISK";
        else if (profitLoss > 15 || priceDeviation > 25) return "🟡 MEDIUM RISK";
        else return "🟢 LOW RISK";
    }
    
    /**
     * Get risk explanation
     */
    private static String getRiskExplanation(CryptoData crypto) {
        double profitLoss = crypto.getProfitLossPercentage() * 100;
        
        if (profitLoss > 30) {
            return "High profits suggest potential for profit-taking. Consider securing gains.";
        } else if (profitLoss < -20) {
            return "Significant losses detected. Evaluate position size and risk tolerance.";
        } else if (crypto.currentPrice > crypto.targetPriceLongTerm * 1.2) {
            return "Price exceeds long-term targets. Monitor for potential corrections.";
        } else {
            return "Position appears within acceptable risk parameters.";
        }
    }
    
    /**
     * Get strategic recommendations
     */
    private static String getStrategicRecommendations(CryptoData crypto) {
        StringBuilder recommendations = new StringBuilder();
        
        double profitLoss = crypto.getProfitLossPercentage();
        double entryOpportunity = crypto.getEntryOpportunity();
        
        // Primary recommendation
        if (profitLoss > 0.25) {
            recommendations.append("🎯 PRIMARY: Consider taking partial profits (25-50%)\n");
            recommendations.append("   Reason: Substantial gains achieved (+25%)\n\n");
        } else if (profitLoss < -0.2) {
            recommendations.append("⚠️ PRIMARY: Evaluate position - consider stop-loss\n");
            recommendations.append("   Reason: Significant unrealized losses (-20%)\n\n");
        } else if (entryOpportunity > 0.1) {
            recommendations.append("💰 PRIMARY: Consider accumulating more\n");
            recommendations.append("   Reason: Price below ideal entry point\n\n");
        } else {
            recommendations.append("⏳ PRIMARY: Hold current position\n");
            recommendations.append("   Reason: Price within expected range\n\n");
        }
        
        // Secondary recommendations
        recommendations.append("📋 SECONDARY ACTIONS:\n");
        
        if (crypto.currentPrice < crypto.expectedEntry) {
            recommendations.append("• Dollar-cost average if you have additional capital\n");
        }
        
        if (crypto.currentPrice > crypto.targetPrice3Month) {
            recommendations.append("• Set up alerts for potential pullbacks\n");
        }
        
        if (crypto.holdings > 0) {
            recommendations.append("• Review portfolio allocation percentage\n");
        }
        
        recommendations.append("• Monitor volume and market sentiment\n");
        
        return recommendations.toString();
    }
    
    /**
     * Get technical signals
     */
    private static String getTechnicalSignals(CryptoData crypto) {
        StringBuilder signals = new StringBuilder();
        
        double currentPrice = crypto.currentPrice;
        double entryTarget = crypto.expectedEntry;
        double shortTarget = crypto.targetPrice3Month;
        double longTarget = crypto.targetPriceLongTerm;
        
        // Support and Resistance
        signals.append(String.format("🔹 Support Level: $%.2f (Entry Target)\n", entryTarget));
        signals.append(String.format("🔹 Resistance Level: $%.2f (3M Target)\n", shortTarget));
        signals.append(String.format("🔹 Major Target: $%.2f (Long-term)\n\n", longTarget));
        
        // Price Action
        if (currentPrice < entryTarget) {
            signals.append("📉 Price Action: Below support - potential buying opportunity\n");
        } else if (currentPrice > shortTarget) {
            signals.append("📈 Price Action: Above resistance - potential profit-taking zone\n");
        } else {
            signals.append("📊 Price Action: Trading within expected range\n");
        }
        
        // Momentum indicators (simulated)
        double momentum = (currentPrice - entryTarget) / entryTarget;
        if (momentum > 0.1) {
            signals.append("🚀 Momentum: Strong upward trend\n");
        } else if (momentum < -0.1) {
            signals.append("📉 Momentum: Downward pressure\n");
        } else {
            signals.append("⚖️ Momentum: Neutral/Consolidating\n");
        }
        
        return signals.toString();
    }
}
