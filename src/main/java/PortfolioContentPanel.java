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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.json.JSONObject;

/**
 * Portfolio content panel that can be embedded in the main application
 * This is the content part of CryptoPortfolioGUI without the JFrame window
 */
public class PortfolioContentPanel extends JPanel {
    
    private DefaultTableModel tableModel;
    private JTable cryptoTable;
    private JButton refreshButton;
    private JButton addCryptoButton;
    private JLabel statusLabel;
    private JLabel portfolioValueLabel;
    private Timer refreshTimer;
    private DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("+#0.00%;-#0.00%");
    private DecimalFormat amountFormat = new DecimalFormat("#,##0.########");
    
    // Crypto data storage
    private List<CryptoData> cryptoList;
    private static final String DATA_FILE = "src/data/.portfolio_data.bin"; // Hidden binary file for security
    private boolean isUpdatingTable = false; // Flag to prevent infinite recursion
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);
    
    public PortfolioContentPanel() {
        loadPortfolioData(); // Load saved data first
        if (cryptoList.isEmpty()) {
            initializeCryptoList(); // Only initialize defaults if no saved data
        }
        setupUI();
        loadInitialPrices();
        // Fetch AI advice once during initialization
        refreshAiAdvice();
        startAutoRefresh();
    }
    
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
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.NORTH);
        
        // Create table panel
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
        
        // Setup click-to-deselect after all components are created
        SwingUtilities.invokeLater(() -> {
            setupClickToDeselect();
        });
    }
    
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(BACKGROUND_COLOR);
        statusPanel.setBorder(new EmptyBorder(10, 0, 15, 0));
        
        // Left side - status
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(BACKGROUND_COLOR);
        
        statusLabel = new JLabel("📊 Portfolio Status: Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(TEXT_SECONDARY);
        leftPanel.add(statusLabel);
        
        // Right side - portfolio value
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(BACKGROUND_COLOR);
        
        portfolioValueLabel = new JLabel("💰 Total Value: $0.00");
        portfolioValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        portfolioValueLabel.setForeground(PRIMARY_COLOR);
        rightPanel.add(portfolioValueLabel);
        
        statusPanel.add(leftPanel, BorderLayout.WEST);
        statusPanel.add(rightPanel, BorderLayout.EAST);
        
        return statusPanel;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BACKGROUND_COLOR);
        
        // Create table model with enhanced columns
        String[] columnNames = {"💎", "Name", "Holdings", "Avg Cost", "Current", "Entry Target", "3M Target", "Long Target", "Total Value", "P&L", "% Change", "AI Advice"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3 || column == 5 || column == 6 || column == 7; // Holdings, Average Cost, Entry Target, Target 3M, Target Long
            }
        };
        
        // Create table
        cryptoTable = new JTable(tableModel);
        setupTable();
        
        // Add table model listener to handle direct editing of editable columns
        tableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && !isUpdatingTable) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                
                if (row >= 0 && row < cryptoList.size() && col >= 0) {
                    try {
                        String valueStr = tableModel.getValueAt(row, col).toString();
                        // Remove currency formatting if present
                        valueStr = valueStr.replace("$", "").replace(",", "");
                        double newValue = Double.parseDouble(valueStr);
                        
                        CryptoData crypto = cryptoList.get(row);
                        
                        switch (col) {
                            case 2: // Holding Amount
                                crypto.holdings = newValue;
                                break;
                            case 3: // Average Cost
                                crypto.avgBuyPrice = newValue;
                                break;
                            case 5: // Entry Target
                                crypto.expectedEntry = newValue;
                                break;
                            case 6: // Target 3M
                                crypto.targetPrice3Month = newValue;
                                break;
                            case 7: // Target Long
                                crypto.targetPriceLongTerm = newValue;
                                break;
                            default:
                                return; // Don't process other columns
                        }
                        
                        savePortfolioData();
                        
                        // Update the table display with proper formatting
                        isUpdatingTable = true;
                        try {
                            switch (col) {
                                case 2:
                                    tableModel.setValueAt(amountFormat.format(newValue), row, col);
                                    break;
                                case 3:
                                case 5:
                                case 6:
                                case 7:
                                    tableModel.setValueAt(priceFormat.format(newValue), row, col);
                                    break;
                            }
                            updateTableData();
                            updatePortfolioValue();
                        } finally {
                            isUpdatingTable = false;
                        }
                        
                    } catch (NumberFormatException ex) {
                        // Revert to original value if invalid
                        isUpdatingTable = true;
                        try {
                            CryptoData crypto = cryptoList.get(row);
                            switch (col) {
                                case 2:
                                    tableModel.setValueAt(amountFormat.format(crypto.holdings), row, col);
                                    break;
                                case 3:
                                    tableModel.setValueAt(priceFormat.format(crypto.avgBuyPrice), row, col);
                                    break;
                                case 5:
                                    tableModel.setValueAt(priceFormat.format(crypto.expectedEntry), row, col);
                                    break;
                                case 6:
                                    tableModel.setValueAt(priceFormat.format(crypto.targetPrice3Month), row, col);
                                    break;
                                case 7:
                                    tableModel.setValueAt(priceFormat.format(crypto.targetPriceLongTerm), row, col);
                                    break;
                            }
                        } finally {
                            isUpdatingTable = false;
                        }
                        JOptionPane.showMessageDialog(this, "Invalid format! Please enter a valid number.");
                    }
                }
            }
        });
        
        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(cryptoTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(SURFACE_COLOR);
        scrollPane.setBackground(SURFACE_COLOR);
        
        // Wrap in card-like panel
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(SURFACE_COLOR);
        cardPanel.setBorder(new CompoundBorder(
            new LineBorder(DIVIDER_COLOR, 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        
        tablePanel.add(cardPanel, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    private void setupTable() {
        cryptoTable.setRowHeight(45); // Increased row height for better appearance
        cryptoTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cryptoTable.setBackground(SURFACE_COLOR);
        cryptoTable.setForeground(TEXT_PRIMARY);
        cryptoTable.setSelectionBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 50));
        cryptoTable.setSelectionForeground(TEXT_PRIMARY);
        cryptoTable.setGridColor(new Color(240, 240, 240)); // Lighter grid lines
        cryptoTable.setShowGrid(true);
        cryptoTable.setIntercellSpacing(new Dimension(1, 1));
        
        // Enhanced alternating row colors
        cryptoTable.setShowHorizontalLines(true);
        cryptoTable.setShowVerticalLines(true);
        
        // Set minimum column widths and enable auto-resize
        cryptoTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Set preferred minimum widths for better display
        cryptoTable.getColumnModel().getColumn(0).setMinWidth(50);   // Code
        cryptoTable.getColumnModel().getColumn(1).setMinWidth(120);  // Name
        cryptoTable.getColumnModel().getColumn(2).setMinWidth(85);   // Holdings
        cryptoTable.getColumnModel().getColumn(3).setMinWidth(90);   // Avg Cost
        cryptoTable.getColumnModel().getColumn(4).setMinWidth(90);   // Current
        cryptoTable.getColumnModel().getColumn(5).setMinWidth(85);   // Entry Target
        cryptoTable.getColumnModel().getColumn(6).setMinWidth(80);   // 3M Target
        cryptoTable.getColumnModel().getColumn(7).setMinWidth(80);   // Long Target
        cryptoTable.getColumnModel().getColumn(8).setMinWidth(100);  // Total Value
        cryptoTable.getColumnModel().getColumn(9).setMinWidth(90);   // P&L
        cryptoTable.getColumnModel().getColumn(10).setMinWidth(85);  // % Change
        cryptoTable.getColumnModel().getColumn(11).setMinWidth(80);  // AI Advice
        
        // Set preferred widths based on content
        autoFitColumnWidths();
        
        // Enhanced header styling
        JTableHeader header = cryptoTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(245, 245, 247)); // Slightly different background
        header.setForeground(TEXT_PRIMARY);
        header.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(12, 8, 12, 8)
        ));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 45));
        
        // Custom cell renderer with enhanced styling
        cryptoTable.setDefaultRenderer(Object.class, new EnhancedPortfolioTableCellRenderer());
        
        // Add mouse listener for AI advice column clicks
        cryptoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = cryptoTable.rowAtPoint(e.getPoint());
                int column = cryptoTable.columnAtPoint(e.getPoint());
                
                // Check if clicked on AI Advice column (column 11)
                if (column == 11 && row >= 0 && row < cryptoList.size()) {
                    showAiAnalysisDialog(cryptoList.get(row));
                }
            }
        });
    }
    
    /**
     * Setup click-to-deselect functionality for the table
     */
    private void setupClickToDeselect() {
        MouseAdapter deselectListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Clear table selection when clicking outside the table
                if (e.getSource() != cryptoTable) {
                    cryptoTable.clearSelection();
                }
            }
        };
        
        // Add special handler for the table's scroll pane to handle clicks in empty areas
        Component scrollPane = cryptoTable.getParent().getParent(); // JScrollPane
        if (scrollPane instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) scrollPane;
            
            // Add listener to the scroll pane viewport
            sp.getViewport().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Convert point to table coordinates
                    Point tablePoint = SwingUtilities.convertPoint(
                        sp.getViewport(), e.getPoint(), cryptoTable);
                    
                    // Check if the click is outside the table bounds or in an empty area
                    if (tablePoint.y < 0 || tablePoint.y >= cryptoTable.getHeight() ||
                        tablePoint.x < 0 || tablePoint.x >= cryptoTable.getWidth() ||
                        cryptoTable.rowAtPoint(tablePoint) == -1) {
                        cryptoTable.clearSelection();
                    }
                }
            });
        }
        
        // Add the listener to the main panel and its components
        this.addMouseListener(deselectListener);
        
        // Add listener to status panel, control panel, and other components
        addDeselectListenerToComponents(this, deselectListener);
    }
    
    /**
     * Recursively add deselect listener to all components except the table
     */
    private void addDeselectListenerToComponents(Container container, MouseAdapter listener) {
        for (Component component : container.getComponents()) {
            if (component != cryptoTable && !(component instanceof JScrollPane && 
                ((JScrollPane) component).getViewport().getView() == cryptoTable)) {
                component.addMouseListener(listener);
                
                if (component instanceof Container) {
                    addDeselectListenerToComponents((Container) component, listener);
                }
            }
        }
    }
    
    /**
     * Auto-fit column widths based on content with improved data length calculation
     */
    private void autoFitColumnWidths() {
        FontMetrics fm = cryptoTable.getFontMetrics(cryptoTable.getFont());
        FontMetrics headerFm = cryptoTable.getTableHeader().getFontMetrics(cryptoTable.getTableHeader().getFont());
        
        for (int column = 0; column < cryptoTable.getColumnCount(); column++) {
            int maxWidth = 0;
            
            // Check header width with font metrics
            String headerText = cryptoTable.getColumnName(column);
            int headerWidth = headerFm.stringWidth(headerText) + 30; // Add padding for header
            maxWidth = Math.max(maxWidth, headerWidth);
            
            // Check data widths using string length and font metrics
            for (int row = 0; row < cryptoTable.getRowCount(); row++) {
                Object value = cryptoTable.getValueAt(row, column);
                if (value != null) {
                    String text = value.toString();
                    int textWidth = fm.stringWidth(text);
                    
                    // Add extra padding for different column types
                    int padding = 30; // Default padding
                    if (column == 0) { // Symbol column - needs less space
                        padding = 20;
                    } else if (column == 1) { // Name column - needs more space
                        padding = 40;
                    } else if (column >= 2 && column <= 10) { // Numeric columns
                        padding = 35;
                    } else if (column == 11) { // AI Advice column
                        padding = 30;
                    }
                    
                    maxWidth = Math.max(maxWidth, textWidth + padding);
                }
            }
            
            // Set minimum and maximum width constraints
            int minWidth = getColumnMinWidth(column);
            int maxAllowedWidth = getColumnMaxWidth(column);
            
            maxWidth = Math.max(maxWidth, minWidth);
            maxWidth = Math.min(maxWidth, maxAllowedWidth);
            
            cryptoTable.getColumnModel().getColumn(column).setPreferredWidth(maxWidth);
        }
    }
    
    /**
     * Get minimum width for each column type
     */
    private int getColumnMinWidth(int column) {
        switch (column) {
            case 0: return 50;   // Code
            case 1: return 120;  // Name
            case 2: return 85;   // Holdings
            case 3: return 90;   // Avg Cost
            case 4: return 90;   // Current
            case 5: return 85;   // Entry Target
            case 6: return 80;   // 3M Target
            case 7: return 80;   // Long Target
            case 8: return 100;  // Total Value
            case 9: return 90;   // P&L
            case 10: return 85;  // % Change
            case 11: return 80;  // AI Advice
            default: return 80;
        }
    }
    
    /**
     * Get maximum width for each column type to prevent overly wide columns
     */
    private int getColumnMaxWidth(int column) {
        switch (column) {
            case 0: return 80;   // Code
            case 1: return 200;  // Name
            case 2: return 120;  // Holdings
            case 3: return 120;  // Avg Cost
            case 4: return 120;  // Current
            case 5: return 120;  // Entry Target
            case 6: return 110;  // 3M Target
            case 7: return 110;  // Long Target
            case 8: return 150;  // Total Value
            case 9: return 130;  // P&L
            case 10: return 110; // % Change
            case 11: return 120; // AI Advice
            default: return 150;
        }
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        controlPanel.setBackground(BACKGROUND_COLOR);
        
        refreshButton = createModernButton("🔄 Refresh Prices", PRIMARY_COLOR);
        refreshButton.addActionListener(e -> refreshPrices());
        
        JButton refreshAiButton = createModernButton("🤖 Refresh AI", new Color(156, 39, 176));
        refreshAiButton.addActionListener(e -> refreshAiAdvice());
        
        addCryptoButton = createModernButton("➕ Add Crypto", SUCCESS_COLOR);
        addCryptoButton.addActionListener(e -> showAddCryptoDialog());
        
        JButton removeButton = createModernButton("🗑️ Remove", DANGER_COLOR);
        removeButton.addActionListener(e -> removeCrypto());
        
        controlPanel.add(refreshButton);
        controlPanel.add(refreshAiButton);
        controlPanel.add(addCryptoButton);
        controlPanel.add(removeButton);
        
        return controlPanel;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(140, 35));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            Color originalColor = bgColor;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(originalColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }
    
    private void loadInitialPrices() {
        SwingUtilities.invokeLater(() -> {
            // Sort by total value before displaying
            sortCryptosByTotalValue();
            
            tableModel.setRowCount(0);
            for (CryptoData crypto : cryptoList) {
                addCryptoToTable(crypto);
            }
            updatePortfolioValue(); // Update portfolio value display
            // Auto-fit column widths after loading all data with a delay to ensure proper calculation
            SwingUtilities.invokeLater(() -> {
                autoFitColumnWidths();
                cryptoTable.revalidate();
                cryptoTable.repaint();
            });
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
    
    private void addCryptoToTable(CryptoData crypto) {
        Object[] rowData = {
            crypto.symbol.toUpperCase(),                // Code
            crypto.name,                                // Name
            amountFormat.format(crypto.holdings),       // Holding Amount
            priceFormat.format(crypto.avgBuyPrice),     // Average Cost
            priceFormat.format(crypto.currentPrice),    // Current Price
            priceFormat.format(crypto.expectedEntry),   // Entry Target
            priceFormat.format(crypto.targetPrice3Month), // Target 3M
            priceFormat.format(crypto.targetPriceLongTerm), // Target Long
            priceFormat.format(crypto.getTotalValue()), // Total Value
            priceFormat.format(crypto.getProfitLoss()), // Profit/Loss
            percentFormat.format(crypto.getProfitLossPercentage()), // % Change
            crypto.getAiAdvice() + " ℹ️"                 // AI Advice with info icon
        };
        tableModel.addRow(rowData);
        
        // Auto-fit column widths after adding new row with a slight delay
        SwingUtilities.invokeLater(() -> {
            autoFitColumnWidths();
            cryptoTable.revalidate();
        });
    }
    
    private String getStatusEmoji(CryptoData crypto) {
        double difference = crypto.currentPrice - crypto.expectedPrice;
        double percentChange = Math.abs(crypto.getPercentageChange()) * 100;
        
        if (difference > 0) {
            if (percentChange > 10) return "🚀";
            else if (percentChange > 5) return "📈";
            else return "✅";
        } else if (difference < 0) {
            if (percentChange > 10) return "🔴";
            else if (percentChange > 5) return "📉";
            else return "⚠️";
        } else {
            return "⚖️";
        }
    }
    
    private void refreshPrices() {
        statusLabel.setText("📊 Portfolio Status: Refreshing...");
        refreshButton.setEnabled(false);
        refreshButton.setText("Refreshing...");
        
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
                statusLabel.setText("📊 Portfolio Status: Ready");
                refreshButton.setEnabled(true);
                refreshButton.setText("🔄 Refresh Prices");
                updatePortfolioValue();
            }
        };
        worker.execute();
    }
    
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
                statusLabel.setText("📊 Portfolio Status: Error fetching prices");
            });
        }
    }
    
    /**
     * Refresh AI advice for all cryptocurrencies (called separately from price refresh)
     */
    private void refreshAiAdvice() {
        statusLabel.setText("📊 Portfolio Status: Updating AI advice...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fetchAiAdvice();
                return null;
            }
            
            @Override
            protected void done() {
                updateTableData();
                statusLabel.setText("📊 Portfolio Status: Ready");
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
    
    private void updateTableData() {
        if (isUpdatingTable) return; // Prevent recursive calls
        
        isUpdatingTable = true;
        try {
            // Sort by total value first to maintain ranking
            sortCryptosByTotalValue();
            
            // If the order has changed, rebuild the entire table
            if (tableModel.getRowCount() != cryptoList.size()) {
                rebuildTable();
            } else {
                // Check if order has changed by comparing symbols
                boolean orderChanged = false;
                for (int i = 0; i < cryptoList.size() && i < tableModel.getRowCount(); i++) {
                    String currentSymbol = tableModel.getValueAt(i, 0).toString();
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
                    for (int i = 0; i < cryptoList.size() && i < tableModel.getRowCount(); i++) {
                        CryptoData crypto = cryptoList.get(i);
                        
                        tableModel.setValueAt(priceFormat.format(crypto.currentPrice), i, 4);        // Current Price
                        tableModel.setValueAt(priceFormat.format(crypto.getTotalValue()), i, 8);     // Total Value
                        tableModel.setValueAt(priceFormat.format(crypto.getProfitLoss()), i, 9);     // Profit/Loss
                        tableModel.setValueAt(percentFormat.format(crypto.getProfitLossPercentage()), i, 10); // % Change
                        tableModel.setValueAt(crypto.getAiAdvice() + " ℹ️", i, 11);           // AI Advice with info icon
                    }
                }
            }
            
            // Auto-fit column widths after updating data (less frequently)
            if (cryptoList.size() <= 10) { // Only auto-fit for smaller datasets to avoid performance issues
                SwingUtilities.invokeLater(this::autoFitColumnWidths);
            }
        } finally {
            isUpdatingTable = false;
        }
    }
    
    /**
     * Rebuild the entire table when order has changed
     */
    private void rebuildTable() {
        tableModel.setRowCount(0);
        for (CryptoData crypto : cryptoList) {
            Object[] rowData = {
                crypto.symbol.toUpperCase(),                // Code
                crypto.name,                                // Name
                amountFormat.format(crypto.holdings),       // Holding Amount
                priceFormat.format(crypto.avgBuyPrice),     // Average Cost
                priceFormat.format(crypto.currentPrice),    // Current Price
                priceFormat.format(crypto.expectedEntry),   // Entry Target
                priceFormat.format(crypto.targetPrice3Month), // Target 3M
                priceFormat.format(crypto.targetPriceLongTerm), // Target Long
                priceFormat.format(crypto.getTotalValue()), // Total Value
                priceFormat.format(crypto.getProfitLoss()), // Profit/Loss
                percentFormat.format(crypto.getProfitLossPercentage()), // % Change
                crypto.getAiAdvice() + " ℹ️"                 // AI Advice with info icon
            };
            tableModel.addRow(rowData);
        }
    }
    
    private void showAddCryptoDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Cryptocurrency", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(SURFACE_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Create modern input fields
        JLabel titleLabel = new JLabel("📈 Add New Cryptocurrency");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JTextField idField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        JTextField symbolField = new JTextField(20);
        JTextField expectedPriceField = new JTextField(20);
        JTextField expectedEntryField = new JTextField(20);
        JTextField target3MonthField = new JTextField(20);
        JTextField targetLongTermField = new JTextField(20);
        JTextField holdingsField = new JTextField(20);
        JTextField avgBuyPriceField = new JTextField(20);
        
        // Style input fields
        styleTextField(idField);
        styleTextField(nameField);
        styleTextField(symbolField);
        styleTextField(expectedPriceField);
        styleTextField(expectedEntryField);
        styleTextField(target3MonthField);
        styleTextField(targetLongTermField);
        styleTextField(holdingsField);
        styleTextField(avgBuyPriceField);
        
        // Add placeholder text
        setPlaceholderText(idField, "e.g., bitcoin, ethereum, cardano");
        setPlaceholderText(nameField, "e.g., Bitcoin, Ethereum, Cardano");
        setPlaceholderText(symbolField, "e.g., BTC, ETH, ADA");
        setPlaceholderText(expectedPriceField, "e.g., 50000.0");
        setPlaceholderText(expectedEntryField, "e.g., 45000.0");
        setPlaceholderText(target3MonthField, "e.g., 60000.0");
        setPlaceholderText(targetLongTermField, "e.g., 100000.0");
        setPlaceholderText(holdingsField, "e.g., 0.00032973, 0.5, 2.0, 1000");
        setPlaceholderText(avgBuyPriceField, "e.g., 45000.0");
        
        // Create labels with improved styling
        JLabel[] labels = {
            createStyledLabel("💎 Coin ID:", "The unique identifier used by CoinGecko API"),
            createStyledLabel("🏷️ Display Name:", "The full name of the cryptocurrency"),
            createStyledLabel("🔤 Symbol:", "The trading symbol (e.g., BTC, ETH)"),
            createStyledLabel("🎯 Current Target:", "Your current expected price in USD"),
            createStyledLabel("🔥 Entry Target:", "Your ideal entry/buy price in USD"),
            createStyledLabel("📅 Target 3M:", "Your 3-month target price in USD"),
            createStyledLabel("🚀 Target Long:", "Your long-term target price in USD"),
            createStyledLabel("💰 Holding Amount:", "Number of coins you own"),
            createStyledLabel("💵 Average Cost:", "Your average purchase price per coin")
        };
        
        JTextField[] fields = {idField, nameField, symbolField, expectedPriceField, expectedEntryField, target3MonthField, targetLongTermField, holdingsField, avgBuyPriceField};
        
        // Add title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 20, 25, 20);
        dialog.add(titleLabel, gbc);
        
        // Add form fields
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 20, 8, 10);
        for (int i = 0; i < labels.length; i++) {
            gbc.gridy = i + 1;
            gbc.gridx = 0;
            gbc.weightx = 0.3;
            dialog.add(labels[i], gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 0.7;
            gbc.insets = new Insets(8, 10, 8, 20);
            dialog.add(fields[i], gbc);
            gbc.insets = new Insets(8, 20, 8, 10);
        }
        
        // Buttons with improved styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(SURFACE_COLOR);
        
        JButton addButton = createModernButton("✅ Add Cryptocurrency", SUCCESS_COLOR);
        JButton cancelButton = createModernButton("❌ Cancel", new Color(108, 117, 125));
        
        addButton.setPreferredSize(new Dimension(180, 40));
        cancelButton.setPreferredSize(new Dimension(120, 40));
        
        addButton.addActionListener(e -> {
            try {
                String id = getFieldText(idField);
                String name = getFieldText(nameField);
                String symbol = getFieldText(symbolField).toUpperCase();
                
                if (id.isEmpty() || name.isEmpty() || symbol.isEmpty()) {
                    showValidationError(dialog, "Please fill in all required fields (ID, Name, Symbol)!");
                    return;
                }
                
                double expectedPrice = parseDoubleField(expectedPriceField, "Current Target");
                double expectedEntry = parseDoubleField(expectedEntryField, "Entry Target");
                double target3Month = parseDoubleField(target3MonthField, "Target 3M");
                double targetLongTerm = parseDoubleField(targetLongTermField, "Target Long");
                double holdings = parseDoubleField(holdingsField, "Holding Amount");
                double avgBuyPrice = parseDoubleField(avgBuyPriceField, "Average Cost");
                
                // If target fields are empty, default them to expected price
                if (expectedEntry == 0.0) expectedEntry = expectedPrice * 0.9; // 10% below expected
                if (target3Month == 0.0) target3Month = expectedPrice;
                if (targetLongTerm == 0.0) targetLongTerm = expectedPrice;
                
                // Check for duplicate symbol
                for (CryptoData existing : cryptoList) {
                    if (existing.symbol.equalsIgnoreCase(symbol)) {
                        showValidationError(dialog, "A cryptocurrency with symbol '" + symbol + "' already exists!");
                        return;
                    }
                }
                
                CryptoData newCrypto = new CryptoData(id, name, symbol, 0.0, expectedPrice, expectedEntry, target3Month, targetLongTerm, holdings, avgBuyPrice);
                cryptoList.add(newCrypto);
                
                // Sort and rebuild the table to maintain ranking by total value
                sortCryptosByTotalValue();
                rebuildTable();
                
                savePortfolioData();
                updatePortfolioValue();
                dialog.dispose();
                refreshPrices();
                
                // Show success message
                JOptionPane.showMessageDialog(this, 
                    "Successfully added " + name + " (" + symbol + ") to your portfolio!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (NumberFormatException ex) {
                showValidationError(dialog, "Please enter valid numbers for prices and amounts!");
            } catch (Exception ex) {
                showValidationError(dialog, "Error adding cryptocurrency: " + ex.getMessage());
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridy = labels.length + 1;
        gbc.gridx = 0; 
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 15, 20);
        dialog.add(buttonPanel, gbc);
        
        dialog.setSize(550, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
    
    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
            new LineBorder(DIVIDER_COLOR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(SURFACE_COLOR);
        field.setForeground(TEXT_PRIMARY);
        
        // Add focus listener for placeholder effect
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getForeground().equals(TEXT_SECONDARY)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
        });
    }
    
    private JLabel createStyledLabel(String text, String tooltip) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);
        if (tooltip != null && !tooltip.isEmpty()) {
            label.setToolTipText(tooltip);
        }
        return label;
    }
    
    private void setPlaceholderText(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(TEXT_SECONDARY);
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
            }
        });
    }
    
    private String getFieldText(JTextField field) {
        String text = field.getText().trim();
        // Return empty string if it's still showing placeholder text
        if (field.getForeground().equals(TEXT_SECONDARY)) {
            return "";
        }
        return text;
    }
    
    private double parseDoubleField(JTextField field, String fieldName) throws NumberFormatException {
        String text = getFieldText(field);
        if (text.isEmpty()) {
            return 0.0; // Default value for empty fields
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid " + fieldName + ": " + text);
        }
    }
    
    private void showValidationError(JDialog parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // Portfolio data persistence methods using binary serialization for security
    private void savePortfolioData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(cryptoList);
        } catch (IOException e) {
            System.err.println("Error saving portfolio data: " + e.getMessage());
            // Fallback to properties file if binary fails
            savePortfolioDataAsProperties();
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
    
    private void updatePortfolioValue() {
        double totalValue = 0.0;
        double totalProfitLoss = 0.0;
        
        for (CryptoData crypto : cryptoList) {
            totalValue += crypto.getTotalValue();
            totalProfitLoss += crypto.getProfitLoss();
        }
        
        String valueText = "💰 Total Value: " + priceFormat.format(totalValue);
        if (totalProfitLoss != 0) {
            String profitLossText = (totalProfitLoss >= 0 ? " (+" : " (") + priceFormat.format(totalProfitLoss) + ")";
            valueText += profitLossText;
        }
        
        portfolioValueLabel.setText(valueText);
        
        // Update color based on profit/loss
        if (totalProfitLoss > 0) {
            portfolioValueLabel.setForeground(SUCCESS_COLOR);
        } else if (totalProfitLoss < 0) {
            portfolioValueLabel.setForeground(DANGER_COLOR);
        } else {
            portfolioValueLabel.setForeground(PRIMARY_COLOR);
        }
    }
    
    private void removeCrypto() {
        int selectedRow = cryptoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a cryptocurrency to remove!");
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
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
            SwingUtilities.invokeLater(this::autoFitColumnWidths);
        }
    }
    
    private void startAutoRefresh() {
        // Auto-refresh every 10 seconds
        refreshTimer = new Timer(10000, e -> refreshPrices());
        refreshTimer.start();
    }
    
    /**
     * Show AI analysis dialog with detailed recommendations
     */
    private void showAiAnalysisDialog(CryptoData crypto) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                   "AI Analysis for " + crypto.name + " (" + crypto.symbol + ")", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SURFACE_COLOR);
        
        // Create title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(PRIMARY_COLOR);
        titlePanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("🤖 AI Investment Analysis");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel cryptoLabel = new JLabel(crypto.name + " (" + crypto.symbol + ")");
        cryptoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cryptoLabel.setForeground(Color.WHITE);
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(cryptoLabel, BorderLayout.SOUTH);
        
        // Create analysis text area
        JTextArea analysisArea = new JTextArea();
        analysisArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        analysisArea.setBackground(SURFACE_COLOR);
        analysisArea.setForeground(TEXT_PRIMARY);
        analysisArea.setEditable(false);
        analysisArea.setWrapStyleWord(true);
        analysisArea.setLineWrap(true);
        analysisArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Set loading text initially
        analysisArea.setText("🔄 Generating AI analysis...\n\nPlease wait while our AI analyzes " + crypto.name + " data...");
        
        // Create scroll pane for analysis
        JScrollPane scrollPane = new JScrollPane(analysisArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(SURFACE_COLOR);
        
        JButton refreshButton = createModernButton("🔄 Refresh Analysis", PRIMARY_COLOR);
        JButton closeButton = createModernButton("✅ Close", new Color(108, 117, 125));
        
        refreshButton.addActionListener(e -> {
            analysisArea.setText("🔄 Refreshing analysis...\n\nPlease wait...");
            // Generate new analysis in background
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return AiAdviceService.getDetailedAnalysis(crypto);
                }
                
                @Override
                protected void done() {
                    try {
                        analysisArea.setText(get());
                        analysisArea.setCaretPosition(0); // Scroll to top
                    } catch (Exception ex) {
                        analysisArea.setText("❌ Error generating analysis: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        });
        
        closeButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        
        // Add components to dialog
        dialog.add(titlePanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Generate analysis in background
        SwingWorker<String, Void> analysisWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return AiAdviceService.getDetailedAnalysis(crypto);
            }
            
            @Override
            protected void done() {
                try {
                    analysisArea.setText(get());
                    analysisArea.setCaretPosition(0); // Scroll to top
                } catch (Exception ex) {
                    analysisArea.setText("❌ Error generating analysis: " + ex.getMessage());
                }
            }
        };
        analysisWorker.execute();
        
        // Configure and show dialog
        dialog.setSize(650, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);
        dialog.setVisible(true);
    }
    
    // Enhanced custom table cell renderer with decorative styling
    private class EnhancedPortfolioTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Enhanced padding and styling
            setBorder(new CompoundBorder(
                new LineBorder(new Color(245, 245, 245), 1),
                new EmptyBorder(8, 12, 8, 12)
            ));
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            
            // Alternating row colors for better readability
            Color evenRowColor = new Color(252, 252, 252);
            Color oddRowColor = SURFACE_COLOR;
            
            if (!isSelected) {
                if (row % 2 == 0) {
                    c.setBackground(evenRowColor);
                } else {
                    c.setBackground(oddRowColor);
                }
                
                // Special styling for different column types
                if (column == 0) { // Symbol column
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                    setForeground(PRIMARY_COLOR);
                    setHorizontalAlignment(CENTER);
                } else if (column == 9 || column == 10) { // P&L and % Change columns
                    if (row < cryptoList.size()) {
                        CryptoData crypto = cryptoList.get(row);
                        double profitLoss = crypto.getProfitLoss();
                        
                        if (profitLoss > 0) {
                            setForeground(SUCCESS_COLOR);
                            setFont(new Font("Segoe UI", Font.BOLD, 13));
                        } else if (profitLoss < 0) {
                            setForeground(DANGER_COLOR);
                            setFont(new Font("Segoe UI", Font.BOLD, 13));
                        } else {
                            setForeground(TEXT_PRIMARY);
                        }
                    }
                    setHorizontalAlignment(RIGHT);
                } else if (column == 11) { // AI Advice column
                    // Create custom panel for AI advice with icon positioning
                    JPanel aiPanel = new JPanel(new BorderLayout());
                    aiPanel.setOpaque(true);
                    
                    // Extract advice text without icon
                    String fullText = value != null ? value.toString() : "Loading...";
                    String adviceText = fullText.replace(" ℹ️", "").trim();
                    
                    // Create advice label
                    JLabel adviceLabel = new JLabel(adviceText);
                    adviceLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    adviceLabel.setForeground(PRIMARY_COLOR);
                    adviceLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    // Create info icon label
                    JLabel iconLabel = new JLabel("ℹ️");
                    iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    iconLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                    iconLabel.setVerticalAlignment(SwingConstants.TOP);
                    iconLabel.setBorder(new EmptyBorder(2, 0, 0, 4));
                    
                    // Add components to panel
                    aiPanel.add(adviceLabel, BorderLayout.CENTER);
                    aiPanel.add(iconLabel, BorderLayout.EAST);
                    
                    // Set background color based on advice type
                    Color bgColor = new Color(200, 200, 200, 30); // Default light gray
                    if (!adviceText.equals("Loading...")) {
                        String advice = adviceText.toLowerCase();
                        if (advice.contains("buy") || advice.contains("good") || advice.contains("bullish")) {
                            bgColor = new Color(76, 175, 80, 30); // Light green
                        } else if (advice.contains("sell") || advice.contains("cut") || advice.contains("bearish")) {
                            bgColor = new Color(244, 67, 54, 30); // Light red
                        } else {
                            bgColor = new Color(255, 193, 7, 30); // Light yellow for hold/wait
                        }
                    }
                    
                    aiPanel.setBackground(bgColor);
                    adviceLabel.setOpaque(false);
                    iconLabel.setOpaque(false);
                    
                    // Apply border
                    aiPanel.setBorder(new CompoundBorder(
                        new LineBorder(PRIMARY_COLOR, 1, true),
                        new EmptyBorder(4, 6, 4, 6)
                    ));
                    
                    // Add tooltip
                    aiPanel.setToolTipText("Click for detailed AI analysis and recommendations");
                    
                    return aiPanel;
                } else if (column >= 2 && column <= 8) { // Numeric columns
                    setHorizontalAlignment(RIGHT);
                    setForeground(TEXT_PRIMARY);
                    
                    // Special highlighting for current price column
                    if (column == 4) {
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    }
                } else { // Text columns
                    setHorizontalAlignment(LEFT);
                    setForeground(TEXT_PRIMARY);
                }
                
                // Add subtle hover effect simulation
                if (hasFocus && !isSelected) {
                    setBorder(new CompoundBorder(
                        new LineBorder(PRIMARY_COLOR, 2),
                        new EmptyBorder(6, 10, 6, 10)
                    ));
                }
            } else {
                // Selected row styling
                c.setBackground(new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 100));
                setForeground(TEXT_PRIMARY);
                setBorder(new CompoundBorder(
                    new LineBorder(PRIMARY_COLOR, 2),
                    new EmptyBorder(6, 10, 6, 10)
                ));
            }
            
            return c;
        }
    }
}
