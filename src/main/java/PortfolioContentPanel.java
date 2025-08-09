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
    private DecimalFormat amountFormat = new DecimalFormat("#,##0.####");
    
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
        String[] columnNames = {"💎", "Name", "Holdings", "Avg Cost", "Current", "Entry Target", "3M Target", "Long Target", "Total Value", "P&L", "% Change", "Entry"};
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
        cryptoTable.getColumnModel().getColumn(11).setMinWidth(50);  // Entry Status
        
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
    }
    
    /**
     * Auto-fit column widths based on content
     */
    private void autoFitColumnWidths() {
        for (int column = 0; column < cryptoTable.getColumnCount(); column++) {
            int maxWidth = 0;
            
            // Check header width
            javax.swing.table.TableCellRenderer headerRenderer = cryptoTable.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                cryptoTable, cryptoTable.getColumnName(column), false, false, 0, column);
            maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width);
            
            // Check data widths
            for (int row = 0; row < cryptoTable.getRowCount(); row++) {
                javax.swing.table.TableCellRenderer cellRenderer = cryptoTable.getCellRenderer(row, column);
                Component comp = cellRenderer.getTableCellRendererComponent(
                    cryptoTable, cryptoTable.getValueAt(row, column), false, false, row, column);
                maxWidth = Math.max(maxWidth, comp.getPreferredSize().width);
            }
            
            // Add padding and set preferred width
            maxWidth += 20; // Add padding
            cryptoTable.getColumnModel().getColumn(column).setPreferredWidth(maxWidth);
        }
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        controlPanel.setBackground(BACKGROUND_COLOR);
        
        refreshButton = createModernButton("🔄 Refresh Prices", PRIMARY_COLOR);
        refreshButton.addActionListener(e -> refreshPrices());
        
        addCryptoButton = createModernButton("➕ Add Crypto", SUCCESS_COLOR);
        addCryptoButton.addActionListener(e -> showAddCryptoDialog());
        
        JButton removeButton = createModernButton("🗑️ Remove", DANGER_COLOR);
        removeButton.addActionListener(e -> removeCrypto());
        
        controlPanel.add(refreshButton);
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
            tableModel.setRowCount(0);
            for (CryptoData crypto : cryptoList) {
                addCryptoToTable(crypto);
            }
            updatePortfolioValue(); // Update portfolio value display
            autoFitColumnWidths(); // Auto-fit column widths after loading all data
            refreshPrices();
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
            crypto.getEntryStatusEmoji()                // Entry Status
        };
        tableModel.addRow(rowData);
        
        // Auto-fit column widths after adding new row
        SwingUtilities.invokeLater(this::autoFitColumnWidths);
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
    
    private void updateTableData() {
        if (isUpdatingTable) return; // Prevent recursive calls
        
        isUpdatingTable = true;
        try {
            for (int i = 0; i < cryptoList.size() && i < tableModel.getRowCount(); i++) {
                CryptoData crypto = cryptoList.get(i);
                
                tableModel.setValueAt(priceFormat.format(crypto.currentPrice), i, 4);        // Current Price
                tableModel.setValueAt(priceFormat.format(crypto.getTotalValue()), i, 8);     // Total Value
                tableModel.setValueAt(priceFormat.format(crypto.getProfitLoss()), i, 9);     // Profit/Loss
                tableModel.setValueAt(percentFormat.format(crypto.getProfitLossPercentage()), i, 10); // % Change
                tableModel.setValueAt(crypto.getEntryStatusEmoji(), i, 11);                   // Entry Status
            }
            
            // Auto-fit column widths after updating data
            SwingUtilities.invokeLater(this::autoFitColumnWidths);
        } finally {
            isUpdatingTable = false;
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
        setPlaceholderText(holdingsField, "e.g., 0.5, 2.0, 1000");
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
                addCryptoToTable(newCrypto);
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
                
                if (id != null && name != null && symbol != null && 
                    expectedPriceStr != null && holdingsStr != null && avgBuyPriceStr != null) {
                    
                    double expectedPrice = Double.parseDouble(expectedPriceStr);
                    double expectedEntry = (expectedEntryStr != null) ? Double.parseDouble(expectedEntryStr) : expectedPrice * 0.9;
                    double targetPrice3Month = (targetPrice3MonthStr != null) ? Double.parseDouble(targetPrice3MonthStr) : expectedPrice;
                    double targetPriceLongTerm = (targetPriceLongTermStr != null) ? Double.parseDouble(targetPriceLongTermStr) : expectedPrice;
                    double holdings = Double.parseDouble(holdingsStr);
                    double avgBuyPrice = Double.parseDouble(avgBuyPriceStr);
                    
                    cryptoList.add(new CryptoData(id, name, symbol, 0.0, expectedPrice, expectedEntry, targetPrice3Month, targetPriceLongTerm, holdings, avgBuyPrice));
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
                    
                    cryptoList.add(new CryptoData(id, name, symbol, 0.0, expectedPrice, expectedEntry, targetPrice3Month, targetPriceLongTerm, holdings, avgBuyPrice));
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
            tableModel.removeRow(selectedRow);
            savePortfolioData();
            updatePortfolioValue();
            // Auto-fit column widths after removing row
            SwingUtilities.invokeLater(this::autoFitColumnWidths);
        }
    }
    
    private void startAutoRefresh() {
        // Auto-refresh every 30 seconds
        refreshTimer = new Timer(30000, e -> refreshPrices());
        refreshTimer.start();
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
                } else if (column == 11) { // Entry Status column
                    setForeground(TEXT_PRIMARY);
                    setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    setHorizontalAlignment(CENTER);
                    
                    // Add background color based on entry opportunity
                    if (row < cryptoList.size()) {
                        CryptoData crypto = cryptoList.get(row);
                        if (crypto.isGoodEntryPoint()) {
                            setBackground(new Color(76, 175, 80, 30)); // Light green background
                        }
                    }
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
