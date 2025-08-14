package data;

import cache.CoinGeckoApiCache;
import model.CryptoData;
import model.WatchlistData;
import model.EntryStatus;
import service.ApiCoordinationService;
import service.TechnicalAnalysisService;
import util.LoggerUtil;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.swing.Timer;
import javax.swing.SwingUtilities;

/**
 * Data Manager for Cryptocurrency Watchlist
 * Handles CRUD operations, persistence, and technical analysis for watchlist items
 */
public class WatchlistDataManager {
    
    private static final String WATCHLIST_FILE = "src/data/watchlist.dat";
    private static final String BACKUP_FILE = "src/data/watchlist_backup.dat";
    private String TAG = "WatchlistDataManager";
    private List<WatchlistData> watchlist;
    private final Object lock = new Object();
    
    // Analysis state management
    private volatile boolean isAnalyzing = false;
    private volatile long lastAnalysisTime = 0;
    private static final long MIN_ANALYSIS_INTERVAL = 15000; // Reduced to 15 seconds
    
    // API coordination
    private final ApiCoordinationService apiCoordinator = ApiCoordinationService.getInstance();
    
    // Callback interface for UI updates
    public interface TechnicalAnalysisCallback {
        void onTechnicalAnalysisComplete(WatchlistData item);
        void onAllAnalysisComplete();
    }
    
    private TechnicalAnalysisCallback uiCallback;

    public WatchlistDataManager() {
        this.watchlist = new ArrayList<>();
        LoggerUtil.debug(WatchlistDataManager.class, "Created empty watchlist, now loading from file...");
        loadWatchlist();
    }
    
    /**
     * Set callback for UI updates when technical analysis completes
     */
    public void setTechnicalAnalysisCallback(TechnicalAnalysisCallback callback) {
        this.uiCallback = callback;
        LoggerUtil.debug(WatchlistDataManager.class, "UI callback set for technical analysis updates");
    }

    /**
     * Add cryptocurrency to watchlist
     */
    public boolean addToWatchlist(String id, String name, double currentPrice) {
        synchronized (lock) {
            // Check if already exists
            if (isInWatchlist(id)) {
                LoggerUtil.warning(WatchlistDataManager.class, 
                    "id " + id + " already exists in watchlist");
                return false;
            }
            
            // Need to provide all required constructor parameters
            WatchlistData watchlistItem = new WatchlistData(
                id,          // Use proper CoinGecko ID
                name,                 // name  
                name, // id
                currentPrice,         // currentPrice
                currentPrice * 0.95,  // expectedEntry (5% below current)
                currentPrice * 1.20,  // targetPrice3Month (20% above)
                currentPrice * 1.50   // targetPriceLongTerm (50% above)
            );
            watchlist.add(watchlistItem);
            
            LoggerUtil.info(WatchlistDataManager.class, 
                String.format("Added %s to watchlist with CoinGecko ID: %s", name, id));
            
            // Perform initial technical analysis
            analyzeWatchlistItem(watchlistItem);
            
            saveWatchlist();
            return true;
        }
    }
    
    /**
     * Remove cryptocurrency from watchlist
     */
    public boolean removeFromWatchlist(String symbol) {
        synchronized (lock) {
            boolean removed = watchlist.removeIf(item -> item.getSymbol().equals(symbol));
            
            if (removed) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Removed " + symbol + " from watchlist");
                saveWatchlist();
            }
            
            return removed;
        }
    }
    
    /**
     * Update price for watchlist item
     */
    public void updatePrice(String symbol, double newPrice) {
        synchronized (lock) {
            Optional<WatchlistData> item = watchlist.stream()
                .filter(w -> w.getSymbol().equals(symbol))
                .findFirst();
                
            if (item.isPresent()) {
                WatchlistData watchlistItem = item.get();
                double oldPrice = watchlistItem.getCurrentPrice();
                watchlistItem.updatePrice(newPrice);
                
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Updated %s price: %.2f -> %.2f", 
                        symbol, oldPrice, newPrice));
                
                saveWatchlist();
            }
        }
    }
    
    /**
     * Get all watchlist items
     */
    public List<WatchlistData> getWatchlist() {
        synchronized (lock) {
            LoggerUtil.debug(WatchlistDataManager.class, ">>> getWatchlist() called, watchlist.size() = " + watchlist.size());
            return new ArrayList<>(watchlist);
        }
    }
    
    /**
     * Get watchlist item by symbol
     */
    public Optional<WatchlistData> getWatchlistItem(String symbol) {
        synchronized (lock) {
            return watchlist.stream()
                .filter(item -> item.getSymbol().equals(symbol))
                .findFirst();
        }
    }
    
    /**
     * Check if symbol is in watchlist
     */
    public boolean isInWatchlist(String symbol) {
        synchronized (lock) {
            return watchlist.stream()
                .anyMatch(item -> item.getSymbol().equals(symbol));
        }
    }
    
    /**
     * Get watchlist items with good entry opportunities
     */
    public List<WatchlistData> getGoodEntryOpportunities() {
        synchronized (lock) {
            return watchlist.stream()
                .filter(item -> item.getEntryOpportunityScore() >= 7.0)
                .sorted((a, b) -> Double.compare(b.getEntryOpportunityScore(), a.getEntryOpportunityScore()))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Get watchlist items by entry status
     */
    public List<WatchlistData> getWatchlistByStatus(EntryStatus status) {
        synchronized (lock) {
            return watchlist.stream()
                .filter(item -> item.getEntryStatus() == status)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Update entry target for watchlist item
     */
    public void updateEntryTarget(String symbol, double targetPrice, String notes) {
        synchronized (lock) {
            Optional<WatchlistData> item = watchlist.stream()
                .filter(w -> w.getSymbol().equals(symbol))
                .findFirst();
                
            if (item.isPresent()) {
                WatchlistData watchlistItem = item.get();
                watchlistItem.setTargetEntryPrice(targetPrice);
                watchlistItem.setNotes(notes);
                
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Updated entry target for %s: %.2f", symbol, targetPrice));
                
                saveWatchlist();
            }
        }
    }
    
    /**
     * Perform technical analysis on watchlist item
     */
    public CompletableFuture<Void> analyzeWatchlistItem(WatchlistData watchlistItem) {
        return TechnicalAnalysisService.analyzeEntry(watchlistItem.toCryptoData())
            .thenAccept(indicators -> {
                synchronized (lock) {
                    watchlistItem.setTechnicalIndicators(indicators);
                    watchlistItem.updateEntryOpportunity();
                    
                    LoggerUtil.info(WatchlistDataManager.class, 
                        String.format("Technical analysis completed for %s - Score: %.1f", 
                            watchlistItem.getSymbol(), watchlistItem.getEntryOpportunityScore()));
                    
                    // Notify UI if callback is set
                    if (uiCallback != null) {
                        uiCallback.onTechnicalAnalysisComplete(watchlistItem);
                    }
                }
            })
            .exceptionally(ex -> {
                LoggerUtil.error(WatchlistDataManager.class, 
                    "Failed to analyze " + watchlistItem.getSymbol(), ex);
                watchlistItem.setTechnicalAnalysisError();
                
                // Still notify UI even on error to update the display
                if (uiCallback != null) {
                    LoggerUtil.debug(WatchlistDataManager.class, 
                        "Calling UI callback for " + watchlistItem.getSymbol() + " technical analysis error");
                    uiCallback.onTechnicalAnalysisComplete(watchlistItem);
                }
                return null;
            });
    }
    
    /**
     * Analyze all watchlist items sequentially to avoid rate limiting
     */
    public CompletableFuture<Void> analyzeAllWatchlistItems() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            
            // Prevent duplicate analysis calls within minimum interval
            if (isAnalyzing) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Analysis already in progress, skipping duplicate request");
                return CompletableFuture.completedFuture(null);
            }
            
            if (currentTime - lastAnalysisTime < MIN_ANALYSIS_INTERVAL) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Analysis requested too soon, waiting for cooldown period");
                return CompletableFuture.completedFuture(null);
            }
            
            isAnalyzing = true;
            lastAnalysisTime = currentTime;
        }
        
        // Notify API coordinator about intensive operations
        apiCoordinator.notifyIntensiveOperationStart("WatchlistDataManager");
        
        return CompletableFuture.runAsync(() -> {
            try {
                analyzeWatchlistItemsSequentially(0);
                // Note: completion notification is now handled in analyzeWatchlistItemsSequentially
                // when all items are actually processed, not just when this method returns
            } catch (Exception e) {
                LoggerUtil.error(WatchlistDataManager.class, "Error in sequential analysis", e);
                synchronized (lock) {
                    isAnalyzing = false;
                }
                apiCoordinator.notifyIntensiveOperationComplete("WatchlistDataManager");
            }
        });
    }
    
    /**
     * Analyze watchlist items one by one with delays to prevent rate limiting
     * This method runs asynchronously to avoid blocking the UI
     */
    private void analyzeWatchlistItemsSequentially(int index) {
        // Check if analysis was cancelled
        synchronized (lock) {
            if (!isAnalyzing) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Analysis was cancelled, stopping sequential analysis");
                return;
            }
            
            if (index >= watchlist.size()) {
                // All items processed - NOW we can mark completion
                saveWatchlist();
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Completed analysis for all " + watchlist.size() + " watchlist items");
                
                // Notify UI that all analysis is complete
                if (uiCallback != null) {
                    SwingUtilities.invokeLater(() -> uiCallback.onAllAnalysisComplete());
                }
                
                // Mark analysis as complete and notify API coordinator
                isAnalyzing = false;
                apiCoordinator.notifyIntensiveOperationComplete("WatchlistDataManager");
                return;
            }
        }
        
        WatchlistData item;
        synchronized (lock) {
            if (index >= watchlist.size()) return; // Double check in case list changed
            item = watchlist.get(index);
        }
        
        LoggerUtil.info(WatchlistDataManager.class, 
            "Analyzing watchlist item " + item.getSymbol() + " (" + (index + 1) + "/" + watchlist.size() + ")");
        
        // Analyze current item asynchronously
        analyzeWatchlistItem(item)
            .thenRunAsync(() -> {
                // Schedule next item analysis with delay - using background thread to avoid UI blocking
                CompletableFuture.delayedExecutor(12, TimeUnit.SECONDS).execute(() -> {
                    analyzeWatchlistItemsSequentially(index + 1);
                });
            })
            .exceptionally(ex -> {
                LoggerUtil.error(WatchlistDataManager.class, 
                    "Error analyzing " + item.getSymbol() + ": " + ex.getMessage(), ex);
                // Continue with next item even if current one fails
                CompletableFuture.delayedExecutor(12, TimeUnit.SECONDS).execute(() -> {
                    analyzeWatchlistItemsSequentially(index + 1);
                });
                return null;
            });
    }
    
    /**
     * Import watchlist from file
     */
    public boolean importWatchlist(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                LoggerUtil.error(WatchlistDataManager.class, 
                    "Import file not found: " + filePath);
                return false;
            }
            
            List<WatchlistData> importedList = new ArrayList<>();
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                @SuppressWarnings("unchecked")
                List<WatchlistData> loaded = (List<WatchlistData>) ois.readObject();
                importedList.addAll(loaded);
            }
            
            synchronized (lock) {
                // Merge with existing watchlist, avoiding duplicates
                for (WatchlistData imported : importedList) {
                    if (!isInWatchlist(imported.getSymbol())) {
                        watchlist.add(imported);
                    }
                }
            }
            
            saveWatchlist();
            LoggerUtil.info(WatchlistDataManager.class, 
                "Successfully imported " + importedList.size() + " watchlist items");
            
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(WatchlistDataManager.class, 
                "Failed to import watchlist from " + filePath, e);
            return false;
        }
    }
    
    /**
     * Export watchlist to file
     */
    public boolean exportWatchlist(String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            
            synchronized (lock) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                    oos.writeObject(new ArrayList<>(watchlist));
                }
            }
            
            LoggerUtil.info(WatchlistDataManager.class, 
                "Successfully exported " + watchlist.size() + " watchlist items to " + filePath);
            
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(WatchlistDataManager.class, 
                "Failed to export watchlist to " + filePath, e);
            return false;
        }
    }
    
    /**
     * Clear all watchlist items
     */
    public void clearWatchlist() {
        synchronized (lock) {
            int count = watchlist.size();
            watchlist.clear();
            saveWatchlist();
            
            LoggerUtil.info(WatchlistDataManager.class, 
                "Cleared " + count + " items from watchlist");
        }
    }
    
    /**
     * Get watchlist statistics
     */
    public Map<String, Object> getWatchlistStatistics() {
        synchronized (lock) {
            Map<String, Object> stats = new HashMap<>();
            
            stats.put("totalItems", watchlist.size());
            stats.put("excellentOpportunities", 
                watchlist.stream().mapToLong(w -> w.getEntryOpportunityScore() >= 9.0 ? 1 : 0).sum());
            stats.put("goodOpportunities", 
                watchlist.stream().mapToLong(w -> w.getEntryOpportunityScore() >= 7.0 && w.getEntryOpportunityScore() < 9.0 ? 1 : 0).sum());
            stats.put("averageOpportunityScore", 
                watchlist.stream().mapToDouble(WatchlistData::getEntryOpportunityScore).average().orElse(0.0));
            
            // Status distribution
            Map<EntryStatus, Long> statusCount = watchlist.stream()
                .collect(Collectors.groupingBy(WatchlistData::getEntryStatus, Collectors.counting()));
            stats.put("statusDistribution", statusCount);
            
            return stats;
        }
    }
    
    /**
     * Save watchlist to file
     */
    private void saveWatchlist() {
        try {
            // Create backup
            File dataFile = new File(WATCHLIST_FILE);
            if (dataFile.exists()) {
                File backupFile = new File(BACKUP_FILE);
                dataFile.renameTo(backupFile);
            }
            
            // Ensure directory exists
            dataFile.getParentFile().mkdirs();
            
            // Save current watchlist
            synchronized (lock) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
                    oos.writeObject(watchlist);
                }
            }
            
            LoggerUtil.info(WatchlistDataManager.class, 
                "Watchlist saved successfully (" + watchlist.size() + " items)");
            
        } catch (Exception e) {
            LoggerUtil.error(WatchlistDataManager.class, "Failed to save watchlist", e);
            
            // Restore backup if save failed
            try {
                File backupFile = new File(BACKUP_FILE);
                if (backupFile.exists()) {
                    backupFile.renameTo(new File(WATCHLIST_FILE));
                }
            } catch (Exception restoreEx) {
                LoggerUtil.error(WatchlistDataManager.class, "Failed to restore backup", restoreEx);
            }
        }
    }
    
    /**
     * Load watchlist from file
     */
    private void loadWatchlist() {
        LoggerUtil.info(WatchlistDataManager.class, TAG + " loadWatchlist() called");
        try {
            File dataFile = new File(WATCHLIST_FILE);
            LoggerUtil.debug(WatchlistDataManager.class, ">>> Checking for watchlist file: " + WATCHLIST_FILE);
            
            if (dataFile.exists()) {
                LoggerUtil.debug(WatchlistDataManager.class, ">>> Watchlist file exists, loading data...");
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
                    @SuppressWarnings("unchecked")
                    List<WatchlistData> loaded = (List<WatchlistData>) ois.readObject();
                    
                    synchronized (lock) {
                        watchlist.clear();
                        watchlist.addAll(loaded);
                    }
                    
                    LoggerUtil.info(WatchlistDataManager.class, 
                        "Loaded " + watchlist.size() + " watchlist items");
                }
            } else {
                LoggerUtil.info(WatchlistDataManager.class, 
                    ">>> No existing watchlist file found, starting with empty watchlist");
            }
            
        } catch (Exception e) {
            LoggerUtil.error(WatchlistDataManager.class, "Failed to load watchlist", e);
            
            // Try to load backup
            LoggerUtil.debug(WatchlistDataManager.class, ">>> Attempting to load backup file...");
            try {
                File backupFile = new File(BACKUP_FILE);
                if (backupFile.exists()) {
                    LoggerUtil.debug(WatchlistDataManager.class, ">>> Backup file exists, loading...");
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(backupFile))) {
                        @SuppressWarnings("unchecked")
                        List<WatchlistData> loaded = (List<WatchlistData>) ois.readObject();
                        
                        synchronized (lock) {
                            watchlist.clear();
                            watchlist.addAll(loaded);
                        }
                        
                        LoggerUtil.info(WatchlistDataManager.class, 
                            "Loaded " + watchlist.size() + " watchlist items from backup");
                    }
                }
            } catch (Exception backupEx) {
                LoggerUtil.error(WatchlistDataManager.class, "Failed to load backup", backupEx);
                watchlist = new ArrayList<>();
            }
        }
    }
    
    /**
     * Add watchlist item directly
     */
    public boolean addWatchlistItem(WatchlistData item) {
        synchronized (lock) {
            if (isInWatchlist(item.symbol)) {
                LoggerUtil.warning(WatchlistDataManager.class, 
                    "Symbol " + item.symbol + " already exists in watchlist");
                return false;
            }
            
            watchlist.add(item);
            
            LoggerUtil.info(WatchlistDataManager.class, 
                "Added " + item.symbol + " to watchlist");
            
            // Perform initial technical analysis
            analyzeWatchlistItem(item);
            
            saveWatchlistData();
            return true;
        }
    }
    
    /**
     * Remove watchlist item by ID
     */
    public boolean removeWatchlistItem(String id) {
        synchronized (lock) {
            boolean removed = watchlist.removeIf(item -> item.id.equals(id));
            
            if (removed) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Removed item with ID " + id + " from watchlist");
                saveWatchlistData();
            }
            
            return removed;
        }
    }
    
    /**
     * Get all watchlist items (alternative method name)
     */
    public List<WatchlistData> getWatchlistItems() {
        return getWatchlist();
    }
    
    /**
     * Refresh prices and analysis for all items sequentially with duplicate prevention
     */
    public void refreshPricesAndAnalysis() {
        LoggerUtil.debug(WatchlistDataManager.class, ">>> refreshPricesAndAnalysis() called");
        
        synchronized (lock) {
            // Check if already analyzing
            if (isAnalyzing) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Technical analysis already in progress, skipping refresh request");
                return;
            }
        }
        
        // Use the sequential analysis method with duplicate prevention
        analyzeAllWatchlistItems()
            .thenRun(() -> {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "Refresh completed for all " + watchlist.size() + " items");
            })
            .exceptionally(ex -> {
                LoggerUtil.error(WatchlistDataManager.class, "Error during refresh", ex);
                return null;
            });
    }
    
    /**
     * Save watchlist data (alternative method name)
     */
    public void saveWatchlistData() {
        saveWatchlist();
    }
    
    /**
     * Import watchlist with exception handling (alternative method signature)
     */
    public void importWatchlistWithException(String filePath) throws Exception {
        if (!importWatchlist(filePath)) {
            throw new Exception("Failed to import watchlist from " + filePath);
        }
    }
    
    /**
     * Export watchlist (alternative method signature that throws exception)  
     */
    public void exportWatchlistWithException(String filePath) throws Exception {
        if (!exportWatchlist(filePath)) {
            throw new Exception("Failed to export watchlist to " + filePath);
        }
    }
    
    // ============ PORTFOLIO MANAGEMENT METHODS ============
    
    /**
     * Get all items that have holdings (portfolio items)
     */
    public List<WatchlistData> getPortfolioItems() {
        synchronized (lock) {
            return watchlist.stream()
                .filter(WatchlistData::hasHoldings)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Get all watchlist-only items (no holdings)
     */
    public List<WatchlistData> getWatchlistOnlyItems() {
        synchronized (lock) {
            return watchlist.stream()
                .filter(WatchlistData::isWatchlistOnly)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Get total portfolio value
     */
    public double getTotalPortfolioValue() {
        synchronized (lock) {
            return watchlist.stream()
                .filter(WatchlistData::hasHoldings)
                .mapToDouble(WatchlistData::getTotalValue)
                .sum();
        }
    }
    
    /**
     * Get total portfolio profit/loss
     */
    public double getTotalProfitLoss() {
        synchronized (lock) {
            return watchlist.stream()
                .filter(WatchlistData::hasHoldings)
                .mapToDouble(WatchlistData::getProfitLoss)
                .sum();
        }
    }
    
    /**
     * Add holdings to existing item or create new portfolio item
     */
    public boolean addPortfolioHoldings(String symbol, String name, double amount, double buyPrice) {
        synchronized (lock) {
            Optional<WatchlistData> existing = watchlist.stream()
                .filter(item -> item.getSymbol().equals(symbol))
                .findFirst();
            
            if (existing.isPresent()) {
                // Add to existing item
                WatchlistData item = existing.get();
                item.addHoldings(amount, buyPrice);
                
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Added %.4f %s holdings @ $%.2f", amount, symbol, buyPrice));
            } else {
                // Create new portfolio item
                WatchlistData newItem = new WatchlistData(
                    symbol.toLowerCase(),
                    name,
                    symbol.toUpperCase(),
                    buyPrice, // Use buy price as current price initially
                    buyPrice * 0.95, // Default entry target 5% below
                    buyPrice * 1.20, // Default 3-month target
                    buyPrice * 1.50, // Default long-term target
                    amount,
                    buyPrice
                );
                
                watchlist.add(newItem);
                
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Created new portfolio item: %.4f %s @ $%.2f", amount, symbol, buyPrice));
                
                // Analyze new item
                analyzeWatchlistItem(newItem);
            }
            
            saveWatchlistData();
            return true;
        }
    }
    
    /**
     * Convert watchlist item to portfolio item by adding holdings
     */
    public boolean convertToPortfolioItem(String symbol, double amount, double buyPrice) {
        return addPortfolioHoldings(symbol, null, amount, buyPrice);
    }
    
    /**
     * Convert portfolio item back to watchlist-only (remove all holdings)
     */
    public boolean convertToWatchlistOnly(String symbol) {
        synchronized (lock) {
            Optional<WatchlistData> item = watchlist.stream()
                .filter(w -> w.getSymbol().equals(symbol) && w.hasHoldings())
                .findFirst();
            
            if (item.isPresent()) {
                WatchlistData portfolioItem = item.get();
                portfolioItem.setHoldings(0, 0);
                
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Converted %s to watchlist-only", symbol));
                
                saveWatchlistData();
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Get portfolio statistics
     */
    public Map<String, Object> getPortfolioStatistics() {
        synchronized (lock) {
            Map<String, Object> stats = new HashMap<>();
            
            List<WatchlistData> portfolioItems = getPortfolioItems();
            List<WatchlistData> watchlistItems = getWatchlistOnlyItems();
            
            stats.put("totalItems", watchlist.size());
            stats.put("portfolioItems", portfolioItems.size());
            stats.put("watchlistOnlyItems", watchlistItems.size());
            stats.put("totalPortfolioValue", getTotalPortfolioValue());
            stats.put("totalProfitLoss", getTotalProfitLoss());
            
            if (getTotalPortfolioValue() > 0) {
                stats.put("totalProfitLossPercentage", getTotalProfitLoss() / (getTotalPortfolioValue() - getTotalProfitLoss()));
            } else {
                stats.put("totalProfitLossPercentage", 0.0);
            }
            
            // Entry opportunities in watchlist-only items
            long goodOpportunities = watchlistItems.stream()
                .filter(item -> item.getEntryOpportunityScore() >= 70.0)
                .count();
            stats.put("goodEntryOpportunities", goodOpportunities);
            
            return stats;
        }
    }
    
    /**
     * Import portfolio data from PortfolioDataManager into watchlist
     * This creates the unified view where portfolio items appear as watchlist items with holdings
     */
    public boolean importPortfolioData(List<CryptoData> portfolioData) {
        if (portfolioData == null || portfolioData.isEmpty()) {
            LoggerUtil.info(WatchlistDataManager.class, "No portfolio data to import");
            return true;
        }
        
        synchronized (lock) {
            int imported = 0;
            int updated = 0;
            
            for (CryptoData cryptoData : portfolioData) {
                String id = cryptoData.id;
                
                // Check if already exists in watchlist
                Optional<WatchlistData> existing = watchlist.stream()
                    .filter(item -> item.getID().equals(id))
                    .findFirst();
                
                if (existing.isPresent()) {
                    // Update existing item with portfolio data
                    WatchlistData item = existing.get();
                    
                    // Update prices and holdings
                    item.currentPrice = cryptoData.currentPrice;
                    if (cryptoData.holdings > 0) {
                        item.setHoldings(cryptoData.holdings, cryptoData.avgBuyPrice);
                        updated++;
                    }
                    
                    // Update targets if not set
                    if (item.expectedEntry == 0) {
                        item.expectedEntry = cryptoData.expectedPrice > 0 ? cryptoData.expectedPrice : cryptoData.currentPrice * 0.95;
                    }
                    if (item.targetPrice3Month == 0) {
                        item.targetPrice3Month = cryptoData.targetPrice3Month > 0 ? cryptoData.targetPrice3Month : cryptoData.currentPrice * 1.20;
                    }
                    if (item.targetPriceLongTerm == 0) {
                        item.targetPriceLongTerm = cryptoData.targetPriceLongTerm > 0 ? cryptoData.targetPriceLongTerm : cryptoData.currentPrice * 1.50;
                    }
                    
                } else {
                    // Create new watchlist item from portfolio data
                    WatchlistData newItem = new WatchlistData(
                        cryptoData.id,
                        cryptoData.name,
                        cryptoData.symbol,
                        cryptoData.currentPrice,
                        cryptoData.expectedPrice > 0 ? cryptoData.expectedPrice : cryptoData.currentPrice * 0.95,
                        cryptoData.targetPrice3Month > 0 ? cryptoData.targetPrice3Month : cryptoData.currentPrice * 1.20,
                        cryptoData.targetPriceLongTerm > 0 ? cryptoData.targetPriceLongTerm : cryptoData.currentPrice * 1.50,
                        cryptoData.holdings,
                        cryptoData.avgBuyPrice
                    );
                    
                    // Copy technical analysis if available
                    if (cryptoData.technicalIndicators != null) {
                        newItem.setTechnicalIndicators(cryptoData.technicalIndicators);
                    }
                    
                    watchlist.add(newItem);
                    imported++;
                }
            }
            
            LoggerUtil.info(WatchlistDataManager.class, 
                String.format("Portfolio import complete: %d new items, %d updated items", imported, updated));
            
            if (imported > 0 || updated > 0) {
                saveWatchlistData();
                return true;
            }
            
            return false;
        }
    }

    /**
     * Refresh only prices for all watchlist items without technical analysis
     * This is much faster than refreshPricesAndAnalysis() and suitable for frequent updates
     */
    public void refreshPricesOnly() {
        LoggerUtil.debug(WatchlistDataManager.class, "Starting price-only refresh for watchlist");
        
        synchronized (lock) {
            if (watchlist.isEmpty()) {
                LoggerUtil.debug(WatchlistDataManager.class, "No watchlist items to update");
                return;
            }
            
            // Check cache for all watchlist items first
            List<WatchlistData> itemsNeedingUpdate = new ArrayList<>();
            int cachedPrices = 0;
            
            for (WatchlistData item : watchlist) {
                Double cachedPrice = CoinGeckoApiCache.getCachedPrice(item.id);
                if (cachedPrice != null) {
                    double oldPrice = item.getCurrentPrice();
                    item.updatePrice(cachedPrice);
                    if (Math.abs(oldPrice - cachedPrice) > 0.01) {
                        cachedPrices++;
                        LoggerUtil.debug(WatchlistDataManager.class, 
                            String.format("Using cached price for %s: $%.4f", item.getSymbol(), cachedPrice));
                    }
                } else {
                    itemsNeedingUpdate.add(item);
                }
            }
            
            if (cachedPrices > 0) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Used cached prices for %d watchlist items", cachedPrices));
            }
            
            // If all prices are cached, we're done
            if (itemsNeedingUpdate.isEmpty()) {
                LoggerUtil.info(WatchlistDataManager.class, "All watchlist prices served from cache");
                if (cachedPrices > 0) {
                    saveWatchlist(); // Save updated prices
                }
                return;
            }
            
            // Check with API coordinator before making bulk price request for remaining items
            if (!apiCoordinator.requestApiCall("WatchlistDataManager", "price-only update")) {
                LoggerUtil.info(WatchlistDataManager.class, 
                    "API coordination denied price-only fetch - technical analysis in progress");
                return;
            }

            try {
                // Build API URL for items that need updates
                StringBuilder cryptoIds = new StringBuilder();
                for (int i = 0; i < itemsNeedingUpdate.size(); i++) {
                    if (i > 0) cryptoIds.append(",");
                    cryptoIds.append(itemsNeedingUpdate.get(i).id);
                }
                
                String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + 
                               cryptoIds.toString() + "&vs_currencies=usd";
                
                LoggerUtil.debug(WatchlistDataManager.class, "Price API Request URL: " + apiUrl);
                
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 429) {
                    LoggerUtil.warning(WatchlistDataManager.class, 
                        "Rate limited during price fetch - will retry later");
                    return;
                }
                
                if (responseCode != 200) {
                    LoggerUtil.warning(WatchlistDataManager.class, 
                        "Price API returned response code: " + responseCode);
                    return;
                }
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                int updatedPrices = 0;
                
                for (WatchlistData item : itemsNeedingUpdate) {
                    if (jsonResponse.has(item.id)) {
                        org.json.JSONObject cryptoData = jsonResponse.getJSONObject(item.id);
                        if (cryptoData.has("usd")) {
                            double oldPrice = item.getCurrentPrice();
                            double newPrice = cryptoData.getDouble("usd");
                            item.updatePrice(newPrice);
                            
                            // Cache the new price
                            CoinGeckoApiCache.cachePrice(item.id, newPrice);
                            
                            if (Math.abs(oldPrice - newPrice) > 0.01) { // Only log significant changes
                                updatedPrices++;
                                LoggerUtil.debug(WatchlistDataManager.class, 
                                    String.format("Price updated for %s: $%.4f -> $%.4f", 
                                        item.getSymbol(), oldPrice, newPrice));
                            }
                        }
                    }
                }
                
                LoggerUtil.info(WatchlistDataManager.class, 
                    String.format("Price-only refresh completed: %d price updates from API, %d from cache", 
                                 updatedPrices, cachedPrices));
                
                if (updatedPrices > 0 || cachedPrices > 0) {
                    saveWatchlist(); // Save updated prices
                }
                
            } catch (Exception e) {
                LoggerUtil.error(WatchlistDataManager.class, "Failed to refresh watchlist prices", e);
            }
        }
    }

    /**
     * Cancel all ongoing analysis tasks
     */
    public void cancelAllAnalysis() {
        synchronized (lock) {
            if (isAnalyzing) {
                LoggerUtil.info(WatchlistDataManager.class, "Cancelling ongoing technical analysis");
                isAnalyzing = false;
                apiCoordinator.notifyIntensiveOperationComplete("WatchlistDataManager");
            }
        }
    }

    /**
     * Check if currently analyzing
     */
    public boolean isAnalyzing() {
        synchronized (lock) {
            return isAnalyzing;
        }
    }
    
    /**
     * Get the API coordinator instance (used by WatchlistPanel for coordination checks)
     */
    public ApiCoordinationService getApiCoordinator() {
        return apiCoordinator;
    }
    
    /**
     * Check if the current right panel in the main application is a WatchlistPanel
     * This helps avoid unnecessary data updates when the WatchlistPanel is not visible
     */
    public boolean isWatchlistPanelCurrentlyActive() {
        try {
            // Look for the main application window
            javax.swing.JFrame mainFrame = null;
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                if (window instanceof javax.swing.JFrame && 
                    ((javax.swing.JFrame) window).getTitle().contains("Crypto Portfolio Manager")) {
                    mainFrame = (javax.swing.JFrame) window;
                    break;
                }
            }
            
            if (mainFrame == null) {
                LoggerUtil.debug(WatchlistDataManager.class, "Main application frame not found");
                return false;
            }
            
            // Find the content panel (should be in BorderLayout.CENTER)
            java.awt.Container contentPane = mainFrame.getContentPane();
            return findWatchlistPanelInContainer(contentPane);
            
        } catch (Exception e) {
            LoggerUtil.debug(WatchlistDataManager.class, 
                "Error checking if WatchlistPanel is active: " + e.getMessage());
            // If we can't determine, assume it's active to be safe
            return true;
        }
    }
    
    /**
     * Recursively search for WatchlistPanel in container hierarchy
     */
    private boolean findWatchlistPanelInContainer(java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            // Check if this component is a WatchlistPanel
            if (component.getClass().getName().contains("WatchlistPanel")) {
                LoggerUtil.debug(WatchlistDataManager.class, "Found active WatchlistPanel");
                return true;
            }
            
            // If it's a container, recursively search
            if (component instanceof java.awt.Container) {
                if (findWatchlistPanelInContainer((java.awt.Container) component)) {
                    return true;
                }
            }
        }
        return false;
    }
}
