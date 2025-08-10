import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
        loadPortfolioData(); // Load saved data first
        if (cryptoList.isEmpty()) {
            initializeCryptoList(); // Only initialize defaults if no saved data
        }
    }
    
    /**
     * Set the UI builder reference for callbacks
     */
    public void setUIBuilder(PortfolioUIBuilder uiBuilder) {
        this.uiBuilder = uiBuilder;
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
        savePortfolioData(); // Save initial data
    }
    
    /**
     * Load initial prices and populate table
     */
    public void loadInitialPrices() {
        SwingUtilities.invokeLater(() -> {
            // Sort by total value before displaying
            sortCryptosByTotalValue();
            
            if (uiBuilder != null) {
                uiBuilder.getTableModel().setRowCount(0);
                for (CryptoData crypto : cryptoList) {
                    uiBuilder.addCryptoToTable(crypto);
                }
                updatePortfolioValue(); // Update portfolio value display
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
        try {
            StringBuilder cryptoIds = new StringBuilder();
            for (int i = 0; i < cryptoList.size(); i++) {
                if (i > 0) cryptoIds.append(",");
                cryptoIds.append(cryptoList.get(i).id);
            }
            
            String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + 
                           cryptoIds.toString() + "&vs_currencies=usd";
            
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
            for (CryptoData crypto : cryptoList) {
                if (jsonResponse.has(crypto.id)) {
                    JSONObject cryptoData = jsonResponse.getJSONObject(crypto.id);
                    if (cryptoData.has("usd")) {
                        crypto.currentPrice = cryptoData.getDouble("usd");
                    }
                }
            }
            
        } catch (Exception e) {
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
        if (uiBuilder != null) {
            uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Updating AI advice...");
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fetchAiAdvice();
                return null;
            }
            
            @Override
            protected void done() {
                updateTableData();
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("📊 Portfolio Status: Ready");
                }
            }
        };
        worker.execute();
    }

    /**
     * Fetch AI advice for all cryptocurrencies asynchronously
     */
    private void fetchAiAdvice() {
        try {
            // Fetch AI advice for each cryptocurrency asynchronously
            for (CryptoData crypto : cryptoList) {
                AiAdviceService.getAdviceAsync(crypto)
                    .thenAccept(advice -> {
                        crypto.setAiAdvice(advice);
                        // Update table on EDT
                        SwingUtilities.invokeLater(() -> {
                            updateTableData();
                        });
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Failed to get AI advice for " + crypto.symbol + ": " + throwable.getMessage());
                        // Set fallback advice
                        crypto.setAiAdvice("Hold Position");
                        SwingUtilities.invokeLater(() -> {
                            updateTableData();
                        });
                        return null;
                    });
            }
        } catch (Exception e) {
            System.err.println("Error initiating AI advice fetch: " + e.getMessage());
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
     * Add new cryptocurrency to the portfolio
     */
    public void addCrypto(CryptoData newCrypto) {
        cryptoList.add(newCrypto);
        
        // Sort and rebuild the table to maintain ranking by total value
        sortCryptosByTotalValue();
        rebuildTable();
        
        savePortfolioData();
        updatePortfolioValue();
        refreshPrices();
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
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(cryptoList);
            // Also save as properties backup for compatibility
            savePortfolioDataAsProperties();
        } catch (IOException e) {
            System.err.println("Error saving portfolio data: " + e.getMessage());
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
        cryptoList = new ArrayList<>();
        File file = new File(DATA_FILE);
        
        if (!file.exists()) {
            // Try to load from backup properties file
            loadFromPropertiesBackup();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List<?>) {
                cryptoList = (List<CryptoData>) obj;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading portfolio data: " + e.getMessage());
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