package service;

import model.CryptoData;
import cache.AiResponseCache;
import util.LoggerUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.List;
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
    
    // Rate limiting only - no circuit breaker
    private static volatile long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 10; // 10 seconds between requests (more conservative)
    
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // Reduced to 1 thread for sequential processing
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS) // Increased for detailed analysis
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // We handle retries manually
            .build();
    
    /**
     * Get AI advice for a cryptocurrency asynchronously
     * @param crypto The cryptocurrency data
     * @return CompletableFuture containing three-word advice
     */
    public static CompletableFuture<String> getAdviceAsync(CryptoData crypto) {
        LoggerUtil.info(AiAdviceService.class, "Getting AI advice asynchronously for: " + crypto.symbol);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String advice = getAdvice(crypto);
                if (USE_AI_API) {
                    crypto.setAiAdviceFromAI(advice);
                    // Cache the successful AI response
                    AiResponseCache.cacheSimpleAdvice(crypto.symbol, advice);
                    LoggerUtil.info(AiAdviceService.class, "Successfully cached AI advice for " + crypto.symbol + ": " + advice);
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
        LoggerUtil.info(AiAdviceService.class, "Getting AI advice for: " + crypto.symbol);
        // Use rule-based advice as primary method to avoid API issues
        if (!USE_AI_API) {
            return getSimpleAdvice(crypto);
        }
        
        // Check cache first for AI responses
        String cachedAdvice = AiResponseCache.getCachedSimpleAdvice(crypto.symbol);
        if (cachedAdvice != null) {
            LoggerUtil.info(AiAdviceService.class, "Using cached AI advice for " + crypto.symbol + ": " + cachedAdvice);
            return cachedAdvice;
        }
        
        try {
            // Create a prompt for the AI
            String prompt = createPrompt(crypto);
            
            // Make API request
            String response = makeApiRequest(prompt);
            
            // Parse and clean the response
            String advice = parseResponse(response);
            
            // Ensure it's exactly three words
            String formattedAdvice = formatToThreeWords(advice);
            
            // Cache the successful AI response
            AiResponseCache.cacheSimpleAdvice(crypto.symbol, formattedAdvice);
            LoggerUtil.info(AiAdviceService.class, "Successfully got and cached AI advice for " + crypto.symbol + ": " + formattedAdvice);
            
            return formattedAdvice;
            
        } catch (Exception e) {
            System.err.println("AI API unavailable for " + crypto.symbol + ", using rule-based advice: " + e.getMessage());
            return getSimpleAdvice(crypto);
        }
    }
    
    /**
     * Create a prompt for the AI model
     */
    private static String createPrompt(CryptoData crypto) {
        LoggerUtil.info(AiAdviceService.class, "Creating AI prompt for: " + crypto.symbol);
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
        LoggerUtil.info(AiAdviceService.class, "Making API request to Google Gemini");
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
        LoggerUtil.info(AiAdviceService.class, "Parsing API response");
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
        LoggerUtil.info(AiAdviceService.class, "Cleaning advice text");
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
        LoggerUtil.info(AiAdviceService.class, "Checking if word is action word: " + word);
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
        LoggerUtil.info(AiAdviceService.class, "Checking if word is useful: " + word);
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
        LoggerUtil.info(AiAdviceService.class, "Extracting key words from response text");
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
        LoggerUtil.info(AiAdviceService.class, "Checking if word is investment-related: " + word);
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
        LoggerUtil.info(AiAdviceService.class, "Formatting text to three words");
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
        LoggerUtil.info(AiAdviceService.class, "Capitalizing first letter of word: " + word);
        if (word == null || word.isEmpty()) return word;
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
    
    /**
     * Enhanced rule-based advice with caching support
     */
    private static String getSimpleAdvice(CryptoData crypto) {
        LoggerUtil.info(AiAdviceService.class, "Getting simple advice for: " + crypto.symbol);
        // Check cache first
        String cachedAdvice = AiResponseCache.getCachedSimpleAdvice(crypto.symbol);
        if (cachedAdvice != null) {
            return cachedAdvice;
        }
        // Generate advice based on current data
        String advice = generateSimpleAdvice(crypto);
        return advice;
    }
    
    /**
     * Generate simple advice based on crypto data (extracted for caching)
     */
    private static String generateSimpleAdvice(CryptoData crypto) {
        LoggerUtil.info(AiAdviceService.class, "Generating simple advice for: " + crypto.symbol);
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
        LoggerUtil.info(AiAdviceService.class, "Shutting down AiAdviceService");
        executor.shutdown();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
    
    /**
     * Generate detailed AI analysis for a cryptocurrency
     * @param crypto The cryptocurrency data
     * @return Detailed analysis string from AI or cache
     */
    public static String getDetailedAnalysis(CryptoData crypto) {
        LoggerUtil.info(AiAdviceService.class, "Getting detailed analysis for: " + crypto.symbol);
        return getDetailedAnalysis(crypto, false);
    }
    
    /**
     * Generate detailed AI analysis for a cryptocurrency with cache control
     * @param crypto The cryptocurrency data
     * @param forceRefresh True to bypass cache and get fresh AI response
     * @return Detailed analysis string from AI or cache
     */
    public static String getDetailedAnalysis(CryptoData crypto, boolean forceRefresh) {
        LoggerUtil.info(AiAdviceService.class, "Getting detailed analysis with cache control for: " + crypto.symbol + " (forceRefresh: " + forceRefresh + ")");
        LoggerUtil.info("Generating AI analysis for " + crypto.symbol + (forceRefresh ? " (forced refresh)" : ""));
        
        // Check cache first (unless forced refresh)
        if (!forceRefresh) {
            String cachedResponse = AiResponseCache.getCachedResponse(crypto.symbol);
            if (cachedResponse != null) {
                LoggerUtil.info("Using cached AI analysis for " + crypto.symbol);
                return cachedResponse;
            }
        } else {
            // Clear cache for forced refresh
            AiResponseCache.clearCache(crypto.symbol);
        }
        
        try {
            // Create detailed prompt for AI analysis
            String prompt = createDetailedAnalysisPrompt(crypto);
            
            // Get AI analysis
            String aiAnalysis = getAiResponse(prompt);
            
            if (aiAnalysis != null && !aiAnalysis.trim().isEmpty()) {
                LoggerUtil.info("Successfully generated AI analysis for " + crypto.symbol);
                
                // Format the analysis
                String formattedAnalysis = formatDetailedAnalysis(crypto, aiAnalysis);
                
                // Cache the successful response
                AiResponseCache.cacheResponse(crypto.symbol, formattedAnalysis);
                
                return formattedAnalysis;
            } else {
                LoggerUtil.warning("AI returned empty response for " + crypto.symbol);
                return "AI analysis unavailable at the moment. Please try again.";
            }
            
        } catch (Exception e) {
            LoggerUtil.error("Error getting AI analysis for " + crypto.symbol + ": " + e.getMessage());
            return "Error getting AI analysis: " + e.getMessage();
        }
    }
    
    /**
     * Create a comprehensive prompt for detailed cryptocurrency analysis
     */
    private static String createDetailedAnalysisPrompt(CryptoData crypto) {
        LoggerUtil.info(AiAdviceService.class, "Creating detailed analysis prompt for: " + crypto.symbol);
        double profitLoss = crypto.getProfitLossPercentage() * 100;
        double entryOpportunity = crypto.getEntryOpportunity() * 100;
        double currentToTarget = ((crypto.targetPriceLongTerm - crypto.currentPrice) / crypto.currentPrice) * 100;
        
        return String.format(
            "As a professional cryptocurrency analyst, provide a comprehensive investment analysis for %s (%s). " +
            "Use the following data to create detailed insights:\n\n" +
            
            "CURRENT POSITION:\n" +
            "• Symbol: %s\n" +
            "• Current Price: $%.2f\n" +
            "• Your Average Buy Price: $%.2f\n" +
            "• Holdings: %.4f %s\n" +
            "• Current P&L: %.2f%%\n" +
            "• Expected Entry Price: $%.2f\n" +
            "• 3-Month Target: $%.2f\n" +
            "• Long-term Target: $%.2f\n" +
            "• Entry Opportunity Score: %.2f%%\n" +
            "• Potential to Long Target: %.2f%%\n\n" +
            
            "Please provide analysis in these sections:\n" +
            "1. 📊 MARKET POSITION ANALYSIS - Current price vs targets and market context\n" +
            "2. 💰 PERFORMANCE METRICS - P&L analysis and investment performance\n" +
            "3. ⚠️ RISK ASSESSMENT - Risk level and key risk factors\n" +
            "4. 🎯 STRATEGIC RECOMMENDATIONS - Specific actionable advice (buy/sell/hold)\n" +
            "5. 📈 TECHNICAL OUTLOOK - Price levels, support/resistance, momentum\n" +
            "6. 🔮 FUTURE PROSPECTS - Short and long-term outlook\n\n" +
            
            "Make the analysis specific, actionable, and professional. Use emojis for readability. " +
            "Focus on practical investment decisions based on the current position and market data.",
            
            crypto.name, crypto.symbol, crypto.symbol, crypto.currentPrice, crypto.avgBuyPrice,
            crypto.holdings, crypto.symbol, profitLoss, crypto.expectedEntry,
            crypto.targetPrice3Month, crypto.targetPriceLongTerm, entryOpportunity, currentToTarget
        );
    }
    
    /**
     * Format the AI analysis response with current data
     */
    private static String formatDetailedAnalysis(CryptoData crypto, String aiAnalysis) {
        LoggerUtil.info(AiAdviceService.class, "Formatting detailed analysis for: " + crypto.symbol);
        StringBuilder formatted = new StringBuilder();
        
        // Add header with current data
        formatted.append("🔍 DETAILED AI ANALYSIS FOR ").append(crypto.symbol.toUpperCase()).append("\n");
        formatted.append("Generated: ").append(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // Add the AI analysis
        formatted.append(aiAnalysis);
        
        // Add footer
        formatted.append("\n\n");
        formatted.append("═══════════════════════════════════════════════════════════\n");
        formatted.append("⚠️ DISCLAIMER: This AI analysis is for informational purposes only.\n");
        formatted.append("Always conduct your own research and consider your risk tolerance.\n");
        formatted.append("Cryptocurrency investments carry significant risk of loss.\n");
        
        return formatted.toString();
    }
    
    /**
     * Get AI response for detailed analysis with proper retry logic
     * @param prompt The detailed prompt for analysis
     * @return AI generated analysis text
     */
    private static String getAiResponse(String prompt) {
        LoggerUtil.info(AiAdviceService.class, "Getting AI response for detailed analysis");
        if (!USE_AI_API) {
            LoggerUtil.warning("AI API is disabled");
            return null;
        }
        
        final int MAX_RETRIES = 3;
        final long BASE_DELAY = 5000; // 5 seconds base delay
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Rate limiting - ensure minimum interval between requests
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime;
                if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
                    long waitTime = MIN_REQUEST_INTERVAL - timeSinceLastRequest;
                    LoggerUtil.info("Rate limiting: waiting " + waitTime + "ms before attempt " + attempt);
                    Thread.sleep(waitTime);
                }
                
                LoggerUtil.info("Making AI API request (attempt " + attempt + "/" + MAX_RETRIES + ")");
                
                // Make API request
                String response = makeDetailedApiRequest(prompt);
                lastRequestTime = System.currentTimeMillis();
                
                // Parse and return the response
                String analysis = parseDetailedResponse(response);
                
                if (analysis != null && !analysis.trim().isEmpty()) {
                    LoggerUtil.info("Successfully received AI response on attempt " + attempt);
                    return analysis;
                } else {
                    LoggerUtil.warning("AI returned empty response on attempt " + attempt);
                    if (attempt < MAX_RETRIES) {
                        long retryDelay = BASE_DELAY * attempt; // Linear backoff
                        LoggerUtil.info("Retrying in " + retryDelay + "ms...");
                        Thread.sleep(retryDelay);
                    }
                }
                
            } catch (IOException e) {
                if (e.getMessage().contains("Rate limit exceeded")) {
                    LoggerUtil.warning("Rate limit hit on attempt " + attempt + ": " + e.getMessage());
                    if (attempt < MAX_RETRIES) {
                        // Exponential backoff for rate limit errors
                        long retryDelay = BASE_DELAY * (long) Math.pow(2, attempt - 1);
                        LoggerUtil.info("Rate limit backoff: waiting " + retryDelay + "ms before retry...");
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            LoggerUtil.error("Retry interrupted");
                            return null;
                        }
                    } else {
                        LoggerUtil.error("Rate limit exceeded after all retry attempts");
                        return null;
                    }
                } else {
                    LoggerUtil.error("AI API request failed on attempt " + attempt + ": " + e.getMessage());
                    if (attempt < MAX_RETRIES) {
                        long retryDelay = BASE_DELAY * attempt;
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return null;
                        }
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LoggerUtil.error("AI API request interrupted: " + e.getMessage());
                return null;
            } catch (Exception e) {
                LoggerUtil.error("Unexpected error on attempt " + attempt + ": " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(BASE_DELAY * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        
        LoggerUtil.error("AI API failed after all retry attempts");
        return null;
    }

    /**
     * Make API request optimized for detailed analysis
     */
    private static String makeDetailedApiRequest(String prompt) throws IOException {
        LoggerUtil.info(AiAdviceService.class, "Making detailed API request to Google Gemini");
        LoggerUtil.debug("Making detailed analysis API request to: " + API_URL);
        
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
        
        // Configuration optimized for detailed analysis
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.8);
        generationConfig.put("maxOutputTokens", 2048); // Much higher for detailed analysis
        generationConfig.put("topP", 0.9);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);
        
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
            String responseBody = response.body() != null ? response.body().string() : "";
            
            LoggerUtil.debug("AI API response code: " + responseCode);
            
            if (response.isSuccessful()) {
                return responseBody;
            } else {
                // Handle specific error codes
                String errorMessage;
                switch (responseCode) {
                    case 429:
                        errorMessage = "Rate limit exceeded. Please try again later.";
                        LoggerUtil.warning("Google Gemini API rate limit hit (429)");
                        break;
                    case 401:
                    case 403:
                        errorMessage = "Invalid API key. Please check your Google AI API key.";
                        LoggerUtil.error("Google Gemini API authentication failed (" + responseCode + ")");
                        break;
                    case 400:
                        errorMessage = "Bad request. Please check the request format.";
                        LoggerUtil.error("Google Gemini API bad request (400): " + responseBody);
                        break;
                    case 500:
                    case 502:
                    case 503:
                    case 504:
                        errorMessage = "Google Gemini API server error (" + responseCode + "). Please try again later.";
                        LoggerUtil.warning("Google Gemini API server error (" + responseCode + ")");
                        break;
                    default:
                        errorMessage = "API request failed with code " + responseCode + ": " + responseBody;
                        LoggerUtil.error("Google Gemini API unexpected error (" + responseCode + "): " + responseBody);
                }
                throw new IOException(errorMessage);
            }
        }
    }

    /**
     * Parse detailed analysis response from AI API
     */
    private static String parseDetailedResponse(String response) {
        LoggerUtil.info(AiAdviceService.class, "Parsing detailed AI response");
        try {
            LoggerUtil.debug("Parsing detailed AI response");
            
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
                                    LoggerUtil.debug("Successfully extracted detailed analysis text");
                                    
                                    // Clean up the response text for detailed analysis
                                    String analysis = cleanDetailedAnalysisText(generatedText);
                                    if (analysis != null && !analysis.trim().isEmpty()) {
                                        return analysis;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response format from AI API");
        } catch (Exception e) {
            LoggerUtil.error("Error parsing detailed AI response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clean and format detailed analysis text
     */
    private static String cleanDetailedAnalysisText(String fullText) {
        LoggerUtil.info(AiAdviceService.class, "Cleaning detailed analysis text");
        if (fullText == null || fullText.trim().isEmpty()) {
            return null;
        }
        
        // Clean the text but preserve structure for detailed analysis
        String cleaned = fullText
            .replaceAll("(?i)^(here is|here's|i'll provide|my analysis is|based on|the analysis shows)", "")
            .trim();
        
        // Ensure we have substantial content
        if (cleaned.length() < 100) {
            LoggerUtil.warning("AI response too short for detailed analysis: " + cleaned.length() + " characters");
            return null;
        }
        
        return cleaned;
    }

    /**
     * Generate AI portfolio overview analysis for email reports
     * @param cryptoList The list of crypto holdings in the portfolio
     * @return AI-generated portfolio overview analysis with balance recommendations
     */
    public static String getPortfolioOverviewAnalysis(List<CryptoData> cryptoList) {
        LoggerUtil.info(AiAdviceService.class, "Getting AI portfolio overview analysis for email");
        
        if (cryptoList == null || cryptoList.isEmpty()) {
            LoggerUtil.warning(AiAdviceService.class, "No crypto data available for portfolio overview analysis");
            return "No portfolio data available for analysis.";
        }
        
        try {
            // Create comprehensive portfolio analysis prompt
            String prompt = createPortfolioOverviewPrompt(cryptoList);
            
            // Get AI analysis
            String aiAnalysis = getAiResponse(prompt);
            
            if (aiAnalysis != null && !aiAnalysis.trim().isEmpty()) {
                LoggerUtil.info(AiAdviceService.class, "Successfully generated portfolio overview analysis");
                return formatPortfolioOverviewAnalysis(aiAnalysis, cryptoList);
            } else {
                LoggerUtil.warning(AiAdviceService.class, "AI returned empty response for portfolio overview");
                return "AI portfolio overview analysis unavailable at the moment. Please try again.";
            }
            
        } catch (Exception e) {
            LoggerUtil.error(AiAdviceService.class, "Error getting portfolio overview analysis: " + e.getMessage(), e);
            return "Error getting portfolio overview analysis: " + e.getMessage();
        }
    }
    
    /**
     * Create comprehensive prompt for portfolio overview analysis
     */
    private static String createPortfolioOverviewPrompt(List<CryptoData> cryptoList) {
        LoggerUtil.info(AiAdviceService.class, "Creating portfolio overview analysis prompt");
        
        StringBuilder prompt = new StringBuilder();
        
        // Calculate portfolio metrics
        double totalValue = 0;
        double totalProfitLoss = 0;
        for (CryptoData crypto : cryptoList) {
            totalValue += crypto.getTotalValue();
            totalProfitLoss += crypto.getProfitLoss();
        }
        
        prompt.append("As a professional cryptocurrency portfolio advisor, analyze this complete portfolio and provide comprehensive insights about holdings allocation, performance, and rebalancing recommendations.\n\n");
        
        // Portfolio summary
        prompt.append("PORTFOLIO OVERVIEW:\n");
        prompt.append(String.format("• Total Portfolio Value: $%.2f\n", totalValue));
        prompt.append(String.format("• Total P&L: $%.2f (%.2f%%)\n", 
            totalProfitLoss, totalValue > 0 ? (totalProfitLoss / totalValue) * 100 : 0));
        prompt.append(String.format("• Number of Holdings: %d\n\n", cryptoList.size()));
        
        // Individual holdings analysis
        prompt.append("DETAILED HOLDINGS BREAKDOWN:\n");
        
        // Sort by total value for better analysis
        List<CryptoData> sortedCryptos = new java.util.ArrayList<>(cryptoList);
        sortedCryptos.sort((a, b) -> Double.compare(b.getTotalValue(), a.getTotalValue()));
        
        for (CryptoData crypto : sortedCryptos) {
            double allocation = totalValue > 0 ? (crypto.getTotalValue() / totalValue) * 100 : 0;
            double profitLossPercentage = crypto.getProfitLossPercentage() * 100;
            
            prompt.append(String.format("• %s (%s):\n", crypto.symbol.toUpperCase(), crypto.name));
            prompt.append(String.format("  - Current Value: $%.2f (%.2f%% of portfolio)\n", 
                crypto.getTotalValue(), allocation));
            prompt.append(String.format("  - Holdings: %.6f %s at avg cost $%.4f\n", 
                crypto.holdings, crypto.symbol, crypto.avgBuyPrice));
            prompt.append(String.format("  - Current Price: $%.4f (P&L: %.2f%%)\n", 
                crypto.currentPrice, profitLossPercentage));
            prompt.append(String.format("  - 3M Target: $%.4f, Long Target: $%.4f\n\n", 
                crypto.targetPrice3Month, crypto.targetPriceLongTerm));
        }
        
        // Request specific analysis sections
        prompt.append("REQUIRED ANALYSIS - Please provide detailed insights in these sections:\n\n");
        
        prompt.append("🎯 PORTFOLIO ALLOCATION ASSESSMENT:\n");
        prompt.append("- Evaluate current allocation percentages\n");
        prompt.append("- Identify over/under-allocated positions\n");
        prompt.append("- Rate overall diversification (1-10 scale)\n\n");
        
        prompt.append("⚖️ REBALANCING RECOMMENDATIONS:\n");
        prompt.append("- Specific suggestions to improve balance\n");
        prompt.append("- Which positions to increase/decrease\n");
        prompt.append("- Target allocation percentages for each holding\n\n");
        
        prompt.append("📊 PERFORMANCE ANALYSIS:\n");
        prompt.append("- Best and worst performing assets\n");
        prompt.append("- Overall portfolio performance assessment\n");
        prompt.append("- Risk vs return evaluation\n\n");
        
        prompt.append("🔮 STRATEGIC OUTLOOK:\n");
        prompt.append("- Market positioning analysis\n");
        prompt.append("- Future growth potential of current holdings\n");
        prompt.append("- Recommended actions for next 3-6 months\n\n");
        
        prompt.append("Format your response with clear sections using the emoji headers above. ");
        prompt.append("Provide specific, actionable recommendations with numerical targets where appropriate. ");
        prompt.append("Focus on practical portfolio optimization strategies.");
        
        return prompt.toString();
    }
    
    /**
     * Format AI portfolio overview analysis for email display
     */
    private static String formatPortfolioOverviewAnalysis(String aiAnalysis, List<CryptoData> cryptoList) {
        LoggerUtil.info(AiAdviceService.class, "Formatting portfolio overview analysis");
        
        StringBuilder formatted = new StringBuilder();
        
        // Add the AI analysis directly without header
        formatted.append(aiAnalysis);
        
        // Add portfolio summary stats
        formatted.append("\n\n📋 PORTFOLIO QUICK STATS:\n");
        formatted.append("─────────────────────────────\n");
        
        double totalValue = 0;
        double totalProfitLoss = 0;
        String bestPerformer = "N/A";
        double bestPerformance = Double.NEGATIVE_INFINITY;
        
        for (CryptoData crypto : cryptoList) {
            totalValue += crypto.getTotalValue();
            totalProfitLoss += crypto.getProfitLoss();
            
            double performance = crypto.getProfitLossPercentage() * 100;
            if (performance > bestPerformance) {
                bestPerformance = performance;
                bestPerformer = crypto.symbol;
            }
        }
        
        formatted.append(String.format("• Total Value: $%.2f\n", totalValue));
        formatted.append(String.format("• Total P&L: $%.2f (%.2f%%)\n", 
            totalProfitLoss, totalValue > 0 ? (totalProfitLoss / totalValue) * 100 : 0));
        formatted.append(String.format("• Holdings Count: %d assets\n", cryptoList.size()));
        formatted.append(String.format("• Best Performer: %s (%.2f%%)\n", bestPerformer, bestPerformance));
        
        // Add generation timestamp
        formatted.append("\n⏰ Analysis Generated: ");
        formatted.append(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return formatted.toString();
    }
}