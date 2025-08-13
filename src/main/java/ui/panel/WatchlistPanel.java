package ui.panel;

import data.WatchlistDataManager;
import model.WatchlistData;
import model.TechnicalIndicators.TrendDirection;
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
    private DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("+#0.00%;-#0.00%");
    
    // Reference to data manager
    private WatchlistDataManager dataManager;
    
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
        LoggerUtil.info(WatchlistPanel.class, "Initializing Watchlist Panel");
        
        try {
            // Initialize data manager
            dataManager = new WatchlistDataManager();
            
            // Setup the UI
            setupUI();
            
            // Load initial data
            refreshWatchlistData();
            
            LoggerUtil.info(WatchlistPanel.class, "Watchlist Panel initialized successfully");
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
        
        JLabel subtitleLabel = new JLabel("Technical analysis and entry signals for crypto buy decisions");
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
        
        statsPanel.add(watchlistStatsLabel);
        statsPanel.add(statusLabel);
        
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
        
        // Create table model - Focus on technical analysis for buy decisions
        String[] columnNames = {
            "Symbol", "Name", "Current Price", "RSI", "MACD", "Trend", "Volume", 
            "Technical Analysis", "Days Tracked"
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
        
        // Set minimum column widths - Technical analysis focused
        watchlistTable.getColumnModel().getColumn(0).setMinWidth(70);   // Symbol
        watchlistTable.getColumnModel().getColumn(1).setMinWidth(120);  // Name
        watchlistTable.getColumnModel().getColumn(2).setMinWidth(90);   // Current Price
        watchlistTable.getColumnModel().getColumn(3).setMinWidth(70);   // RSI
        watchlistTable.getColumnModel().getColumn(4).setMinWidth(80);   // MACD
        watchlistTable.getColumnModel().getColumn(5).setMinWidth(80);   // Trend
        watchlistTable.getColumnModel().getColumn(6).setMinWidth(90);   // Volume
        watchlistTable.getColumnModel().getColumn(7).setMinWidth(130);  // Technical Analysis
        watchlistTable.getColumnModel().getColumn(8).setMinWidth(70);   // Days Tracked
        
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
                    
                    if (column == 7) { // Technical Analysis column
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
        refreshButton = createStyledButton("🔄 Refresh Prices", SUCCESS_COLOR);
        refreshButton.addActionListener(e -> refreshWatchlistData());
        
        // Import/Export buttons
        JButton importButton = createStyledButton("📥 Import", TEXT_SECONDARY);
        importButton.addActionListener(e -> importWatchlist());
        
        JButton exportButton = createStyledButton("📤 Export", TEXT_SECONDARY);
        exportButton.addActionListener(e -> exportWatchlist());
        
        controlPanel.add(addItemButton);
        controlPanel.add(removeItemButton);
        controlPanel.add(refreshButton);
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
                
                // Color coding based on column
                switch (column) {
                    case 3: // RSI
                        if (item.hasTechnicalAnalysis()) {
                            double rsi = item.technicalIndicators.getRsi();
                            if (rsi < 30) {
                                setForeground(SUCCESS_COLOR); // Oversold - Buy signal
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else if (rsi > 70) {
                                setForeground(DANGER_COLOR); // Overbought - Sell signal
                            } else {
                                setForeground(TEXT_PRIMARY);
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 4: // MACD
                        if (item.hasTechnicalAnalysis()) {
                            double macd = item.technicalIndicators.getMacd();
                            double macdSignal = item.technicalIndicators.getMacdSignal();
                            if (macd > macdSignal) {
                                setForeground(SUCCESS_COLOR); // Bullish
                            } else {
                                setForeground(DANGER_COLOR); // Bearish
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 5: // Trend
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
                        
                    case 6: // Volume
                        if (item.hasTechnicalAnalysis()) {
                            boolean volumeConfirmation = item.technicalIndicators.isVolumeConfirmation();
                            if (volumeConfirmation) {
                                setForeground(SUCCESS_COLOR);
                                setFont(getFont().deriveFont(Font.BOLD));
                            } else {
                                setForeground(TEXT_PRIMARY);
                            }
                        } else {
                            setForeground(TEXT_SECONDARY);
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        break;
                        
                    case 7: // Technical Analysis - clickable
                        setForeground(PRIMARY_COLOR);
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        break;
                        
                    case 2: // Price columns
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
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("🔄 Refreshing...");
            statusLabel.setForeground(WARNING_COLOR);
        });
        
        // Refresh in background thread
        new Thread(() -> {
            try {
                dataManager.refreshPricesAndAnalysis();
                
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    updateStats();
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
     * Update table with current watchlist data
     */
    private void updateTableData() {
        // Clear existing rows
        tableModel.setRowCount(0);
        
        // Add watchlist items
        List<WatchlistData> items = dataManager.getWatchlistItems();
        for (WatchlistData item : items) {
            Object[] rowData = {
                item.symbol,
                item.name,
                priceFormat.format(item.currentPrice),
                // Technical Analysis Columns
                getTechnicalValue(item, "RSI"),
                getTechnicalValue(item, "MACD"),
                getTechnicalValue(item, "TREND"),
                getTechnicalValue(item, "VOLUME"),
                // Analysis Status
                item.getTechnicalAnalysisStatus(),
                item.getDaysSinceAdded() + "d"
            };
            tableModel.addRow(rowData);
        }
    }
    
    /**
     * Update watchlist statistics
     */
    private void updateStats() {
        Map<String, Object> stats = dataManager.getPortfolioStatistics();
        int totalItems = (Integer) stats.get("totalItems");
        long goodOpportunities = (Long) stats.get("goodEntryOpportunities");
        
        // Count items with strong buy signals
        List<WatchlistData> items = dataManager.getWatchlistItems();
        long strongBuySignals = items.stream()
            .filter(item -> item.hasTechnicalAnalysis() && item.technicalIndicators.getRsi() < 30)
            .count();
        
        long technicalReady = items.stream()
            .filter(item -> item.hasTechnicalAnalysis())
            .count();
        
        String statsText = String.format("📊 Items: %d | Buy Signals: %d | Technical Ready: %d/%d", 
                                        totalItems, strongBuySignals, technicalReady, totalItems);
        
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
     * Get technical indicator value for display in table
     */
    private String getTechnicalValue(WatchlistData item, String indicator) {
        if (!item.hasTechnicalAnalysis()) {
            return "-";
        }
        
        switch (indicator) {
            case "RSI":
                double rsi = item.technicalIndicators.getRsi();
                if (rsi < 30) return String.format("%.1f 🟢", rsi);
                else if (rsi > 70) return String.format("%.1f 🔴", rsi);
                else return String.format("%.1f", rsi);
                
            case "MACD":
                double macd = item.technicalIndicators.getMacd();
                double macdSignal = item.technicalIndicators.getMacdSignal();
                String signal = macd > macdSignal ? " 🟢" : " 🔴";
                return String.format("%.4f%s", macd, signal);
                
            case "TREND":
                TrendDirection trend = item.technicalIndicators.getTrend();
                switch (trend) {
                    case BULLISH: return "📈 BULL";
                    case BEARISH: return "📉 BEAR";
                    case NEUTRAL: return "➡️ NEUTRAL";
                    default: return "❓ UNKNOWN";
                }
                
            case "VOLUME":
                boolean volumeConfirmation = item.technicalIndicators.isVolumeConfirmation();
                double volumeRatio = item.technicalIndicators.getVolumeRatio();
                String volumeIcon = volumeConfirmation ? " 🟢" : "";
                return String.format("%.1fx%s", volumeRatio, volumeIcon);
                
            default:
                return "-";
        }
    }
}
