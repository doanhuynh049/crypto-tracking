package ui.panel;

import cache.CoinGeckoApiCache;
import data.WatchlistDataManager;
import model.WatchlistData;
import model.TechnicalIndicators.TrendDirection;
import ui.CleanupablePanel;
import ui.dialog.AddWatchlistItemDialog;
import ui.dialog.TechnicalAnalysisDetailDialog;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Watchlist Panel for tracking cryptocurrency entry opportunities
 * Distinguished from portfolio holdings - focuses on technical analysis and entry signals
 */
public class WatchlistPanel extends JPanel {
    
    // UI Components
    private DefaultTableModel tableModel;
    private JTable watchlistTable;
    private JButton refreshButton;
    private JButton addItemButton;
    private JButton removeItemButton;
    private JLabel statusLabel;
    private JLabel watchlistStatsLabel;
    private JLabel cacheStatusLabel;
    private DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("+#0.00%;-#0.00%");
    
    // Reference to data manager
    private WatchlistDataManager dataManager;
    
    // Auto-refresh timer for price updates
    private Timer priceRefreshTimer;
    
    // Debouncing for updateTableData calls
    private Timer updateTableDataTimer;
    private final int UPDATE_DELAY_MS = 300; // 300ms debounce delay
    
    // Modern color scheme matching the main app
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);
    
    public WatchlistPanel() {
        LoggerUtil.debug(WatchlistPanel.class, "=== Initializing Watchlist Panel ===");
        
        try {
            // Initialize data manager
            dataManager = new WatchlistDataManager();
            
            // Set callback for technical analysis updates
            dataManager.setTechnicalAnalysisCallback(new WatchlistDataManager.TechnicalAnalysisCallback() {
                @Override
                public void onTechnicalAnalysisComplete(WatchlistData item) {
                    LoggerUtil.debug(WatchlistPanel.class, 
                        "=== Technical Analysis Complete Callback for " + item.getSymbol() + " ===");
                    
                    SwingUtilities.invokeLater(() -> {
                        // updateTableData();
                        updateStats();
                        statusLabel.setText("✅ " + item.getSymbol() + " analyzed");
                        statusLabel.setForeground(SUCCESS_COLOR);
                    });
                }
                
                @Override
                public void onAllAnalysisComplete() {
                    LoggerUtil.debug(WatchlistPanel.class, 
                        "=== All Technical Analysis Complete Callback ===");
                    
                    SwingUtilities.invokeLater(() -> {
                        updateTableData();
                        updateStats();
                        statusLabel.setText("✅ All analysis complete");
                        statusLabel.setForeground(SUCCESS_COLOR);
                    });
                }
            });
            
            // Setup the UI
            setupUI();
            
            // Load initial data
            refreshWatchlistData();
            
            LoggerUtil.info(WatchlistPanel.class, "=== Watchlist Panel initialized successfully ===");
        } catch (Exception e) {
            LoggerUtil.error(WatchlistPanel.class, "Failed to initialize Watchlist Panel", e);
        }
    }
    
    /**
     * Setup the main UI layout and components
     */
    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        // Create header panel
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Create table panel
        add(createTablePanel(), BorderLayout.CENTER);
        
        // Create control panel
        add(createControlPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Create the header panel with title and stats
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        // Title section
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(BACKGROUND_COLOR);
        
        JLabel titleLabel = new JLabel("🎯 Crypto Watchlist");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        JLabel subtitleLabel = new JLabel("Comprehensive technical analysis with RSI, MACD, trend, volume, and support/resistance levels");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        
        titlePanel.add(titleLabel);
        
        // Stats section
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        statsPanel.setBackground(BACKGROUND_COLOR);
        
        watchlistStatsLabel = new JLabel("📊 Items: 0 | Buy Signals: 0 | Technical Analysis: Ready");
        watchlistStatsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        watchlistStatsLabel.setForeground(PRIMARY_COLOR);
        
        statusLabel = new JLabel("✅ Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(SUCCESS_COLOR);
        
        // Cache Status label
        cacheStatusLabel = new JLabel("⚡ Cache: Initializing...");
        cacheStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cacheStatusLabel.setForeground(new Color(76, 175, 80));
        
        statsPanel.add(watchlistStatsLabel);
        statsPanel.add(statusLabel);
        statsPanel.add(cacheStatusLabel);
        
        // Layout
        JPanel leftSection = new JPanel();
        leftSection.setLayout(new BoxLayout(leftSection, BoxLayout.Y_AXIS));
        leftSection.setBackground(BACKGROUND_COLOR);
        leftSection.add(titlePanel);
        leftSection.add(Box.createVerticalStrut(5));
        leftSection.add(subtitleLabel);
        leftSection.add(Box.createVerticalStrut(10));
        leftSection.add(statsPanel);
        
        headerPanel.add(leftSection, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    /**
     * Create the main table panel for watchlist items
     */
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BACKGROUND_COLOR);
        tablePanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        
        // Create table model - Comprehensive technical analysis for buy/sell decisions
        String[] columnNames = {
            "Symbol", "Name", "Price", "RSI", "MACD", "SMA Cross", "Volume", 
            "Support/Resistance", "Trend", "Entry Quality", "Overall Signal", "Days"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // No editable columns - read-only technical analysis view
                return false;
            }
        };
        
        // Create table
        watchlistTable = new JTable(tableModel);
        setupTable();
        
        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(watchlistTable);
        scrollPane.setBorder(new LineBorder(DIVIDER_COLOR, 1));
        scrollPane.setBackground(SURFACE_COLOR);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        return tablePanel;
    }
    
    /**
     * Setup table appearance and behavior
     */
    private void setupTable() {
        watchlistTable.setRowHeight(50);
        watchlistTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        watchlistTable.setBackground(SURFACE_COLOR);
        watchlistTable.setForeground(TEXT_PRIMARY);
        watchlistTable.setSelectionBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 50));
        watchlistTable.setSelectionForeground(TEXT_PRIMARY);
        watchlistTable.setGridColor(new Color(240, 240, 240));
        watchlistTable.setShowGrid(true);
        watchlistTable.setIntercellSpacing(new Dimension(1, 1));
        
        // Set column widths
        watchlistTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Set minimum column widths - Comprehensive technical analysis focused
        watchlistTable.getColumnModel().getColumn(0).setMinWidth(60);   // Symbol
        watchlistTable.getColumnModel().getColumn(1).setMinWidth(100);  // Name
        watchlistTable.getColumnModel().getColumn(2).setMinWidth(80);   // Price
        watchlistTable.getColumnModel().getColumn(3).setMinWidth(60);   // RSI
        watchlistTable.getColumnModel().getColumn(4).setMinWidth(70);   // MACD
        watchlistTable.getColumnModel().getColumn(5).setMinWidth(80);   // SMA Cross
        watchlistTable.getColumnModel().getColumn(6).setMinWidth(80);   // Volume
        watchlistTable.getColumnModel().getColumn(7).setMinWidth(120);  // Support/Resistance
        watchlistTable.getColumnModel().getColumn(8).setMinWidth(70);   // Trend
        watchlistTable.getColumnModel().getColumn(9).setMinWidth(90);   // Entry Quality
        watchlistTable.getColumnModel().getColumn(10).setMinWidth(100); // Overall Signal
        watchlistTable.getColumnModel().getColumn(11).setMinWidth(50);  // Days
        
        // Enhanced header styling
        JTableHeader header = watchlistTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(245, 245, 247));
        header.setForeground(TEXT_PRIMARY);
        header.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 8, 12, 8)
        ));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 45));
        
        // Custom cell renderer
        watchlistTable.setDefaultRenderer(Object.class, new WatchlistTableCellRenderer());
        
        // Add mouse listener for clickable columns
        watchlistTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = watchlistTable.columnAtPoint(e.getPoint());
                int row = watchlistTable.rowAtPoint(e.getPoint());
                
                if (row >= 0 && row < dataManager.getWatchlistItems().size()) {
                    WatchlistData item = dataManager.getWatchlistItems().get(row);
                    
                    if (column == 7 || column == 9 || column == 10) { // Support/Resistance, Entry Quality, Overall Signal columns
                        showTechnicalAnalysisDialog(item);
                    }
                }
            }
        });
    }
    
    /**
     * Create the control panel with action buttons
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        controlPanel.setBackground(BACKGROUND_COLOR);
        controlPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        // Add Item button
        addItemButton = createStyledButton("➕ Add to Watchlist", PRIMARY_COLOR);
        addItemButton.addActionListener(e -> showAddItemDialog());
        
        // Remove Item button
        removeItemButton = createStyledButton("🗑️ Remove Item", DANGER_COLOR);
        removeItemButton.addActionListener(e -> removeSelectedItem());
        
        // Refresh button
        refreshButton = createStyledButton("🔄 Refresh All", SUCCESS_COLOR);
        refreshButton.addActionListener(e -> refreshWatchlistData());
        
        // Technical Analysis refresh button
        JButton techAnalysisButton = createStyledButton("📊 Refresh Technical Analysis", PRIMARY_COLOR);
        techAnalysisButton.addActionListener(e -> refreshTechnicalAnalysisOnly());
        
        // Filter buttons for different signals
        JButton buySignalsButton = createStyledButton("🔥 Show Buy Signals", new Color(76, 175, 80));
        buySignalsButton.addActionListener(e -> filterBySignal("BUY"));
        
        JButton allSignalsButton = createStyledButton("📋 Show All", TEXT_SECONDARY);
        allSignalsButton.addActionListener(e -> showAllItems());
        
        // Import/Export buttons
        JButton importButton = createStyledButton("📥 Import", TEXT_SECONDARY);
        importButton.addActionListener(e -> importWatchlist());
        
        JButton exportButton = createStyledButton("📤 Export", TEXT_SECONDARY);
        exportButton.addActionListener(e -> exportWatchlist());
        
        controlPanel.add(addItemButton);
        controlPanel.add(removeItemButton);
        controlPanel.add(refreshButton);
        controlPanel.add(techAnalysisButton);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(buySignalsButton);
        controlPanel.add(allSignalsButton);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(importButton);
        controlPanel.add(exportButton);
        
        return controlPanel;
    }
    
    /**
     * Create a styled button with the given text and color
     */
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setBorder(new CompoundBorder(
            new LineBorder(color.darker(), 1),
            new EmptyBorder(8, 16, 8, 16)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    /**
     * Custom table cell renderer for watchlist table
     */
    private class WatchlistTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Set default properties
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setHorizontalAlignment(SwingConstants.LEFT);
            
            if (!isSelected) {
                // Alternating row colors
                if (row % 2 == 0) {
                    comp.setBackground(SURFACE_COLOR);
                } else {
                    comp.setBackground(new Color(248, 250, 252));
                }
            }
            
            // Get the watchlist item
            if (row < dataManager.getWatchlistItems().size()) {
                WatchlistData item = dataManager.getWatchlistItems().get(row);
                
                // Color coding based on column for enhanced technical analysis
                switch (column) {
                    case 3: // RSI - Momentum Indicator
                        if (item.hasTechnicalAnalysis()) {
                            double rsi = item.technicalIndicators.getRsi();
                            if (rsi < 30) {
                                setForeground(SUCCESS_COLOR); // Oversold - Strong Buy signal
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else if (rsi < 40) {
                                setForeground(new Color(139, 195, 74)); // Light green - Buy signal
                            } else if (rsi > 70) {
                                setForeground(DANGER_COLOR); // Overbought - Sell signal
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else if (rsi > 60) {
                                setForeground(WARNING_COLOR); // Warning - Potential sell
                            } else {
                                setForeground(TEXT_PRIMARY); // Neutral
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 4: // MACD - Trend and Momentum
                        if (item.hasTechnicalAnalysis()) {
                            double macd = item.technicalIndicators.getMacd();
                            double macdSignal = item.technicalIndicators.getMacdSignal();
                            if (macd > macdSignal) {
                                setForeground(SUCCESS_COLOR); // Bullish crossover
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else {
                                setForeground(DANGER_COLOR); // Bearish crossover
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 5: // SMA Cross - Trend Confirmation
                        if (item.hasTechnicalAnalysis()) {
                            double sma10 = item.technicalIndicators.getSma10();
                            double sma50 = item.technicalIndicators.getSma50();
                            if (sma10 > sma50) {
                                setForeground(SUCCESS_COLOR); // Golden Cross
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else {
                                setForeground(DANGER_COLOR); // Death Cross
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 6: // Volume - Confirmation Indicator
                        if (item.hasTechnicalAnalysis()) {
                            boolean volumeConfirmation = item.technicalIndicators.isVolumeConfirmation();
                            double volumeRatio = item.technicalIndicators.getVolumeRatio();
                            if (volumeConfirmation && volumeRatio > 1.5) {
                                setForeground(SUCCESS_COLOR); // Strong volume confirmation
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else if (volumeConfirmation) {
                                setForeground(new Color(139, 195, 74)); // Moderate volume
                            } else {
                                setForeground(TEXT_PRIMARY); // Weak volume
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 7: // Support/Resistance - Market Structure
                        if (item.hasTechnicalAnalysis()) {
                            // Get fresh price from cache (non-blocking)
                            Double cachedPrice = cache.CoinGeckoApiCache.getCachedPrice(item.id);
                            double currentPrice = cachedPrice != null ? cachedPrice : item.getCurrentPrice();
                            
                            double support = item.technicalIndicators.getSupportLevel();
                            double resistance = item.technicalIndicators.getResistanceLevel();
                            double supportDistance = Math.abs(currentPrice - support) / support;
                            double resistanceDistance = Math.abs(resistance - currentPrice) / resistance;
                            
                            if (supportDistance < 0.02) { // Within 2% of support
                                setForeground(SUCCESS_COLOR); // Near support - buy opportunity
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else if (resistanceDistance < 0.02) { // Within 2% of resistance
                                setForeground(DANGER_COLOR); // Near resistance - sell opportunity
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else {
                                setForeground(PRIMARY_COLOR); // Between levels
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setForeground(PRIMARY_COLOR); // Clickable
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        break;
                        
                    case 8: // Trend Direction
                        if (item.hasTechnicalAnalysis()) {
                            TrendDirection trend = item.technicalIndicators.getTrend();
                            switch (trend) {
                                case BULLISH:
                                    setForeground(SUCCESS_COLOR);
                                    setFont(getFont().deriveFont(Font.BOLD));
                                    break;
                                case BEARISH:
                                    setForeground(DANGER_COLOR);
                                    setFont(getFont().deriveFont(Font.BOLD));
                                    break;
                                case NEUTRAL:
                                    setForeground(WARNING_COLOR);
                                    break;
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 9: // Entry Quality - clickable
                        if (item.hasTechnicalAnalysis()) {
                            double entryScore = item.technicalIndicators.getEntryQualityScore();
                            if (entryScore >= 80) {
                                setForeground(SUCCESS_COLOR);
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else if (entryScore >= 60) {
                                setForeground(new Color(139, 195, 74));
                            } else if (entryScore >= 40) {
                                setForeground(WARNING_COLOR);
                            } else {
                                setForeground(DANGER_COLOR);
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setForeground(PRIMARY_COLOR); // Clickable
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        break;
                        
                    case 10: // Overall Signal - clickable
                        if (item.hasTechnicalAnalysis()) {
                            String signal = item.entrySignal;
                            switch (signal) {
                                case "STRONG_BUY":
                                    setForeground(SUCCESS_COLOR);
                                    setFont(getFont().deriveFont(Font.BOLD));
                                    break;
                                case "BUY":
                                    setForeground(new Color(139, 195, 74));
                                    setFont(getFont().deriveFont(Font.BOLD));
                                    break;
                                case "NEUTRAL":
                                    setForeground(WARNING_COLOR);
                                    break;
                                case "WAIT":
                                    setForeground(new Color(255, 152, 0));
                                    break;
                                case "AVOID":
                                    setForeground(DANGER_COLOR);
                                    setFont(getFont().deriveFont(Font.BOLD));
                                    break;
                                default:
                                    setForeground(TEXT_PRIMARY);
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setForeground(PRIMARY_COLOR); // Clickable
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        break;
                        
                    case 2: // Price column
                        setHorizontalAlignment(SwingConstants.RIGHT);
                        setForeground(TEXT_PRIMARY);
                        break;
                        
                    default:
                        setForeground(TEXT_PRIMARY);
                }
            }
            
            return comp;
        }
    }
    
    /**
     * Refresh watchlist data and update table
     */
    public void refreshWatchlistData() {
        LoggerUtil.debug(WatchlistPanel.class, ">>> refreshWatchlistData() called");
        
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("🔄 Refreshing...");
            statusLabel.setForeground(WARNING_COLOR);
        });
        
        // Refresh in background thread
        new Thread(() -> {
            try {
                LoggerUtil.debug(WatchlistPanel.class, ">>> Calling dataManager.refreshPricesAndAnalysis()");
                
                SwingUtilities.invokeLater(() -> {
                    dataManager.refreshPricesAndAnalysis(this);
                    updateTableData();
                    updateStats();
                    updateCacheStatus();
                    statusLabel.setText("✅ Updated");
                    statusLabel.setForeground(SUCCESS_COLOR);
                });
                
            } catch (Exception e) {
                LoggerUtil.error(WatchlistPanel.class, "Failed to refresh watchlist data", e);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ Error");
                    statusLabel.setForeground(DANGER_COLOR);
                });
            }
        }).start();
    }
    
    /**
     * Refresh only technical analysis without updating prices
     */
    private void refreshTechnicalAnalysisOnly() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("🔄 Refreshing Technical Analysis...");
            statusLabel.setForeground(WARNING_COLOR);
        });
        
        new Thread(() -> {
            try {
                dataManager.analyzeAllWatchlistItems(this).join();
                
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    updateStats();
                    updateCacheStatus();
                    statusLabel.setText("✅ Technical Analysis Updated");
                    statusLabel.setForeground(SUCCESS_COLOR);
                });
                
            } catch (Exception e) {
                LoggerUtil.error(WatchlistPanel.class, "Failed to refresh technical analysis", e);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ Technical Analysis Error");
                    statusLabel.setForeground(DANGER_COLOR);
                });
            }
        }).start();
    }
    
    /**
     * Filter watchlist items by signal type
     */
    private void filterBySignal(String signalType) {
        // Clear existing rows
        tableModel.setRowCount(0);
        
        List<WatchlistData> items = dataManager.getWatchlistItems();
        for (WatchlistData item : items) {
            boolean shouldShow = false;
            
            if (signalType.equals("BUY") && item.hasTechnicalAnalysis()) {
                // Show items with strong buy signals based on multiple indicators
                double rsi = item.technicalIndicators.getRsi();
                double macd = item.technicalIndicators.getMacd();
                double macdSignal = item.technicalIndicators.getMacdSignal();
                boolean volumeConfirmation = item.technicalIndicators.isVolumeConfirmation();
                String entrySignal = item.entrySignal;
                
                shouldShow = (rsi < 40) || // RSI buy signal
                           (macd > macdSignal && volumeConfirmation) || // MACD with volume
                           entrySignal.equals("STRONG_BUY") || entrySignal.equals("BUY");
            }
            
            if (shouldShow) {
                Object[] rowData = {
                    item.symbol,
                    item.name,
                    priceFormat.format(item.currentPrice),
                    getTechnicalValue(item, "RSI"),
                    getTechnicalValue(item, "MACD"),
                    getTechnicalValue(item, "SMA_CROSS"),
                    getTechnicalValue(item, "VOLUME"),
                    getTechnicalValue(item, "SUPPORT_RESISTANCE"),
                    getTechnicalValue(item, "TREND"),
                    getTechnicalValue(item, "ENTRY_QUALITY"),
                    getTechnicalValue(item, "OVERALL_SIGNAL"),
                    item.getDaysSinceAdded() + "d"
                };
                tableModel.addRow(rowData);
            }
        }
        
        // Update status
        statusLabel.setText("🔍 Filtered: " + signalType + " signals (" + tableModel.getRowCount() + " items)");
        statusLabel.setForeground(PRIMARY_COLOR);
    }
    
    /**
     * Show all watchlist items (remove filters)
     */
    private void showAllItems() {
        LoggerUtil.info(WatchlistPanel.class, "showAllItems");
        updateTableData();
        statusLabel.setText("📋 Showing all items");
        statusLabel.setForeground(SUCCESS_COLOR);
    }
    
    /**
     * Update table with current watchlist data
     */
    private void updateTableData() {
        LoggerUtil.info(WatchlistPanel.class, "Clearing existing table rows");
        tableModel.setRowCount(0);
        
        // Add watchlist items
        List<WatchlistData> items = dataManager.getWatchlistItems();
        LoggerUtil.debug(WatchlistPanel.class, ">>> Retrieved " + items.size() + " watchlist items from dataManager");
        
        for (WatchlistData item : items) {
            // Get fresh price from cache or API instead of using item.currentPrice
            double currentPrice = getFreshPrice(item);
            
            LoggerUtil.info(WatchlistPanel.class, ">>> Processing item: " + item.symbol + 
                " | Fresh Price: " + currentPrice + " | Stored Price: " + item.currentPrice +
                " | Has Technical Analysis: " + item.hasTechnicalAnalysis());
            
            Object[] rowData = {
                item.symbol,
                item.name,
                priceFormat.format(currentPrice),
                // Momentum Indicators
                getTechnicalValue(item, "RSI"),
                getTechnicalValue(item, "MACD"),
                // Trend Indicators
                getTechnicalValue(item, "SMA_CROSS"),
                // Volume Indicators
                getTechnicalValue(item, "VOLUME"),
                // Market Structure
                getTechnicalValue(item, "SUPPORT_RESISTANCE"),
                // Trend Direction
                getTechnicalValue(item, "TREND"),
                // Entry Quality Assessment
                getTechnicalValue(item, "ENTRY_QUALITY"),
                // Overall Signal
                getTechnicalValue(item, "OVERALL_SIGNAL"),
                item.getDaysSinceAdded() + "d"
            };
            tableModel.addRow(rowData);
        }
    }

    /**
     * Update table data for a specific row index with the latest WatchlistData
     * @param rowIndex the index of the row to update
     */
    public void updateRowData(int rowIndex) {
        List<WatchlistData> items = dataManager.getWatchlistItems();
        if (rowIndex < 0 || rowIndex >= items.size()) {
            LoggerUtil.warning(WatchlistPanel.class, "updateRowData: Invalid row index " + rowIndex);
            return;
        }
        WatchlistData item = items.get(rowIndex);
        double currentPrice = getFreshPrice(item);
        Object[] rowData = {
            item.symbol,
            item.name,
            priceFormat.format(currentPrice),
            getTechnicalValue(item, "RSI"),
            getTechnicalValue(item, "MACD"),
            getTechnicalValue(item, "SMA_CROSS"),
            getTechnicalValue(item, "VOLUME"),
            getTechnicalValue(item, "SUPPORT_RESISTANCE"),
            getTechnicalValue(item, "TREND"),
            getTechnicalValue(item, "ENTRY_QUALITY"),
            getTechnicalValue(item, "OVERALL_SIGNAL"),
            item.getDaysSinceAdded() + "d"
        };
        for (int col = 0; col < rowData.length; col++) {
            tableModel.setValueAt(rowData[col], rowIndex, col);
        }
        LoggerUtil.info(WatchlistPanel.class, "updateRowData: Updated row " + rowIndex + " for symbol " + item.symbol);
    }
    
    /**
     * Update watchlist statistics
     */
    private void updateStats() {
        Map<String, Object> stats = dataManager.getPortfolioStatistics();
        int totalItems = (Integer) stats.get("totalItems");
        long goodOpportunities = (Long) stats.get("goodEntryOpportunities");
        
        // Count items with strong buy signals based on multiple indicators
        List<WatchlistData> items = dataManager.getWatchlistItems();
        long strongBuySignals = items.stream()
            .filter(item -> item.hasTechnicalAnalysis() && 
                           (item.technicalIndicators.getRsi() < 30 || // Oversold RSI
                            (item.technicalIndicators.getMacd() > item.technicalIndicators.getMacdSignal() && // MACD bullish
                             item.technicalIndicators.isVolumeConfirmation()))) // Volume confirmation
            .count();
        
        long technicalReady = items.stream()
            .filter(item -> item.hasTechnicalAnalysis())
            .count();
        
        // Count different signal strengths
        long bullishSignals = items.stream()
            .filter(item -> item.hasTechnicalAnalysis() && 
                           item.technicalIndicators.getTrend().toString().equals("BULLISH"))
            .count();
        
        String statsText = String.format("📊 Items: %d | Buy Signals: %d | Bullish: %d | Technical Ready: %d/%d", 
                                        totalItems, strongBuySignals, bullishSignals, technicalReady, totalItems);
        
        watchlistStatsLabel.setText(statsText);
    }
    
    /**
     * Show dialog to add new watchlist item
     */
    private void showAddItemDialog() {
        // Create and show add item dialog
        AddWatchlistItemDialog dialog = new AddWatchlistItemDialog((JFrame) SwingUtilities.getWindowAncestor(this), dataManager);
        dialog.setVisible(true);
        
        // Refresh after dialog closes
        if (dialog.isItemAdded()) {
            refreshWatchlistData();
        }
    }
    
    /**
     * Remove selected watchlist item
     */
    private void removeSelectedItem() {
        int selectedRow = watchlistTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < dataManager.getWatchlistItems().size()) {
            WatchlistData item = dataManager.getWatchlistItems().get(selectedRow);
            
            int result = JOptionPane.showConfirmDialog(
                this,
                "Remove " + item.symbol + " from watchlist?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                dataManager.removeWatchlistItem(item.id);
                refreshWatchlistData();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.");
        }
    }
    
    /**
     * Show technical analysis detail dialog
     */
    private void showTechnicalAnalysisDialog(WatchlistData item) {
        TechnicalAnalysisDetailDialog dialog = new TechnicalAnalysisDetailDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), item);
        dialog.setVisible(true);
    }
    
    /**
     * Import watchlist from file
     */
    private void importWatchlist() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                dataManager.importWatchlistWithException(fileChooser.getSelectedFile().getAbsolutePath());
                refreshWatchlistData();
                JOptionPane.showMessageDialog(this, "Watchlist imported successfully!");
            } catch (Exception e) {
                LoggerUtil.error(WatchlistPanel.class, "Failed to import watchlist", e);
                JOptionPane.showMessageDialog(this, "Failed to import watchlist: " + e.getMessage(),
                                            "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Export watchlist to file
     */
    private void exportWatchlist() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        fileChooser.setSelectedFile(new java.io.File("watchlist_export.json"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                dataManager.exportWatchlistWithException(fileChooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Watchlist exported successfully!");
            } catch (Exception e) {
                LoggerUtil.error(WatchlistPanel.class, "Failed to export watchlist", e);
                JOptionPane.showMessageDialog(this, "Failed to export watchlist: " + e.getMessage(),
                                            "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Get the data manager for external access
     */
    public WatchlistDataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * Get technical indicator value for display in table with comprehensive analysis
     */
    private String getTechnicalValue(WatchlistData item, String indicator) {
        LoggerUtil.debug(WatchlistPanel.class, "getTechnicalValue() called for " + item.symbol + 
            " | indicator: " + indicator + " | hasTechnicalAnalysis: " + item.hasTechnicalAnalysis());
        
        if (!item.hasTechnicalAnalysis()) {
            LoggerUtil.debug(WatchlistPanel.class, "No technical analysis for " + item.symbol + ", returning '-'");
            return "-";
        }
        
        switch (indicator) {
            case "RSI":
                double rsi = item.technicalIndicators.getRsi();
                String rsiSignal = "";
                if (rsi < 30) rsiSignal = " 🔥"; // Strong buy
                else if (rsi < 40) rsiSignal = " 🟢"; // Buy
                else if (rsi > 70) rsiSignal = " 🔴"; // Strong sell
                else if (rsi > 60) rsiSignal = " ⚠️"; // Warning
                return String.format("%.1f%s", rsi, rsiSignal);
                
            case "MACD":
                double macd = item.technicalIndicators.getMacd();
                double macdSignal = item.technicalIndicators.getMacdSignal();
                String signal = macd > macdSignal ? " 📈" : " 📉";
                return String.format("%.4f%s", macd - macdSignal, signal);
                
            case "SMA_CROSS":
                double sma10 = item.technicalIndicators.getSma10();
                double sma50 = item.technicalIndicators.getSma50();
                if (sma10 > sma50) {
                    return "🌟 Golden"; // Golden Cross - bullish
                } else {
                    return "💀 Death"; // Death Cross - bearish
                }
                
            case "VOLUME":
                boolean volumeConfirmation = item.technicalIndicators.isVolumeConfirmation();
                double volumeRatio = item.technicalIndicators.getVolumeRatio();
                String volumeIcon = "";
                if (volumeConfirmation && volumeRatio > 1.5) volumeIcon = " 🔥";
                else if (volumeConfirmation) volumeIcon = " 🟢";
                return String.format("%.1fx%s", volumeRatio, volumeIcon);
                
            case "SUPPORT_RESISTANCE":
                // Get fresh price from cache (non-blocking) or fallback to stored price
                Double cachedPrice = cache.CoinGeckoApiCache.getCachedPrice(item.id);
                double currentPrice = cachedPrice != null ? cachedPrice : item.getCurrentPrice();
                
                double support = item.technicalIndicators.getSupportLevel();
                double resistance = item.technicalIndicators.getResistanceLevel();
                double supportDistance = Math.abs(currentPrice - support) / support * 100;
                double resistanceDistance = Math.abs(resistance - currentPrice) / resistance * 100;
                
                if (supportDistance < 2.0) {
                    return String.format("S: %.1f%%", supportDistance);
                } else if (resistanceDistance < 2.0) {
                    return String.format("R: %.1f%%", resistanceDistance);
                } else {
                    return "📊 Mid";
                }
                
            case "TREND":
                TrendDirection trend = item.technicalIndicators.getTrend();
                switch (trend) {
                    case BULLISH: return "📈 BULL";
                    case BEARISH: return "📉 BEAR";
                    case NEUTRAL: return "➡️ NEUTRAL";
                    default: return "❓ UNKNOWN";
                }
                
            case "ENTRY_QUALITY":
                double entryScore = item.technicalIndicators.getEntryQualityScore();
                String qualityIcon = "";
                if (entryScore >= 80) qualityIcon = " 🔥";
                else if (entryScore >= 60) qualityIcon = " 🟢";
                else if (entryScore >= 40) qualityIcon = " ⚠️";
                else qualityIcon = " 🔴";
                return String.format("%.0f%s", entryScore, qualityIcon);
                
            case "OVERALL_SIGNAL":
                String entrySignal = item.entrySignal;
                switch (entrySignal) {
                    case "STRONG_BUY": return "🔥 STRONG BUY";
                    case "BUY": return "🟢 BUY";
                    case "NEUTRAL": return "⚖️ NEUTRAL";
                    case "WAIT": return "⏳ WAIT";
                    case "AVOID": return "🔴 AVOID";
                    default: return "❓ UNKNOWN";
                }
                
            default:
                return "-";
        }
    }
    
    /**
     * Stop auto-refresh timer (useful for cleanup)
     */
    public void stopAutoRefresh() {
        if (priceRefreshTimer != null) {
            priceRefreshTimer.stop();
            LoggerUtil.info(WatchlistPanel.class, "Stopped watchlist auto-refresh timer");
        }
    }
    
    /**
     * Refresh only prices for all watchlist items (faster than full refresh)
     * This method is called by the auto-refresh timer
     */
    public void refreshPricesOnly() {
        LoggerUtil.debug(WatchlistPanel.class, "Auto-refreshing prices only");
        
        // Run price refresh in background thread
        new Thread(() -> {
            try {
                dataManager.refreshPricesOnly();
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    updateStats();
                    updateCacheStatus();
                    
                    // Update status briefly to show price refresh
                    statusLabel.setText("🔄 Prices updated");
                    statusLabel.setForeground(SUCCESS_COLOR);
                    
                    // Reset status after 2 seconds
                    Timer resetTimer = new Timer(2000, e -> {
                        statusLabel.setText("✅ Ready");
                        statusLabel.setForeground(SUCCESS_COLOR);
                    });
                    resetTimer.setRepeats(false);
                    resetTimer.start();
                });
                
            } catch (Exception e) {
                LoggerUtil.error(WatchlistPanel.class, "Failed to auto-refresh prices", e);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ Price refresh error");
                    statusLabel.setForeground(DANGER_COLOR);
                });
            }
        }).start();
    }
    
    /**
     * Update cache status display
     */
    public void updateCacheStatus() {
        try {
            cache.CoinGeckoApiCache cacheInstance = cache.CoinGeckoApiCache.getInstance();
            double efficiency = cacheInstance.getCacheEfficiency();
            
            String status;
            Color color;
            
            if (efficiency >= 0.8) {
                status = "⚡ Cache: Excellent (" + String.format("%.0f%%", efficiency * 100) + ")";
                color = new Color(76, 175, 80); // Green
            } else if (efficiency >= 0.6) {
                status = "🟡 Cache: Good (" + String.format("%.0f%%", efficiency * 100) + ")";
                color = new Color(255, 193, 7); // Orange
            } else if (efficiency >= 0.3) {
                status = "🟠 Cache: Fair (" + String.format("%.0f%%", efficiency * 100) + ")";
                color = new Color(255, 152, 0); // Dark orange
            } else {
                status = "🔴 Cache: Poor (" + String.format("%.0f%%", efficiency * 100) + ")";
                color = new Color(244, 67, 54); // Red
            }
            
            cacheStatusLabel.setText(status);
            cacheStatusLabel.setForeground(color);
        } catch (Exception e) {
            cacheStatusLabel.setText("❌ Cache: Error");
            cacheStatusLabel.setForeground(new Color(244, 67, 54));
        }
    }
    
    /**
     * Get cache status label for external access
     */
    public JLabel getCacheStatusLabel() {
        return cacheStatusLabel;
    }

    /**
     * Get fresh price from cache or API for an item
     * This ensures we always display the most current price available
     */
    private double getFreshPrice(WatchlistData item) {
        try {
            // First try to get from cache
            Double cachedPrice = cache.CoinGeckoApiCache.getCachedPrice(item.id);
            if (cachedPrice != null) {
                LoggerUtil.debug(WatchlistPanel.class, 
                    String.format("Using cached price for %s: $%.4f", item.symbol, cachedPrice));
                return cachedPrice;
            }
            
            // If not in cache, try to fetch from API (if coordination allows)
            if (dataManager.getApiCoordinator().canMakeApiCall("WatchlistPanel", "fresh-price-check")) {
                Double apiPrice = fetchPriceFromApi(item.id);
                if (apiPrice != null) {
                    // Cache the fresh price
                    cache.CoinGeckoApiCache.cachePrice(item.id, apiPrice);
                    LoggerUtil.debug(WatchlistPanel.class, 
                        String.format("Fetched fresh price for %s from API: $%.4f", item.symbol, apiPrice));
                    return apiPrice;
                }
            }
            
            // Fallback to stored price if cache miss and API unavailable
            LoggerUtil.debug(WatchlistPanel.class, 
                String.format("Using fallback stored price for %s: $%.4f", item.symbol, item.currentPrice));
            return item.currentPrice;
            
        } catch (Exception e) {
            LoggerUtil.warning(WatchlistPanel.class, 
                "Error getting fresh price for " + item.symbol + ", using stored price: " + e.getMessage());
            return item.currentPrice;
        }
    }
    
    /**
     * Fetch single price from CoinGecko API
     */
    private Double fetchPriceFromApi(String cryptoId) {
        try {
            String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + cryptoId + "&vs_currencies=usd";
            
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // Short timeout for UI responsiveness
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                LoggerUtil.warning(WatchlistPanel.class, 
                    "API returned response code: " + responseCode + " for " + cryptoId);
                return null;
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
            if (jsonResponse.has(cryptoId)) {
                org.json.JSONObject cryptoData = jsonResponse.getJSONObject(cryptoId);
                if (cryptoData.has("usd")) {
                    return cryptoData.getDouble("usd");
                }
            }
            
            return null;
            
        } catch (Exception e) {
            LoggerUtil.warning(WatchlistPanel.class, "Failed to fetch price from API for " + cryptoId + ": " + e.getMessage());
            return null;
        }
    }
}
