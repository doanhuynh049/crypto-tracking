package data;

import cache.AiResponseCache;
import cache.CoinGeckoApiCache;
import model.CryptoData;
import service.AiAdviceService;
import service.ApiCoordinationService;
import service.TechnicalAnalysisService;
import ui.panel.PortfolioUIBuilder;
import util.LoggerUtil;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

/**
 * Data Manager class responsible for handling all data operations
 * including loading, saving, fetching prices, and managing cryptocurrency data.
 */
public class PortfolioDataManager {
    
    // Crypto data storage
    private List<CryptoData> cryptoList;
    private static final String DATA_FILE = "src/data/.portfolio_data.bin"; // Hidden binary file for security
    private boolean isUpdatingTable = false; // Flag to prevent infinite recursion
    
    // Data comparison for conditional saving
    private String lastSavedDataHash = null; // Hash of last saved data to prevent unnecessary saves
    
    // Reference to UI builder for updates
    private PortfolioUIBuilder uiBuilder;
    private Timer refreshTimer;
    
    // API coordination
    private final ApiCoordinationService apiCoordinator = ApiCoordinationService.getInstance();
    
    public PortfolioDataManager() {
        LoggerUtil.info(PortfolioDataManager.class, "Initializing Portfolio Data Manager");
        
        try {
            loadPortfolioData(); // Load saved data first
            if (cryptoList.isEmpty()) {
                LoggerUtil.info(PortfolioDataManager.class, "No saved data found, initializing with default cryptocurrency list");
                initializeCryptoList(); // Only initialize defaults if no saved data
            } else {
                LoggerUtil.info(PortfolioDataManager.class, "Loaded " + cryptoList.size() + " cryptocurrency entries from saved data");
            }
        } catch (Exception e) {
            LoggerUtil.error(PortfolioDataManager.class, "Failed to initialize Portfolio Data Manager", e);
            throw e;
        }
    }
    
    /**
     * Set the UI builder reference for callbacks
     */
    public void setUIBuilder(PortfolioUIBuilder uiBuilder) {
        this.uiBuilder = uiBuilder;
    }

    /**
     * Validate cryptocurrency ID with CoinGecko API
     * @param cryptoId The cryptocurrency ID to validate
     * @return CompletableFuture<ValidationResult> containing validation info
     */
    public CompletableFuture<ValidationResult> validateCryptocurrencyId(String cryptoId) {
        LoggerUtil.debug(PortfolioDataManager.class, "Validating cryptocurrency ID: " + cryptoId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first for validation
            Double cachedPrice = CoinGeckoApiCache.getCachedPrice(cryptoId);
            if (cachedPrice != null) {
                LoggerUtil.debug(PortfolioDataManager.class, "Using cached price for validation of: " + cryptoId);
                return new ValidationResult(true, "Valid cryptocurrency ID (cached)", cachedPrice, cryptoId);
            }
            
            try {
                // Test API call to check if cryptocurrency exists
                String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + 
                               cryptoId + "&vs_currencies=usd";
                
                LoggerUtil.debug(PortfolioDataManager.class, "Making API request to validate: " + cryptoId);
                
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    
                    if (jsonResponse.has(cryptoId)) {
                        JSONObject cryptoData = jsonResponse.getJSONObject(cryptoId);
                        if (cryptoData.has("usd")) {
                            double currentPrice = cryptoData.getDouble("usd");
                            
                            // Cache the validated price
                            CoinGeckoApiCache.cachePrice(cryptoId, currentPrice);
                            
                            return new ValidationResult(true, "Valid cryptocurrency ID", currentPrice, cryptoId);
                        }
                    }
                    return new ValidationResult(false, "Cryptocurrency ID not found in API response", 0.0, cryptoId);
                } else {
                    return new ValidationResult(false, "API returned error code: " + responseCode, 0.0, cryptoId);
                }
                
            } catch (Exception e) {
                return new ValidationResult(false, "Error validating cryptocurrency: " + e.getMessage(), 0.0, cryptoId);
            }
        });
    }

    /**
     * Inner class to represent validation results
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public final double currentPrice;
        public final String cryptoId;
        
        public ValidationResult(boolean isValid, String message, double currentPrice, String cryptoId) {
            this.isValid = isValid;
            this.message = message;
            this.currentPrice = currentPrice;
            this.cryptoId = cryptoId;
        }
    }

    /**
     * Add new cryptocurrency to the portfolio with validation
     */
    public CompletableFuture<Boolean> addCryptoWithValidation(CryptoData newCrypto) {
        return validateCryptocurrencyId(newCrypto.id)
            .thenApply(validation -> {
                SwingUtilities.invokeLater(() -> {
                    if (validation.isValid) {
                        // Update current price from validation
                        newCrypto.currentPrice = validation.currentPrice;
                        
                        // Add to list
                        cryptoList.add(newCrypto);
                        
                        // Sort and rebuild the table to maintain ranking by total value
                        sortCryptosByTotalValue();
                        rebuildTable();
                        
                        savePortfolioData();
                        updatePortfolioValue();
                        
                        // Show success message with current price
                        if (uiBuilder != null) {
                            JOptionPane.showMessageDialog(uiBuilder.getCryptoTable(), 
                                String.format("✅ Successfully added %s (%s)!\n\n" +
                                            "📊 Current Price: $%.4f\n" +
                                            "🔄 Price fetching is working correctly!",
                                            newCrypto.name, newCrypto.symbol, validation.currentPrice), 
                                "Cryptocurrency Added Successfully", 
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                        
                        // Refresh prices to ensure everything is up to date
                        refreshPrices();
                        
                    } else {
                        // Show error message with suggestions
                        String errorMsg = String.format("❌ Failed to add cryptocurrency!\n\n" +
                                                       "🔍 Issue: %s\n\n" +
                                                       "💡 Suggestions:\n" +
                                                       "• Check if the Coin ID is correct\n" +
                                                       "• Visit CoinGecko and search for your coin\n" +
                                                       "• Use the exact ID from the coin's URL\n" +
                                                       "• Example: bitcoin, ethereum, cardano, solana\n\n" +
                                                       "🌐 Internet connection required for validation",
                                                       validation.message);
                        
                        if (uiBuilder != null) {
                            JOptionPane.showMessageDialog(uiBuilder.getCryptoTable(), 
                                errorMsg, 
                                "Validation Failed", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                return validation.isValid;
            });
    }

    /**
     * Initialize default cryptocurrency list
     */
    private void initializeCryptoList() {
        cryptoList = new ArrayList<>();
        // Initialize with popular cryptocurrencies with sample holdings
        cryptoList.add(new CryptoData("bitcoin", "Bitcoin", "BTC", 0.0, 50000.0, 45000.0, 55000.0, 80000.0, 0.0, 0.0));     
        // AI fields are automatically initialized in constructors (transient data)
        
        forceSavePortfolioData(); // Force save initial data (always save on first setup)
    }
    
    /**
     * Load initial prices and populate table
     */
    public void loadInitialPrices() {
        SwingUtilities.invokeLater(() -> {
            // Sort by total value before displaying
            sortCryptosByTotalValue();
            
            // AI fields are automatically initialized in constructors/loading (transient data)
            
            if (uiBuilder != null) {
                uiBuilder.getTableModel().setRowCount(0);
                for (CryptoData crypto : cryptoList) {
                    uiBuilder.addCryptoToTable(crypto);
                }
                updatePortfolioValue(); // Update portfolio value display
                // Initialize AI status with all loading
                updateAiStatusProgress();
                // Auto-fit column widths after loading all data with a delay to ensure proper calculation
                SwingUtilities.invokeLater(() -> {
                    uiBuilder.autoFitColumnWidths();
                });
            }
            refreshPrices();
        });
    }
    
    /**
     * Sort cryptocurrencies by total value in descending order (highest first)
     */
    private void sortCryptosByTotalValue() {
        cryptoList.sort((crypto1, crypto2) -> {
            double value1 = crypto1.getTotalValue();
            double value2 = crypto2.getTotalValue();
            return Double.compare(value2, value1); // Descending order
        });
    }
    
    /**
     * Refresh cryptocurrency prices
     */
    public void refreshPrices() {
        LoggerUtil.debug(PortfolioDataManager.class, "Starting price refresh");
        
        if (uiBuilder != null) {
            uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Refreshing...");
            uiBuilder.getRefreshButton().setEnabled(false);
            uiBuilder.getRefreshButton().setText("Refreshing...");
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fetchCryptoPrices();
                // AI advice is fetched separately only when needed, not on every price refresh
                return null;
            }
            
            @Override
            protected void done() {
                LoggerUtil.debug(PortfolioDataManager.class, "Price refresh completed");
                updateTableData();
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Ready");
                    uiBuilder.getRefreshButton().setEnabled(true);
                    uiBuilder.getRefreshButton().setText("🔄 Refresh Prices");
                }
                updatePortfolioValue();
            }
        };
        worker.execute();
    }
    
    /**
     * Fetch cryptocurrency prices from API with coordinated rate limiting
     */
    private void fetchCryptoPrices() {
        LoggerUtil.debug(PortfolioDataManager.class, "Fetching cryptocurrency prices from API");
        
        // Check cache for all cryptocurrencies first
        List<CryptoData> cryptosNeedingUpdate = new ArrayList<>();
        int cachedPrices = 0;
        
        for (CryptoData crypto : cryptoList) {
            Double cachedPrice = CoinGeckoApiCache.getCachedPrice(crypto.id);
            if (cachedPrice != null) {
                double oldPrice = crypto.currentPrice;
                crypto.currentPrice = cachedPrice;
                if (oldPrice != crypto.currentPrice) {
                    cachedPrices++;
                    LoggerUtil.debug(PortfolioDataManager.class, 
                        String.format("Using cached price for %s: $%.4f", crypto.symbol, cachedPrice));
                }
            } else {
                cryptosNeedingUpdate.add(crypto);
            }
        }
        
        if (cachedPrices > 0) {
            LoggerUtil.info(PortfolioDataManager.class, 
                String.format("Used cached prices for %d cryptocurrencies", cachedPrices));
        }
        
        // If all prices are cached, we're done
        if (cryptosNeedingUpdate.isEmpty()) {
            LoggerUtil.info(PortfolioDataManager.class, "All prices served from cache");
            SwingUtilities.invokeLater(() -> updateTableData());
            return;
        }
        
        // Check with API coordinator before making bulk price request for remaining cryptos
        if (!apiCoordinator.requestApiCall("PortfolioDataManager", "bulk price update")) {
            LoggerUtil.info(PortfolioDataManager.class, 
                "API coordination denied price fetch - technical analysis in progress");
            return;
        }
        
        try {
            StringBuilder cryptoIds = new StringBuilder();
            for (int i = 0; i < cryptosNeedingUpdate.size(); i++) {
                if (i > 0) cryptoIds.append(",");
                cryptoIds.append(cryptosNeedingUpdate.get(i).id);
            }
            
            String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + 
                           cryptoIds.toString() + "&vs_currencies=usd";
            
            LoggerUtil.debug(PortfolioDataManager.class, "API Request URL: " + apiUrl);
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // Increased timeout
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                LoggerUtil.warning(PortfolioDataManager.class, 
                    "Rate limited during price fetch - will retry later");
                return;
            }
            
            if (responseCode != 200) {
                LoggerUtil.warning(PortfolioDataManager.class, 
                    "Price API returned response code: " + responseCode);
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            int updatedPrices = 0;
            
            for (CryptoData crypto : cryptosNeedingUpdate) {
                if (jsonResponse.has(crypto.id)) {
                    JSONObject cryptoData = jsonResponse.getJSONObject(crypto.id);
                    if (cryptoData.has("usd")) {
                        double oldPrice = crypto.currentPrice;
                        double newPrice = cryptoData.getDouble("usd");
                        crypto.currentPrice = newPrice;
                        
                        // Cache the new price
                        CoinGeckoApiCache.cachePrice(crypto.id, newPrice);
                        
                        if (oldPrice != crypto.currentPrice) {
                            updatedPrices++;
                            LoggerUtil.debug(PortfolioDataManager.class, 
                                String.format("Price updated for %s: $%.4f -> $%.4f", 
                                    crypto.symbol, oldPrice, crypto.currentPrice));
                        }
                    }
                }
            }
            
            LoggerUtil.info(PortfolioDataManager.class, 
                String.format("Successfully updated prices for %d cryptocurrencies", updatedPrices));
            
            // Update cache status display
            SwingUtilities.invokeLater(() -> {
                if (uiBuilder != null) {
                    uiBuilder.updateCacheStatus();
                }
            });
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioDataManager.class, "Failed to fetch cryptocurrency prices", e);
            SwingUtilities.invokeLater(() -> {
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Error fetching prices");
                }
            });
        }
        
        // Final UI update
        SwingUtilities.invokeLater(() -> {
            updateTableData();
            if (uiBuilder != null) {
                uiBuilder.updateCacheStatus();
            }
        });
    }

    /**
     * Refresh technical analysis for all cryptocurrencies with coordination
     */
    public void refreshTechnicalAnalysis() {
        LoggerUtil.info(PortfolioDataManager.class, "Starting technical analysis refresh");
        
        // Notify other systems about intensive operations
        apiCoordinator.notifyIntensiveOperationStart("PortfolioDataManager");
        
        if (uiBuilder != null) {
            uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Analyzing technical indicators...");
        }
        
        // Reset all cryptocurrencies to LOADING state before starting technical analysis
        for (CryptoData crypto : cryptoList) {
            crypto.technicalAnalysisStatus = "LOADING";
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fetchTechnicalAnalysis();
                return null;
            }
            
            @Override
            protected void done() {
                // Notify completion of intensive operations
                apiCoordinator.notifyIntensiveOperationComplete("PortfolioDataManager");
                
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Technical analysis completed");
                }
            }
        };
        worker.execute();
    }

    /**
     * Fetch technical analysis for all cryptocurrencies sequentially
     */
    private void fetchTechnicalAnalysis() {
        try {
            // Fetch technical analysis sequentially with delays
            fetchTechnicalAnalysisSequentially(0);
        } catch (Exception e) {
            LoggerUtil.error(PortfolioDataManager.class, "Error initiating technical analysis fetch", e);
        }
    }

    /**
     * Fetch technical analysis sequentially for each cryptocurrency
     */
    private void fetchTechnicalAnalysisSequentially(int index) {
        if (index >= cryptoList.size()) {
            // All cryptocurrencies processed
            SwingUtilities.invokeLater(() -> {
                updateTableData();
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Ready");
                }
            });
            return;
        }
        
        CryptoData crypto = cryptoList.get(index);
        LoggerUtil.debug(PortfolioDataManager.class, 
            "Analyzing technical indicators for " + crypto.symbol + " (" + (index + 1) + "/" + cryptoList.size() + ")");
        
        // Update status to show progress
        SwingUtilities.invokeLater(() -> {
            if (uiBuilder != null) {
                uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Analyzing " + crypto.symbol + "...");
            }
        });
        
        TechnicalAnalysisService.analyzeEntry(crypto)
            .thenAccept(indicators -> {
                LoggerUtil.debug(PortfolioDataManager.class, 
                    "Successfully analyzed " + crypto.symbol + " - Quality: " + indicators.getOverallEntryQuality());
                
                crypto.setTechnicalIndicators(indicators);
                
                // Update table for this specific crypto
                SwingUtilities.invokeLater(() -> {
                    updateSingleCryptoInTable(crypto, index);
                });
                
                // Wait a bit before next request (15 second delay to prevent rate limiting)
                Timer delayTimer = new Timer(15000, e -> {
                    fetchTechnicalAnalysisSequentially(index + 1);
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            })
            .exceptionally(throwable -> {
                LoggerUtil.error(PortfolioDataManager.class, 
                    "Failed to analyze " + crypto.symbol, throwable);
                
                crypto.setTechnicalAnalysisError();
                
                SwingUtilities.invokeLater(() -> {
                    updateSingleCryptoInTable(crypto, index);
                });
                
                // Continue with next crypto even if this one failed (shorter delay for errors)
                Timer delayTimer = new Timer(5000, e -> {
                    fetchTechnicalAnalysisSequentially(index + 1);
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
                
                return null;
            });
    }
    
    /**
     * Refresh AI advice for all cryptocurrencies (called separately from price refresh)
     */
    public void refreshAiAdvice() {
        refreshAiAdvice(false); // Default to using cache
    }
    
    /**
     * Refresh AI advice for all cryptocurrencies with cache control
     * @param forceRefresh True to bypass cache and get fresh AI responses
     */
    public void refreshAiAdvice(boolean forceRefresh) {
        // Reset all cryptocurrencies to LOADING state before starting AI refresh
        for (CryptoData crypto : cryptoList) {
            crypto.aiStatus = "LOADING";
            crypto.aiAdvice = "Loading...";
            crypto.isAiGenerated = false;
        }
        
        if (uiBuilder != null) {
            String statusMessage = forceRefresh ? 
                "📊 Portfolio Status: Force refreshing AI advice..." : 
                "📊 Portfolio Status: Updating AI advice...";
            uiBuilder.getStatusLabel().setText(statusMessage);
            // Initialize AI status label for progress tracking - all cryptos are now loading
            uiBuilder.updateAiStatusLabel(cryptoList.size(), 0, 0, 0, cryptoList.size());
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fetchAiAdvice(forceRefresh);
                return null;
            }
            
            @Override
            protected void done() {
                // Don't call updateTableData() here as the sequential process handles final updates
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Ready");
                }
            }
        };
        worker.execute();
    }

    /**
     * Fetch AI advice for all cryptocurrencies sequentially to avoid rate limiting
     * @param forceRefresh True to bypass cache and get fresh AI responses
     */
    private void fetchAiAdvice(boolean forceRefresh) {
        try {
            // Fetch AI advice sequentially with delays to respect API rate limits
            fetchAiAdviceSequentially(0, forceRefresh);
        } catch (Exception e) {
            System.err.println("Error initiating AI advice fetch: " + e.getMessage());
        }
    }
    
    /**
     * Fetch AI advice sequentially for each cryptocurrency with delay
     * @param index The index of the cryptocurrency to process
     * @param forceRefresh True to bypass cache and get fresh AI responses
     */
    private void fetchAiAdviceSequentially(int index, boolean forceRefresh) {
        if (index >= cryptoList.size()) {
            // All cryptocurrencies processed, update UI
            SwingUtilities.invokeLater(() -> {
                updateTableData();
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: AI advice updated");
                    // Update AI status to show completion
                    uiBuilder.updateAiStatus(cryptoList);
                }
            });
            return;
        }
        
        CryptoData crypto = cryptoList.get(index);
        System.out.println("Fetching AI advice for " + crypto.symbol + " (" + (index + 1) + "/" + cryptoList.size() + ")");
        
        // Update status to show progress
        SwingUtilities.invokeLater(() -> {
            if (uiBuilder != null) {
                uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Getting AI advice for " + crypto.symbol + "...");
                // Update AI status label with current progress
                updateAiStatusProgress();
            }
        });
        
        // Check cache first before making API call (unless forced refresh)
        String cachedAdvice = null;
        if (!forceRefresh) {
            cachedAdvice = AiResponseCache.getCachedSimpleAdvice(crypto.symbol);
        } else {
            // Clear cache for forced refresh
            AiResponseCache.clearSimpleAdviceCache(crypto.symbol);
        }
        
        if (cachedAdvice != null && !cachedAdvice.trim().isEmpty()) {
            System.out.println("Using cached AI advice for " + crypto.symbol + ": " + cachedAdvice);
            
            // Set cached advice with appropriate status
            crypto.setAiAdviceFromCache(cachedAdvice); // Using AI_CACHE status for cached data
            
            // Update table for this specific crypto
            SwingUtilities.invokeLater(() -> {
                updateSingleCryptoInTable(crypto, index);
                // Update AI status progress after completion
                updateAiStatusProgress();
            });
            
            // Continue with next crypto immediately (no delay needed for cached data)
            SwingUtilities.invokeLater(() -> {
                fetchAiAdviceSequentially(index + 1, forceRefresh);
            });
        } else {
            // No cache available, make API call
            AiAdviceService.getAdviceAsync(crypto)
                .thenAccept(advice -> {
                    System.out.println("Successfully got AI advice for " + crypto.symbol + ": " + advice);
                    
                    // Update table for this specific crypto
                    SwingUtilities.invokeLater(() -> {
                        updateSingleCryptoInTable(crypto, index);
                        // Update AI status progress after completion
                        updateAiStatusProgress();
                    });
                    
                    // Wait a bit before next request to respect rate limits (2 seconds delay)
                    Timer delayTimer = new Timer(2000, e -> {
                        fetchAiAdviceSequentially(index + 1, forceRefresh);
                    });
                    delayTimer.setRepeats(false);
                    delayTimer.start();
                })
                .exceptionally(throwable -> {
                    System.err.println("Failed to get AI advice for " + crypto.symbol + ": " + throwable.getMessage());
                    // Set error status
                    crypto.setAiAdviceError();
                    
                    SwingUtilities.invokeLater(() -> {
                        updateSingleCryptoInTable(crypto, index);
                        // Update AI status progress after error
                        updateAiStatusProgress();
                    });
                    
                    // Continue with next crypto even if this one failed (shorter delay for errors)
                    Timer delayTimer = new Timer(1000, e -> {
                        fetchAiAdviceSequentially(index + 1, forceRefresh);
                    });
                    delayTimer.setRepeats(false);
                    delayTimer.start();
                    
                    return null;
                });
        }
    }
    
    /**
     * Update AI status progress in real-time
     */
    private void updateAiStatusProgress() {
        if (uiBuilder == null) return;
        
        int totalCount = cryptoList.size();
        int aiSuccessCount = 0;
        int fallbackCount = 0;
        int errorCount = 0;
        int loadingCount = 0;
        
        for (CryptoData crypto : cryptoList) {
            crypto.initializeAiFields();
            String status = crypto.aiStatus;
            if ("AI_SUCCESS".equals(status)) {
                aiSuccessCount++;
            } else if ("FALLBACK".equals(status)) {
                fallbackCount++;
            } else if ("ERROR".equals(status)) {
                errorCount++;
            } else {
                loadingCount++;
            }
        }
        
        uiBuilder.updateAiStatusLabel(loadingCount, aiSuccessCount, fallbackCount, errorCount, totalCount);
    }
    
    /**
     * Update a single cryptocurrency in the table without rebuilding entire table
     */
    private void updateSingleCryptoInTable(CryptoData crypto, int index) {
        if (uiBuilder != null && index < uiBuilder.getTableModel().getRowCount()) {
            uiBuilder.updateTableRow(index, crypto);
        }
    }
    
    /**
     * Update table data after changes
     */
    public void updateTableData() {
        if (isUpdatingTable || uiBuilder == null) return; // Prevent recursive calls
        
        isUpdatingTable = true;
        try {
            // Sort by total value first to maintain ranking
            sortCryptosByTotalValue();
            
            // If the order has changed, rebuild the entire table
            if (uiBuilder.getTableModel().getRowCount() != cryptoList.size()) {
                rebuildTable();
            } else {
                // Check if order has changed by comparing symbols
                boolean orderChanged = false;
                for (int i = 0; i < cryptoList.size() && i < uiBuilder.getTableModel().getRowCount(); i++) {
                    String currentSymbol = uiBuilder.getTableModel().getValueAt(i, 0).toString();
                    String expectedSymbol = cryptoList.get(i).symbol.toUpperCase();
                    if (!currentSymbol.equals(expectedSymbol)) {
                        orderChanged = true;
                        break;
                    }
                }
                
                if (orderChanged) {
                    rebuildTable();
                } else {
                    // Just update the data values
                    for (int i = 0; i < cryptoList.size() && i < uiBuilder.getTableModel().getRowCount(); i++) {
                        CryptoData crypto = cryptoList.get(i);
                        uiBuilder.updateTableRow(i, crypto);
                    }
                }
            }
            
            // Auto-fit column widths after updating data (less frequently)
            if (cryptoList.size() <= 10) { // Only auto-fit for smaller datasets to avoid performance issues
                SwingUtilities.invokeLater(() -> uiBuilder.autoFitColumnWidths());
            }
            
            // Update AI status display
            uiBuilder.updateAiStatus(cryptoList);
        } finally {
            isUpdatingTable = false;
        }
    }
    
    /**
     * Rebuild the entire table when order has changed
     */
    private void rebuildTable() {
        if (uiBuilder != null) {
            uiBuilder.rebuildTable(cryptoList);
        }
    }
    
    /**
     * Add new cryptocurrency to the portfolio (legacy method - calls validation version)
     */
    public void addCrypto(CryptoData newCrypto) {
        // Use the new validation method instead
        addCryptoWithValidation(newCrypto);
    }
    
    /**
     * Remove cryptocurrency from portfolio
     */
    public void removeCrypto(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a cryptocurrency to remove!");
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(null, 
            "Are you sure you want to remove " + cryptoList.get(selectedRow).name + "?",
            "Confirm Removal", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            cryptoList.remove(selectedRow);
            
            // Sort and rebuild the table to maintain ranking by total value
            sortCryptosByTotalValue();
            rebuildTable();
            
            savePortfolioData();
            updatePortfolioValue();
            // Auto-fit column widths after removing row
            if (uiBuilder != null) {
                SwingUtilities.invokeLater(() -> uiBuilder.autoFitColumnWidths());
            }
        }
    }
    
    /**
     * Update portfolio value display
     */
    public void updatePortfolioValue() {
        double totalValue = 0.0;
        double totalProfitLoss = 0.0;
        
        for (CryptoData crypto : cryptoList) {
            totalValue += crypto.getTotalValue();
            totalProfitLoss += crypto.getProfitLoss();
        }
        
        if (uiBuilder != null) {
            uiBuilder.updatePortfolioValue(totalValue, totalProfitLoss);
        }
    }
    
    /**
     * Start auto-refresh timer
     */
    public void startAutoRefresh() {
        // Auto-refresh every 15 seconds to work well with 8s API coordination delays
        // This allows time for any ongoing technical analysis to complete
        refreshTimer = new Timer(15000, e -> {
            // Only refresh if not in the middle of intensive operations
            if (apiCoordinator.canMakeApiCall("PortfolioDataManager", "auto-refresh")) {
                refreshPrices();
            } else {
                LoggerUtil.debug(PortfolioDataManager.class, 
                    "Skipping auto-refresh - technical analysis in progress");
            }
        });
        refreshTimer.start();
    }
    
    /**
     * Stop auto-refresh timer (useful for cleanup)
     */
    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            LoggerUtil.info(PortfolioDataManager.class, "Stopped portfolio auto-refresh timer");
        }
    }
    
    /**
     * Cancel all ongoing operations (timers, background tasks, etc.)
     */
    public void cancelAllOperations() {
        stopAutoRefresh();
        
        // Notify API coordinator that we're stopping intensive operations
        apiCoordinator.notifyIntensiveOperationComplete("PortfolioDataManager");
        
        LoggerUtil.info(PortfolioDataManager.class, "Cancelled all portfolio operations");
    }
    
    /**
     * Check if there are active operations running
     */
    public boolean hasActiveOperations() {
        return (refreshTimer != null && refreshTimer.isRunning());
    }
    
    /**
     * Check if table is currently being updated to prevent infinite recursion
     */
    public boolean isUpdatingTable() {
        return isUpdatingTable;
    }
    
    /**
     * Set the table updating flag
     */
    public void setUpdatingTable(boolean updating) {
        this.isUpdatingTable = updating;
    }
    
    /**
     * Get the cryptocurrency list for direct access
     */
    public List<CryptoData> getCryptoList() {
        return cryptoList;
    }
    
    /**
     * Calculate hash of current portfolio data for comparison
     * Only includes persistent data (excludes transient AI fields)
     */
    private String calculateDataHash() {
        try {
            StringBuilder dataString = new StringBuilder();
            
            // Sort the list by ID to ensure consistent ordering for hashing
            List<CryptoData> sortedList = new ArrayList<>(cryptoList);
            sortedList.sort((c1, c2) -> c1.id.compareTo(c2.id));
            
            for (CryptoData crypto : sortedList) {
                // Only include persistent data fields (exclude transient AI fields and current price)
                dataString.append(crypto.id).append("|")
                          .append(crypto.name).append("|")
                          .append(crypto.symbol).append("|")
                          .append(crypto.expectedPrice).append("|")
                          .append(crypto.expectedEntry).append("|")
                          .append(crypto.targetPrice3Month).append("|")
                          .append(crypto.targetPriceLongTerm).append("|")
                          .append(crypto.holdings).append("|")
                          .append(crypto.avgBuyPrice).append("|");
            }
            
            // Calculate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataString.toString().getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            LoggerUtil.error(PortfolioDataManager.class, "SHA-256 algorithm not available", e);
            // Fallback: use timestamp (will always save)
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    /**
     * Save portfolio data to file
     */
    public void savePortfolioData() {
        // Call the private method with updated visibility or make it public
        this.savePortfolioDataPrivate();
    }
    
    /**
     * Force save portfolio data without comparison (for initial setup or critical saves)
     */
    public void forceSavePortfolioData() {
        // Temporarily clear the hash to force save
        String originalHash = lastSavedDataHash;
        lastSavedDataHash = null;
        savePortfolioDataPrivate();
        // Don't restore the original hash - let the save method set the new one
    }
    
    /**
     * Save portfolio data - internal method
     */
    private void savePortfolioDataPrivate() {
        // Calculate hash of current data
        String currentDataHash = calculateDataHash();
        
        // Compare with last saved hash to avoid unnecessary saves
        if (currentDataHash.equals(lastSavedDataHash)) {
            LoggerUtil.debug(PortfolioDataManager.class, "Portfolio data unchanged, skipping save operation");
            return;
        }
        
        LoggerUtil.debug(PortfolioDataManager.class, "Portfolio data changed, proceeding with save operation");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(cryptoList);
            // Also save as properties backup for compatibility
            savePortfolioDataAsProperties();
            
            // Update the last saved hash after successful save
            lastSavedDataHash = currentDataHash;
            
            LoggerUtil.info(PortfolioDataManager.class, 
                String.format("Portfolio data saved successfully (%d cryptocurrencies)", cryptoList.size()));
        } catch (IOException e) {
            LoggerUtil.error(PortfolioDataManager.class, "Error saving portfolio data", e);
        }
    }
    
    private void savePortfolioDataAsProperties() {
        try (FileOutputStream output = new FileOutputStream(DATA_FILE + ".backup")) {
            Properties props = new Properties();
            
            // Save count of cryptocurrencies
            props.setProperty("crypto.count", String.valueOf(cryptoList.size()));
            
            // Save each cryptocurrency's data
            for (int i = 0; i < cryptoList.size(); i++) {
                CryptoData crypto = cryptoList.get(i);
                String prefix = "crypto." + i + ".";
                
                props.setProperty(prefix + "id", crypto.id);
                props.setProperty(prefix + "name", crypto.name);
                props.setProperty(prefix + "symbol", crypto.symbol);
                props.setProperty(prefix + "expectedPrice", String.valueOf(crypto.expectedPrice));
                props.setProperty(prefix + "expectedEntry", String.valueOf(crypto.expectedEntry));
                props.setProperty(prefix + "targetPrice3Month", String.valueOf(crypto.targetPrice3Month));
                props.setProperty(prefix + "targetPriceLongTerm", String.valueOf(crypto.targetPriceLongTerm));
                props.setProperty(prefix + "holdings", String.valueOf(crypto.holdings));
                props.setProperty(prefix + "avgBuyPrice", String.valueOf(crypto.avgBuyPrice));
                // Note: AI advice is not saved (transient data)
            }
            
            props.store(output, "Crypto Portfolio Data Backup");
            
        } catch (IOException e) {
            System.err.println("Error saving portfolio backup data: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadPortfolioData() {
        LoggerUtil.debug(PortfolioDataManager.class, "Loading portfolio data from file");
        
        cryptoList = new ArrayList<>();
        File file = new File(DATA_FILE);
        
        if (!file.exists()) {
            LoggerUtil.info(PortfolioDataManager.class, "Portfolio data file not found, attempting to load from backup");
            // Try to load from backup properties file
            loadFromPropertiesBackup();
            return;
        }
        
        try (MigrationObjectInputStream ois = new MigrationObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List<?>) {
                cryptoList = (List<CryptoData>) obj;
                // Initialize AI fields for backward compatibility
                for (CryptoData crypto : cryptoList) {
                    crypto.initializeAiFields();
                }
                
                // Calculate and store hash of loaded data for comparison
                lastSavedDataHash = calculateDataHash();
                
                LoggerUtil.info(PortfolioDataManager.class, 
                    String.format("Successfully loaded %d cryptocurrencies from portfolio data file", cryptoList.size()));
                    
                // Force save to update the binary file with new class names
                LoggerUtil.info(PortfolioDataManager.class, "Migrating data file to new package structure");
                forceSavePortfolioData();
            }
        } catch (IOException | ClassNotFoundException e) {
            LoggerUtil.error(PortfolioDataManager.class, "Error loading portfolio data from binary file", e);
            // Try to load from backup properties file
            loadFromPropertiesBackup();
        }
    }
    
    private void loadFromPropertiesBackup() {
        File backupFile = new File(DATA_FILE + ".backup");
        if (!backupFile.exists()) {
            File oldPropsFile = new File("portfolio_data.properties");
            if (oldPropsFile.exists()) {
                loadFromOldPropertiesFile(oldPropsFile);
            }
            return;
        }
        
        try (FileInputStream input = new FileInputStream(backupFile)) {
            Properties props = new Properties();
            props.load(input);
            
            String countStr = props.getProperty("crypto.count");
            if (countStr == null) return;
            
            int count = Integer.parseInt(countStr);
            
            for (int i = 0; i < count; i++) {
                String prefix = "crypto." + i + ".";
                
                String id = props.getProperty(prefix + "id");
                String name = props.getProperty(prefix + "name");
                String symbol = props.getProperty(prefix + "symbol");
                String expectedPriceStr = props.getProperty(prefix + "expectedPrice");
                String expectedEntryStr = props.getProperty(prefix + "expectedEntry");
                String targetPrice3MonthStr = props.getProperty(prefix + "targetPrice3Month");
                String targetPriceLongTermStr = props.getProperty(prefix + "targetPriceLongTerm");
                String holdingsStr = props.getProperty(prefix + "holdings");
                String avgBuyPriceStr = props.getProperty(prefix + "avgBuyPrice");
                // Note: AI advice is no longer loaded from properties (transient data)
                
                if (id != null && name != null && symbol != null && 
                    expectedPriceStr != null && holdingsStr != null && avgBuyPriceStr != null) {
                    
                    double expectedPrice = Double.parseDouble(expectedPriceStr);
                    double expectedEntry = (expectedEntryStr != null) ? Double.parseDouble(expectedEntryStr) : expectedPrice * 0.9;
                    double targetPrice3Month = (targetPrice3MonthStr != null) ? Double.parseDouble(targetPrice3MonthStr) : expectedPrice;
                    double targetPriceLongTerm = (targetPriceLongTermStr != null) ? Double.parseDouble(targetPriceLongTermStr) : expectedPrice;
                    double holdings = Double.parseDouble(holdingsStr);
                    double avgBuyPrice = Double.parseDouble(avgBuyPriceStr);
                    
                    CryptoData crypto = new CryptoData(id, name, symbol, 0.0, expectedPrice, expectedEntry, targetPrice3Month, targetPriceLongTerm, holdings, avgBuyPrice);
                    // AI fields are automatically initialized in constructor (transient data)
                    cryptoList.add(crypto);
                }
            }
            
            // Calculate and store hash of loaded data for comparison
            if (!cryptoList.isEmpty()) {
                lastSavedDataHash = calculateDataHash();
            }
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading portfolio backup data: " + e.getMessage());
            cryptoList = new ArrayList<>();
        }
    }
    
    private void loadFromOldPropertiesFile(File oldFile) {
        try (FileInputStream input = new FileInputStream(oldFile)) {
            Properties props = new Properties();
            props.load(input);
            
            String countStr = props.getProperty("crypto.count");
            if (countStr == null) return;
            
            int count = Integer.parseInt(countStr);
            
            for (int i = 0; i < count; i++) {
                String prefix = "crypto." + i + ".";
                
                String id = props.getProperty(prefix + "id");
                String name = props.getProperty(prefix + "name");
                String symbol = props.getProperty(prefix + "symbol");
                String expectedPriceStr = props.getProperty(prefix + "expectedPrice");
                String expectedEntryStr = props.getProperty(prefix + "expectedEntry");
                String targetPrice3MonthStr = props.getProperty(prefix + "targetPrice3Month");
                String targetPriceLongTermStr = props.getProperty(prefix + "targetPriceLongTerm");
                String holdingsStr = props.getProperty(prefix + "holdings");
                String avgBuyPriceStr = props.getProperty(prefix + "avgBuyPrice");
                
                if (id != null && name != null && symbol != null && 
                    expectedPriceStr != null && holdingsStr != null && avgBuyPriceStr != null) {
                    
                    double expectedPrice = Double.parseDouble(expectedPriceStr);
                    double expectedEntry = (expectedEntryStr != null) ? Double.parseDouble(expectedEntryStr) : expectedPrice * 0.9;
                    double targetPrice3Month = (targetPrice3MonthStr != null) ? Double.parseDouble(targetPrice3MonthStr) : expectedPrice;
                    double targetPriceLongTerm = (targetPriceLongTermStr != null) ? Double.parseDouble(targetPriceLongTermStr) : expectedPrice;
                    double holdings = Double.parseDouble(holdingsStr);
                    double avgBuyPrice = Double.parseDouble(avgBuyPriceStr);
                    
                    CryptoData crypto = new CryptoData(id, name, symbol, 0.0, expectedPrice, expectedEntry, targetPrice3Month, targetPriceLongTerm, holdings, avgBuyPrice);
                    // AI fields are automatically initialized in constructor (transient data)
                    cryptoList.add(crypto);
                }
            }
            
            // Calculate and store hash before saving (since forceSavePortfolioData will be called)
            if (!cryptoList.isEmpty()) {
                lastSavedDataHash = calculateDataHash();
            }
            
            // Force save to binary format and delete old file (migration scenario)
            forceSavePortfolioData();
            oldFile.delete();
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading old portfolio data: " + e.getMessage());
            cryptoList = new ArrayList<>();
        }
    }
    
    /**
     * Custom ObjectInputStream that handles class name migrations due to package reorganization
     */
    private static class MigrationObjectInputStream extends ObjectInputStream {
        
        public MigrationObjectInputStream(InputStream in) throws IOException {
            super(in);
        }
        
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String className = desc.getName();
            
            // Handle migration from old package structure to new package structure
            switch (className) {
                case "CryptoData":
                    LoggerUtil.info(PortfolioDataManager.class, "Migrating CryptoData class from root package to model package");
                    return Class.forName("model.CryptoData");
                case "PortfolioRebalanceRecommendation":
                    LoggerUtil.info(PortfolioDataManager.class, "Migrating PortfolioRebalanceRecommendation class to model package");
                    return Class.forName("model.PortfolioRebalanceRecommendation");
                default:
                    // For all other classes, use the default behavior
                    return super.resolveClass(desc);
            }
        }
    }
}