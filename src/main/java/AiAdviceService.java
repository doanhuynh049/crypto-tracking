import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONArray;
import okhttp3.*;

/**
 * Service class for getting AI-generated cryptocurrency advice
 * Uses Google Gemini AI API with OkHttp client
 */
public class AiAdviceService {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY = "AIzaSyBdk8l5xWHr_uQTT0TansUN6ZIpguh6QKM"; // Your Google AI API key
    private static final boolean USE_AI_API = true; // Enabled to use AI API
    
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // Reduced to 1 thread for sequential processing
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    
    /**
     * Get AI advice for a cryptocurrency asynchronously
     * @param crypto The cryptocurrency data
     * @return CompletableFuture containing three-word advice
     */
    public static CompletableFuture<String> getAdviceAsync(CryptoData crypto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String advice = getAdvice(crypto);
                if (USE_AI_API) {
                    crypto.setAiAdviceFromAI(advice);
                } else {
                    crypto.setAiAdviceFromFallback(advice);
                }
                return advice;
            } catch (Exception e) {
                System.err.println("Error getting AI advice for " + crypto.symbol + ": " + e.getMessage());
                String fallbackAdvice = getSimpleAdvice(crypto);
                crypto.setAiAdviceFromFallback(fallbackAdvice);
                return fallbackAdvice;
            }
        }, executor);
    }
    
    /**
     * Get AI advice for a cryptocurrency
     * @param crypto The cryptocurrency data
     * @return Three-word advice string
     */
    public static String getAdvice(CryptoData crypto) {
        // Use rule-based advice as primary method to avoid API issues
        if (!USE_AI_API) {
            return getSimpleAdvice(crypto);
        }
        
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
            System.err.println("AI API unavailable for " + crypto.symbol + ", using rule-based advice: " + e.getMessage());
            return getSimpleAdvice(crypto);
        }
    }
    
    /**
     * Create a prompt for the AI model
     */
    private static String createPrompt(CryptoData crypto) {
        double profitLossPercentage = crypto.getProfitLossPercentage() * 100;
        double entryOpportunity = crypto.getEntryOpportunity() * 100;
        
        // Create a simple prompt for Gemini
        return String.format(
            "You are a cryptocurrency investment advisor. Analyze %s cryptocurrency: " +
            "Current price is $%.2f, target price is $%.2f, holdings are %.4f coins, " +
            "current profit/loss is %.1f%%, entry opportunity is %.1f%%. " +
            "Provide exactly 3 words of investment advice (like 'Buy The Dip' or 'Hold Position'):",
            crypto.symbol.toUpperCase(), crypto.currentPrice, crypto.expectedPrice, 
            crypto.holdings, profitLossPercentage, entryOpportunity
        );
    }
    
    /**
     * Make API request to Google Gemini using OkHttp
     */
    private static String makeApiRequest(String prompt) throws IOException {
        System.out.println("Making API request to: " + API_URL);
        
        // Create request body for Gemini API
        JSONObject contents = new JSONObject();
        JSONArray partsArray = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        partsArray.put(textPart);
        contents.put("parts", partsArray);
        
        JSONArray contentsArray = new JSONArray();
        contentsArray.put(contents);
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contentsArray);
        
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 50);
        generationConfig.put("topP", 0.9);
        requestBody.put("generationConfig", generationConfig);
        
        System.out.println("Request body: " + requestBody.toString());
        
        // Create request
        RequestBody body = RequestBody.create(
            requestBody.toString(), 
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(API_URL + "?key=" + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "CryptoPortfolio/1.0")
            .post(body)
            .build();
        
        // Execute request
        try (Response response = client.newCall(request).execute()) {
            int responseCode = response.code();
            String responseBody = response.body().string();
            
            System.out.println("Response code: " + responseCode);
            System.out.println("Response body: " + responseBody);
            
            if (!response.isSuccessful()) {
                // Handle specific error codes
                if (responseCode == 429) {
                    throw new IOException("Rate limit exceeded. Please try again later.");
                } else if (responseCode == 401 || responseCode == 403) {
                    throw new IOException("Invalid API key. Please check your Google AI API key.");
                } else if (responseCode == 400) {
                    throw new IOException("Bad request. Please check the request format.");
                } else {
                    throw new IOException("API request failed with code " + responseCode + ": " + responseBody);
                }
            }
            
            return responseBody;
        }
    }
    
    /**
     * Parse the API response to extract advice
     */
    private static String parseResponse(String response) {
        try {
            System.out.println("Parsing response: " + response);
            
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    if (firstCandidate.has("content")) {
                        JSONObject content = firstCandidate.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                JSONObject firstPart = parts.getJSONObject(0);
                                if (firstPart.has("text")) {
                                    String generatedText = firstPart.getString("text");
                                    System.out.println("Generated text: " + generatedText);
                                    
                                    // Clean up the response text
                                    String advice = cleanAdviceText(generatedText);
                                    if (advice != null && !advice.trim().isEmpty()) {
                                        return advice;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response format");
        } catch (Exception e) {
            System.err.println("Error parsing response: " + e.getMessage());
            // If JSON parsing fails, try to extract meaningful words from response
            return extractKeyWords(response);
        }
    }
    
    /**
     * Clean and extract advice from the generated text
     */
    private static String cleanAdviceText(String fullText) {
        System.out.println("Cleaning text: " + fullText);
        
        // Remove markdown formatting and common AI prefixes/suffixes
        String cleaned = fullText
            .replaceAll("\\*\\*", "") // Remove markdown bold formatting
            .replaceAll("\\*", "")    // Remove markdown italic formatting
            .replaceAll("(?i)(here are|here's|i recommend|my advice is|i suggest|based on)", "")
            .replaceAll("(?i)(analysis|the|a|an)", "")
            .replaceAll("[\"'`\\n\\r]", "") // Remove quotes and newlines
            .replaceAll("\\s+", " ")
            .trim();
        
        System.out.println("After cleaning: " + cleaned);
        
        // Split into words and keep investment-related terms
        String[] words = cleaned.split("\\s+");
        StringBuilder result = new StringBuilder();
        int wordCount = 0;
        
        for (String word : words) {
            if (wordCount >= 3) break;
            
            // Clean the word but preserve more characters
            String cleanWord = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
            
            if (cleanWord.length() >= 2 && (isInvestmentWord(cleanWord) || isActionWord(cleanWord) || isUsefulWord(cleanWord))) {
                if (wordCount > 0) result.append(" ");
                result.append(capitalizeFirst(cleanWord));
                wordCount++;
            }
        }
        
        System.out.println("Final result: " + result.toString() + " (word count: " + wordCount + ")");
        return wordCount >= 2 ? result.toString() : null;
    }
    
    /**
     * Check if a word is an action word
     */
    private static boolean isActionWord(String word) {
        String[] actionWords = {
            "buy", "sell", "hold", "wait", "watch", "avoid", "take", "cut", "add",
            "reduce", "increase", "accumulate", "distribute", "enter", "exit"
        };
        
        for (String action : actionWords) {
            if (word.equals(action)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a word is useful for investment advice
     */
    private static boolean isUsefulWord(String word) {
        String[] usefulWords = {
            "now", "today", "soon", "exposure", "position", "profits", "gains", 
            "losses", "more", "less", "some", "all", "half", "partial", "current",
            "holdings", "portfolio", "stake", "investment", "allocation"
        };
        
        for (String useful : usefulWords) {
            if (word.equals(useful)) {
                return true;
            }
        }
        return false;
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
     * Enhanced rule-based advice when AI API fails
     */
    private static String getSimpleAdvice(CryptoData crypto) {
        double profitLoss = crypto.getProfitLossPercentage();
        double entryOpportunity = crypto.getEntryOpportunity();
        double currentPrice = crypto.currentPrice;
        double avgBuyPrice = crypto.avgBuyPrice;
        double holdings = crypto.holdings;
        
        // Strong profit scenarios
        if (profitLoss > 0.30) return "Take Profits";
        else if (profitLoss > 0.20) return "Secure Gains";
        else if (profitLoss > 0.15) return "Partial Sell";
        
        // Strong loss scenarios
        else if (profitLoss < -0.30) return "Cut Losses";
        else if (profitLoss < -0.20) return "Review Position";
        else if (profitLoss < -0.15) return "Stop Loss";
        
        // Entry opportunity scenarios
        else if (entryOpportunity > 0.15) return "Buy Opportunity";
        else if (entryOpportunity > 0.10) return "Dollar Average";
        else if (entryOpportunity > 0.05) return "Good Entry";
        
        // Overvalued scenarios
        else if (currentPrice > crypto.targetPriceLongTerm * 1.1) return "Overvalued Now";
        else if (currentPrice > crypto.targetPrice3Month * 1.05) return "Near Target";
        
        // Undervalued scenarios
        else if (currentPrice < crypto.expectedEntry * 0.95) return "Great Price";
        else if (currentPrice < crypto.expectedEntry) return "Below Entry";
        
        // Holdings-based advice
        else if (holdings == 0 && currentPrice < crypto.expectedEntry) return "Start Position";
        else if (holdings == 0) return "Wait Entry";
        else if (holdings > 0 && profitLoss > 0.05) return "Hold Gains";
        else if (holdings > 0 && profitLoss < -0.05) return "Hold Steady";
        
        // Default advice
        else return "Hold Position";
    }
    
    /**
     * Shutdown the executor service and HTTP client
     */
    public static void shutdown() {
        executor.shutdown();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
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
