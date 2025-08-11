import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
    
    // Reference to UI builder for updates
    private PortfolioUIBuilder uiBuilder;
    private Timer refreshTimer;
    
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
        cryptoList.add(new CryptoData("ethereum", "Ethereum", "ETH", 0.0, 3000.0, 2700.0, 3500.0, 5000.0, 0.0, 0.0));
        cryptoList.add(new CryptoData("binancecoin", "Binance Coin", "BNB", 0.0, 400.0, 360.0, 450.0, 600.0, 0.0, 0.0));
        cryptoList.add(new CryptoData("cardano", "Cardano", "ADA", 0.0, 1.0, 0.9, 1.2, 2.0, 0.0, 0.0));
        cryptoList.add(new CryptoData("solana", "Solana", "SOL", 0.0, 100.0, 90.0, 120.0, 200.0, 0.0, 0.0));
        cryptoList.add(new CryptoData("dogecoin", "Dogecoin", "DOGE", 0.0, 0.25, 0.22, 0.30, 0.50, 0.0, 0.0));
        cryptoList.add(new CryptoData("polkadot", "Polkadot", "DOT", 0.0, 25.0, 22.5, 30.0, 50.0, 0.0, 0.0));
        cryptoList.add(new CryptoData("chainlink", "Chainlink", "LINK", 0.0, 20.0, 18.0, 25.0, 40.0, 0.0, 0.0));
        
        // Set initial AI status to LOADING for all cryptocurrencies
        for (CryptoData crypto : cryptoList) {
            crypto.aiStatus = "LOADING";
            crypto.aiAdvice = "Loading...";
        }
        
        savePortfolioData(); // Save initial data
    }
    
    /**
     * Load initial prices and populate table
     */
    public void loadInitialPrices() {
        SwingUtilities.invokeLater(() -> {
            // Sort by total value before displaying
            sortCryptosByTotalValue();
            
            // Reset all cryptocurrencies to LOADING state for a fresh start
            for (CryptoData crypto : cryptoList) {
                crypto.aiStatus = "LOADING";
                crypto.aiAdvice = "Loading...";
                crypto.isAiGenerated = false;
            }
            
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
     * Fetch cryptocurrency prices from API
     */
    private void fetchCryptoPrices() {
        LoggerUtil.debug(PortfolioDataManager.class, "Fetching cryptocurrency prices from API");
        
        try {
            StringBuilder cryptoIds = new StringBuilder();
            for (int i = 0; i < cryptoList.size(); i++) {
                if (i > 0) cryptoIds.append(",");
                cryptoIds.append(cryptoList.get(i).id);
            }
            
            String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + 
                           cryptoIds.toString() + "&vs_currencies=usd";
            
            LoggerUtil.debug(PortfolioDataManager.class, "API Request URL: " + apiUrl);
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            int updatedPrices = 0;
            
            for (CryptoData crypto : cryptoList) {
                if (jsonResponse.has(crypto.id)) {
                    JSONObject cryptoData = jsonResponse.getJSONObject(crypto.id);
                    if (cryptoData.has("usd")) {
                        double oldPrice = crypto.currentPrice;
                        crypto.currentPrice = cryptoData.getDouble("usd");
                        
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
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioDataManager.class, "Failed to fetch cryptocurrency prices", e);
            SwingUtilities.invokeLater(() -> {
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Error fetching prices");
                }
            });
        }
    }
    
    /**
     * Refresh AI advice for all cryptocurrencies (called separately from price refresh)
     */
    public void refreshAiAdvice() {
        // Reset all cryptocurrencies to LOADING state before starting AI refresh
        for (CryptoData crypto : cryptoList) {
            crypto.aiStatus = "LOADING";
            crypto.aiAdvice = "Loading...";
            crypto.isAiGenerated = false;
        }
        
        if (uiBuilder != null) {
            uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Updating AI advice...");
            // Initialize AI status label for progress tracking - all cryptos are now loading
            uiBuilder.updateAiStatusLabel(cryptoList.size(), 0, 0, 0, cryptoList.size());
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fetchAiAdvice();
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
     */
    private void fetchAiAdvice() {
        try {
            // Fetch AI advice sequentially with delays to respect API rate limits
            fetchAiAdviceSequentially(0);
        } catch (Exception e) {
            System.err.println("Error initiating AI advice fetch: " + e.getMessage());
        }
    }
    
    /**
     * Fetch AI advice sequentially for each cryptocurrency with delay
     */
    private void fetchAiAdviceSequentially(int index) {
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
                    fetchAiAdviceSequentially(index + 1);
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
                    fetchAiAdviceSequentially(index + 1);
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
                
                return null;
            });
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
        // Auto-refresh every 10 seconds
        refreshTimer = new Timer(10000, e -> refreshPrices());
        refreshTimer.start();
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
     * Save portfolio data to file
     */
    public void savePortfolioData() {
        // Call the private method with updated visibility or make it public
        this.savePortfolioDataPrivate();
    }
    
    /**
     * Save portfolio data - internal method
     */
    private void savePortfolioDataPrivate() {
        LoggerUtil.debug(PortfolioDataManager.class, "Saving portfolio data to file");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(cryptoList);
            // Also save as properties backup for compatibility
            savePortfolioDataAsProperties();
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
                props.setProperty(prefix + "aiAdvice", crypto.aiAdvice != null ? crypto.aiAdvice : "Loading...");
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
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List<?>) {
                cryptoList = (List<CryptoData>) obj;
                // Initialize AI fields for backward compatibility
                for (CryptoData crypto : cryptoList) {
                    crypto.initializeAiFields();
                }
                LoggerUtil.info(PortfolioDataManager.class, 
                    String.format("Successfully loaded %d cryptocurrencies from portfolio data file", cryptoList.size()));
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
                String aiAdviceStr = props.getProperty(prefix + "aiAdvice");
                
                if (id != null && name != null && symbol != null && 
                    expectedPriceStr != null && holdingsStr != null && avgBuyPriceStr != null) {
                    
                    double expectedPrice = Double.parseDouble(expectedPriceStr);
                    double expectedEntry = (expectedEntryStr != null) ? Double.parseDouble(expectedEntryStr) : expectedPrice * 0.9;
                    double targetPrice3Month = (targetPrice3MonthStr != null) ? Double.parseDouble(targetPrice3MonthStr) : expectedPrice;
                    double targetPriceLongTerm = (targetPriceLongTermStr != null) ? Double.parseDouble(targetPriceLongTermStr) : expectedPrice;
                    double holdings = Double.parseDouble(holdingsStr);
                    double avgBuyPrice = Double.parseDouble(avgBuyPriceStr);
                    
                    CryptoData crypto = new CryptoData(id, name, symbol, 0.0, expectedPrice, expectedEntry, targetPrice3Month, targetPriceLongTerm, holdings, avgBuyPrice);
                    crypto.setAiAdvice(aiAdviceStr != null ? aiAdviceStr : "Loading...");
                    crypto.initializeAiFields(); // Ensure AI fields are properly initialized
                    cryptoList.add(crypto);
                }
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
                    crypto.setAiAdvice("Loading..."); // Set default for old files
                    crypto.initializeAiFields(); // Ensure AI fields are properly initialized
                    cryptoList.add(crypto);
                }
            }
            
            // Save to binary format and delete old file
            savePortfolioData();
            oldFile.delete();
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading old portfolio data: " + e.getMessage());
            cryptoList = new ArrayList<>();
        }
    }
}