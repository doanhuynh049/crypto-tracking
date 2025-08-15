package ui.panel;

import model.CryptoData;
import data.PortfolioDataManager;
import ui.dialog.AddCryptoDialog;
import ui.dialog.AiAnalysisDialog;
import util.LoggerUtil;
import util.CacheManagerUtil;

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
import java.text.DecimalFormat;
import java.util.List;

/**
 * UI Builder class responsible for creating and managing all UI components
 * for the cryptocurrency portfolio interface.
 */
public class PortfolioUIBuilder {
    
    // UI Components
    private DefaultTableModel tableModel;
    private JTable cryptoTable;
    private JButton refreshButton;
    private JButton addCryptoButton;
    private JLabel statusLabel;
    private JLabel portfolioValueLabel;
    private JLabel aiStatusLabel; // New AI status label
    private JLabel cacheStatusLabel; // New cache status label
    private DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("+#0.00%;-#0.00%");
    private DecimalFormat amountFormat = new DecimalFormat("#,##0.########");
    
    // Reference to data manager
    private PortfolioDataManager dataManager;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);
    
    public PortfolioUIBuilder(PortfolioDataManager dataManager) {
        LoggerUtil.info(PortfolioUIBuilder.class, "Initializing Portfolio UI Builder");
        this.dataManager = dataManager;
    }
    
    /**
     * Create the main status panel with portfolio information
     */
    public JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        statusPanel.setBackground(BACKGROUND_COLOR);
        statusPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        // Portfolio value label
        portfolioValueLabel = new JLabel("💰 Total Value: $0.00");
        portfolioValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        portfolioValueLabel.setForeground(PRIMARY_COLOR);
        
        // Status label
        statusLabel = new JLabel("📊 Portfolio Status: Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(TEXT_PRIMARY);
        
        // AI Status label
        aiStatusLabel = new JLabel("🔄 AI: Loading...");
        aiStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        aiStatusLabel.setForeground(new Color(255, 193, 7));
        
        // Cache Status label
        cacheStatusLabel = new JLabel("⚡ Cache: Initializing...");
        cacheStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cacheStatusLabel.setForeground(new Color(76, 175, 80));
        
        statusPanel.add(portfolioValueLabel);
        statusPanel.add(statusLabel);
        statusPanel.add(aiStatusLabel);
        statusPanel.add(cacheStatusLabel);
        
        return statusPanel;
    }
    
    /**
     * Create the main table panel with cryptocurrency data
     */
    public JPanel createTablePanel() {
        LoggerUtil.debug(PortfolioUIBuilder.class, "Creating table panel");
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BACKGROUND_COLOR);
        
        // Create table model
        String[] columnNames = {"Code", "Name", "Holdings", "Avg Cost", "Current", "Entry Target", "3M Target", "Long Target", "Total Value", "P&L", "% Change", "AI Advice"};
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
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && !dataManager.isUpdatingTable()) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                
                if (row >= 0 && row < dataManager.getCryptoList().size() && col >= 0) {
                    // Only process editable columns
                    if (col != 2 && col != 3 && col != 5 && col != 6 && col != 7) {
                        return;
                    }
                    
                    try {
                        String valueStr = tableModel.getValueAt(row, col).toString();
                        // Remove currency formatting if present
                        valueStr = valueStr.replace("$", "").replace(",", "").trim();
                        
                        // Skip if empty
                        if (valueStr.isEmpty()) {
                            return;
                        }
                        
                        double newValue = Double.parseDouble(valueStr);
                        
                        CryptoData crypto = dataManager.getCryptoList().get(row);
                        
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
                        }
                        
                        dataManager.savePortfolioData();
                        
                        // Schedule a delayed update to avoid recursive calls
                        SwingUtilities.invokeLater(() -> {
                            dataManager.setUpdatingTable(true);
                            try {
                                // Update only calculated columns and portfolio value
                                dataManager.updatePortfolioValue();
                                // Refresh the entire table to update calculated values
                                dataManager.updateTableData();
                            } finally {
                                dataManager.setUpdatingTable(false);
                            }
                        });
                        
                    } catch (NumberFormatException ex) {
                        // Revert to original value if invalid
                        dataManager.setUpdatingTable(true);
                        try {
                            CryptoData crypto = dataManager.getCryptoList().get(row);
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
                            dataManager.setUpdatingTable(false);
                        }
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(cryptoTable, "Invalid format! Please enter a valid number.");
                        });
                    }
                }
            }
        });
        
        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(cryptoTable);
        scrollPane.setBorder(new LineBorder(DIVIDER_COLOR, 1));
        scrollPane.setBackground(SURFACE_COLOR);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        return tablePanel;
    }
    
    /**
     * Setup table appearance and behavior
     */
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
        cryptoTable.getColumnModel().getColumn(2).setMinWidth(65);   // Holdings (reduced)
        cryptoTable.getColumnModel().getColumn(3).setMinWidth(90);   // Avg Cost
        cryptoTable.getColumnModel().getColumn(4).setMinWidth(90);   // Current
        cryptoTable.getColumnModel().getColumn(5).setMinWidth(85);   // Entry Target
        cryptoTable.getColumnModel().getColumn(6).setMinWidth(80);   // 3M Target
        cryptoTable.getColumnModel().getColumn(7).setMinWidth(80);   // Long Target
        cryptoTable.getColumnModel().getColumn(8).setMinWidth(100);  // Total Value
        cryptoTable.getColumnModel().getColumn(9).setMinWidth(90);   // P&L
        cryptoTable.getColumnModel().getColumn(10).setMinWidth(85);  // % Change
        cryptoTable.getColumnModel().getColumn(11).setMinWidth(120); // AI Advice (increased)
        
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
                int column = cryptoTable.columnAtPoint(e.getPoint());
                int row = cryptoTable.rowAtPoint(e.getPoint());
                
                if (column == 11 && row >= 0) { // AI Advice column
                    List<CryptoData> cryptoList = dataManager.getCryptoList();
                    if (row < cryptoList.size()) {
                        CryptoData crypto = cryptoList.get(row);
                        showAiAnalysisDialog(crypto);
                    }
                }
            }
        });
    }
    
    /**
     * Setup click-to-deselect functionality for the table
     */
    public void setupClickToDeselect() {
        MouseAdapter deselectListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Check if click was outside table bounds or on empty area
                if (SwingUtilities.isDescendingFrom(e.getComponent(), cryptoTable)) {
                    Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cryptoTable);
                    int row = cryptoTable.rowAtPoint(point);
                    if (row == -1) {
                        cryptoTable.clearSelection();
                    }
                } else {
                    cryptoTable.clearSelection();
                }
            }
        };
        
        // Add special handler for the table's scroll pane to handle clicks in empty areas
        Component scrollPane = cryptoTable.getParent().getParent(); // JScrollPane
        if (scrollPane instanceof JScrollPane) {
            scrollPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cryptoTable);
                    int row = cryptoTable.rowAtPoint(point);
                    if (row == -1) {
                        cryptoTable.clearSelection();
                    }
                }
            });
        }
        
        // Add the listener to the main panel and its components
        addDeselectListenerToComponents(cryptoTable.getParent().getParent().getParent(), deselectListener);
    }
    
    /**
     * Recursively add deselect listener to all components except the table
     */
    private void addDeselectListenerToComponents(Container container, MouseAdapter listener) {
        for (Component component : container.getComponents()) {
            if (component != cryptoTable && component != cryptoTable.getParent() && component != cryptoTable.getParent().getParent()) {
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
    public void autoFitColumnWidths() {
        FontMetrics fm = cryptoTable.getFontMetrics(cryptoTable.getFont());
        FontMetrics headerFm = cryptoTable.getTableHeader().getFontMetrics(cryptoTable.getTableHeader().getFont());
        
        for (int column = 0; column < cryptoTable.getColumnCount(); column++) {
            // Get header width
            String headerText = cryptoTable.getColumnModel().getColumn(column).getHeaderValue().toString();
            int headerWidth = headerFm.stringWidth(headerText) + 30; // Add padding
            int maxWidth = headerWidth;
            
            // Check data widths using string length and font metrics
            for (int row = 0; row < cryptoTable.getRowCount(); row++) {
                Object value = cryptoTable.getValueAt(row, column);
                if (value != null) {
                    String cellText = value.toString();
                    int cellWidth = fm.stringWidth(cellText) + 25; // Add padding
                    maxWidth = Math.max(maxWidth, cellWidth);
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
            case 2: return 65;   // Holdings (reduced)
            case 3: return 90;   // Avg Cost
            case 4: return 90;   // Current
            case 5: return 85;   // Entry Target
            case 6: return 80;   // 3M Target
            case 7: return 80;   // Long Target
            case 8: return 100;  // Total Value
            case 9: return 90;   // P&L
            case 10: return 85;  // % Change
            case 11: return 120; // AI Advice (increased)
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
            case 2: return 100;  // Holdings (reduced max width)
            case 3: return 120;  // Avg Cost
            case 4: return 120;  // Current
            case 5: return 120;  // Entry Target
            case 6: return 110;  // 3M Target
            case 7: return 110;  // Long Target
            case 8: return 150;  // Total Value
            case 9: return 130;  // P&L
            case 10: return 110; // % Change
            case 11: return 160; // AI Advice (increased max width)
            default: return 150;
        }
    }
    
    /**
     * Create control panel with action buttons
     */
    public JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        controlPanel.setBackground(BACKGROUND_COLOR);
        
        refreshButton = createModernButton("🔄 Refresh Prices", PRIMARY_COLOR);
        refreshButton.addActionListener(e -> dataManager.refreshPrices());
        
        JButton refreshAiButton = createModernButton("🤖 Refresh AI", new Color(156, 39, 176));
        refreshAiButton.addActionListener(e -> dataManager.refreshAiAdvice());
        
        addCryptoButton = createModernButton("➕ Add Crypto", SUCCESS_COLOR);
        addCryptoButton.addActionListener(e -> showAddCryptoDialog());
        
        JButton removeButton = createModernButton("🗑️ Remove", DANGER_COLOR);
        removeButton.addActionListener(e -> dataManager.removeCrypto(cryptoTable.getSelectedRow()));
        
        controlPanel.add(refreshButton);
        controlPanel.add(refreshAiButton);
        controlPanel.add(addCryptoButton);
        controlPanel.add(removeButton);
        
        return controlPanel;
    }
    
    /**
     * Create modern styled button
     */
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
                button.setBackground(new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), 200));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }
    
    /**
     * Show add cryptocurrency dialog using the new dedicated dialog class
     */
    private void showAddCryptoDialog() {
        LoggerUtil.info(PortfolioUIBuilder.class, "Opening Add Cryptocurrency dialog");
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(cryptoTable);
        AddCryptoDialog dialog = new AddCryptoDialog(parentFrame, dataManager);
        dialog.showDialog();
    }
    
    /**
     * Style text field with modern appearance
     */
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
    
    /**
     * Create styled label with tooltip
     */
    private JLabel createStyledLabel(String text, String tooltip) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);
        if (tooltip != null && !tooltip.isEmpty()) {
            label.setToolTipText(tooltip);
        }
        return label;
    }
    
    /**
     * Set placeholder text for text field
     */
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
    
    /**
     * Get actual text from field (excluding placeholder)
     */
    private String getFieldText(JTextField field) {
        String text = field.getText().trim();
        // Return empty string if it's still showing placeholder text
        if (field.getForeground().equals(TEXT_SECONDARY)) {
            return "";
        }
        return text;
    }
    
    /**
     * Parse double value from text field
     */
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
    
    /**
     * Show validation error dialog
     */
    private void showValidationError(JDialog parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Show AI analysis dialog with detailed recommendations
     */
    private void showAiAnalysisDialog(CryptoData crypto) {
        LoggerUtil.info(PortfolioUIBuilder.class, "Opening AI Analysis Dialog for " + crypto.symbol);
        
        // Create and show the dedicated AI analysis dialog
        AiAnalysisDialog aiDialog = new AiAnalysisDialog(cryptoTable, crypto);
        aiDialog.showDialog();
    }
    
    /**
     * Show help dialog for finding cryptocurrency IDs
     */
    private void showCoinIdHelpDialog(JDialog parent) {
        JDialog helpDialog = new JDialog(parent, "🔍 How to Find Cryptocurrency ID", true);
        helpDialog.setLayout(new BorderLayout());
        helpDialog.getContentPane().setBackground(SURFACE_COLOR);
        
        // Create title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(33, 150, 243));
        titlePanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("🔍 Finding the Correct Cryptocurrency ID");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Create instructions text area
        JTextArea instructionsArea = new JTextArea();
        instructionsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        instructionsArea.setBackground(SURFACE_COLOR);
        instructionsArea.setForeground(TEXT_PRIMARY);
        instructionsArea.setEditable(false);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        String instructions = """
            📋 STEP-BY-STEP GUIDE:
            
            1️⃣ Visit CoinGecko.com in your web browser
            
            2️⃣ Search for your cryptocurrency (e.g., "Bitcoin", "Ethereum")
            
            3️⃣ Click on the cryptocurrency from search results
            
            4️⃣ Look at the URL in your browser address bar:
               • Example: https://www.coingecko.com/en/coins/bitcoin
               • The ID is the last part: "bitcoin"
            
            💡 COMMON EXAMPLES:
            • Bitcoin → bitcoin
            • Ethereum → ethereum
            • Binance Coin → binancecoin
            • Cardano → cardano
            • Solana → solana
            • Dogecoin → dogecoin
            • Polkadot → polkadot
            • Chainlink → chainlink
            • Polygon → polygon
            • Avalanche → avalanche-2
            
            ⚠️ IMPORTANT NOTES:
            • Use the exact ID from the URL (all lowercase)
            • Some coins have numbers (e.g., "avalanche-2")
            • The ID might be different from the name or symbol
            • Spaces are replaced with hyphens (-)
            
            ✅ VERIFICATION:
            The app will automatically test the ID when you click "Add Cryptocurrency"
            and show you the current price if it's valid!
            """;
        
        instructionsArea.setText(instructions);
        
        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(instructionsArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Create close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(SURFACE_COLOR);
        
        JButton closeButton = createModernButton("✅ Got It!", new Color(33, 150, 243));
        closeButton.addActionListener(e -> helpDialog.dispose());
        buttonPanel.add(closeButton);
        
        // Add components to dialog
        helpDialog.add(titlePanel, BorderLayout.NORTH);
        helpDialog.add(scrollPane, BorderLayout.CENTER);
        helpDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Configure and show dialog
        helpDialog.setSize(550, 600);
        helpDialog.setLocationRelativeTo(parent);
        helpDialog.setResizable(true);
        helpDialog.setVisible(true);
    }

    /**
     * Update portfolio value display
     */
    public void updatePortfolioValue(double totalValue, double totalProfitLoss) {
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
    
    /**
     * Add cryptocurrency to table
     */
    public void addCryptoToTable(CryptoData crypto) {
        LoggerUtil.debug(PortfolioUIBuilder.class, "Adding cryptocurrency to table: " + crypto.symbol);
        
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
            crypto.getAiAdviceWithStatus() + " ℹ️"                 // AI Advice with status icon
        };
        tableModel.addRow(rowData);
        
        // Auto-fit column widths after adding new row with a slight delay
        SwingUtilities.invokeLater(() -> {
            autoFitColumnWidths();
            cryptoTable.revalidate();
        });
    }
    
    /**
     * Rebuild the entire table when order has changed
     */
    public void rebuildTable(List<CryptoData> cryptoList) {
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
                crypto.getAiAdviceWithStatus() + " ℹ️"                 // AI Advice with status icon
            };
            tableModel.addRow(rowData);
        }
    }
    
    /**
     * Update table data for specific row
     */
    public void updateTableRow(int i, CryptoData crypto) {
        tableModel.setValueAt(crypto.symbol.toUpperCase(), i, 0);
        tableModel.setValueAt(crypto.name, i, 1);
        tableModel.setValueAt(amountFormat.format(crypto.holdings), i, 2);
        tableModel.setValueAt(priceFormat.format(crypto.avgBuyPrice), i, 3);
        tableModel.setValueAt(priceFormat.format(crypto.currentPrice), i, 4);
        tableModel.setValueAt(priceFormat.format(crypto.expectedEntry), i, 5);
        tableModel.setValueAt(priceFormat.format(crypto.targetPrice3Month), i, 6);
        tableModel.setValueAt(priceFormat.format(crypto.targetPriceLongTerm), i, 7);
        tableModel.setValueAt(priceFormat.format(crypto.getTotalValue()), i, 8);
        tableModel.setValueAt(priceFormat.format(crypto.getProfitLoss()), i, 9);
        tableModel.setValueAt(percentFormat.format(crypto.getProfitLossPercentage()), i, 10);
        tableModel.setValueAt(crypto.getAiAdviceWithStatus() + " ℹ️", i, 11);
    }
    
    /**
     * Update AI status display based on the crypto list
     */
    public void updateAiStatus(List<CryptoData> cryptoList) {
        LoggerUtil.info(PortfolioUIBuilder.class, "Updating AI status for portfolio");
        if (cryptoList == null || cryptoList.isEmpty()) {
            return;
        }
        
        int totalCount = cryptoList.size();
        int aiSuccessCount = 0;
        int fallbackCount = 0;
        int errorCount = 0;
        int loadingCount = 0;
        
        for (CryptoData crypto : cryptoList) {
            LoggerUtil.info(PortfolioUIBuilder.class, "Updating AI status for: " + crypto.symbol);
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
        
        // Update AI status label with progress information
        updateAiStatusLabel(loadingCount, aiSuccessCount, fallbackCount, errorCount, totalCount);
    }
    
    /**
     * Update AI status label with current progress
     */
    public void updateAiStatusLabel(int loadingCount, int aiSuccessCount, int fallbackCount, int errorCount, int totalCount) {
        if (aiStatusLabel == null) return;
        
        int completedCount = aiSuccessCount + fallbackCount + errorCount;
        
        if (loadingCount > 0) {
            // Still loading - show progress
            aiStatusLabel.setText(String.format("🔄 AI: Loading... (%d/%d completed)", completedCount, totalCount));
            aiStatusLabel.setForeground(new Color(255, 193, 7)); // Orange/yellow for loading
        } else {
            // All completed - show final status
            aiStatusLabel.setText(String.format("✅ AI: Completed (%d AI, %d fallback, %d errors)", 
                                               aiSuccessCount, fallbackCount, errorCount));
            if (errorCount > 0) {
                aiStatusLabel.setForeground(new Color(255, 87, 34)); // Orange for partial errors
            } else {
                aiStatusLabel.setForeground(new Color(76, 175, 80)); // Green for success
            }
        }
    }

    // Getters for UI components
    public JTable getCryptoTable() { return cryptoTable; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public JButton getRefreshButton() { return refreshButton; }
    public JLabel getStatusLabel() { return statusLabel; }
    public JLabel getAiStatusLabel() { return aiStatusLabel; }
    public JLabel getCacheStatusLabel() { return cacheStatusLabel; }

    /**
     * Update cache status display with current cache statistics
     */
    public void updateCacheStatus() {
        if (cacheStatusLabel == null) return;
        
        String cacheStats = CacheManagerUtil.getFormattedCacheStats();
        double efficiency = CacheManagerUtil.getCacheEfficiency();
        
        if (efficiency >= 80) {
            cacheStatusLabel.setText("⚡ Cache: " + cacheStats);
            cacheStatusLabel.setForeground(new Color(76, 175, 80)); // Green for excellent
        } else if (efficiency >= 60) {
            cacheStatusLabel.setText("⚡ Cache: " + cacheStats);
            cacheStatusLabel.setForeground(new Color(33, 150, 243)); // Blue for good
        } else if (efficiency >= 40) {
            cacheStatusLabel.setText("⚡ Cache: " + cacheStats);
            cacheStatusLabel.setForeground(new Color(255, 193, 7)); // Yellow for moderate
        } else if (efficiency > 0) {
            cacheStatusLabel.setText("⚡ Cache: " + cacheStats);
            cacheStatusLabel.setForeground(new Color(255, 87, 34)); // Orange for poor
        } else {
            cacheStatusLabel.setText("⚡ Cache: Initializing...");
            cacheStatusLabel.setForeground(new Color(117, 117, 117)); // Gray for no data
        }
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
                    List<CryptoData> cryptoList = dataManager.getCryptoList();
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
                    
                    // Set background color and border based on AI status
                    Color bgColor = new Color(200, 200, 200, 30); // Default light gray
                    Color borderColor = PRIMARY_COLOR;
                    
                    // Determine colors based on AI status indicators
                    if (adviceText.startsWith("🔄")) { // Loading
                        bgColor = new Color(255, 193, 7, 30); // Yellow for loading
                        borderColor = new Color(255, 193, 7);
                        adviceLabel.setForeground(new Color(102, 77, 3));
                    } else if (adviceText.startsWith("🤖")) { // AI Generated
                        bgColor = new Color(76, 175, 80, 30); // Green for AI success
                        borderColor = new Color(76, 175, 80);
                        adviceLabel.setForeground(new Color(27, 94, 32));
                    } else if (adviceText.startsWith("📊")) { // Rule-based fallback
                        bgColor = new Color(33, 150, 243, 30); // Blue for rule-based
                        borderColor = new Color(33, 150, 243);
                        adviceLabel.setForeground(new Color(13, 71, 161));
                    } else if (adviceText.startsWith("❌")) { // Error
                        bgColor = new Color(244, 67, 54, 30); // Red for error
                        borderColor = new Color(244, 67, 54);
                        adviceLabel.setForeground(new Color(183, 28, 28));
                    } else {
                        // Fallback: analyze advice content for color coding
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
                    
                    // Apply border with appropriate color
                    aiPanel.setBorder(new CompoundBorder(
                        new LineBorder(borderColor, 1, true),
                        new EmptyBorder(4, 6, 4, 6)
                    ));
                    
                    // Add enhanced tooltip with status information
                    String tooltipText = "Click for detailed AI analysis and recommendations";
                    if (adviceText.startsWith("🔄")) {
                        tooltipText = "AI advice is loading... " + tooltipText;
                    } else if (adviceText.startsWith("🤖")) {
                        tooltipText = "AI-generated advice - " + tooltipText;
                    } else if (adviceText.startsWith("📊")) {
                        tooltipText = "Rule-based advice (AI unavailable) - " + tooltipText;
                    } else if (adviceText.startsWith("❌")) {
                        tooltipText = "Error getting AI advice - " + tooltipText;
                    }
                    aiPanel.setToolTipText(tooltipText);
                    
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