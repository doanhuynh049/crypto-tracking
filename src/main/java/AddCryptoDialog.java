import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dedicated dialog class for adding new cryptocurrencies to the portfolio
 * Handles validation, user interface, and integration with the data manager
 */
public class AddCryptoDialog extends JDialog {
    
    // UI Components
    private JTextField idField;
    private JTextField nameField;
    private JTextField symbolField;
    private JTextField expectedPriceField;
    private JTextField expectedEntryField;
    private JTextField target3MonthField;
    private JTextField targetLongTermField;
    private JTextField holdingsField;
    private JTextField avgBuyPriceField;
    
    private JButton addButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JButton testIdButton;
    
    // Dependencies
    private PortfolioDataManager dataManager;
    private Component parentComponent;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);
    
    /**
     * Constructor for AddCryptoDialog
     */
    public AddCryptoDialog(Frame parent, PortfolioDataManager dataManager) {
        super(parent, "Add New Cryptocurrency", true);
        this.dataManager = dataManager;
        this.parentComponent = parent;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        configureDialog();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeComponents() {
        // Initialize text fields
        idField = createStyledTextField();
        nameField = createStyledTextField();
        symbolField = createStyledTextField();
        expectedPriceField = createStyledTextField();
        expectedEntryField = createStyledTextField();
        target3MonthField = createStyledTextField();
        targetLongTermField = createStyledTextField();
        holdingsField = createStyledTextField();
        avgBuyPriceField = createStyledTextField();
        
        // Set placeholder text
        setPlaceholderText(idField, "e.g., bitcoin, ethereum, cardano");
        setPlaceholderText(nameField, "e.g., Bitcoin, Ethereum, Cardano");
        setPlaceholderText(symbolField, "e.g., BTC, ETH, ADA");
        setPlaceholderText(expectedPriceField, "e.g., 50000.0");
        setPlaceholderText(expectedEntryField, "e.g., 45000.0");
        setPlaceholderText(target3MonthField, "e.g., 60000.0");
        setPlaceholderText(targetLongTermField, "e.g., 100000.0");
        setPlaceholderText(holdingsField, "e.g., 0.00032973, 0.5, 2.0, 1000");
        setPlaceholderText(avgBuyPriceField, "e.g., 45000.0");
        
        // Initialize buttons
        addButton = createModernButton("✅ Add Cryptocurrency", SUCCESS_COLOR);
        cancelButton = createModernButton("❌ Cancel", new Color(108, 117, 125));
        helpButton = createModernButton("❓ Help", new Color(33, 150, 243));
        testIdButton = createModernButton("🔍 Test ID", WARNING_COLOR);
        
        // Set button sizes for better layout
        addButton.setPreferredSize(new Dimension(200, 45));
        cancelButton.setPreferredSize(new Dimension(120, 45));
        helpButton.setPreferredSize(new Dimension(100, 45));
        testIdButton.setPreferredSize(new Dimension(120, 45));
    }
    
    /**
     * Setup the dialog layout
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(SURFACE_COLOR);
        
        // Create main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(SURFACE_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        // Create title panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Create form panel
        JPanel formPanel = createFormPanel();
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create the title panel
     */
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(PRIMARY_COLOR);
        titlePanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JLabel titleLabel = new JLabel("📈 Add New Cryptocurrency");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("Complete the form below to add a new cryptocurrency to your portfolio");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 180));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return titlePanel;
    }
    
    /**
     * Create the form panel with all input fields
     */
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(SURFACE_COLOR);
        formPanel.setBorder(new EmptyBorder(25, 0, 25, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 0, 12, 0);
        
        // Create form fields data
        Object[][] fieldData = {
            {"💎 Coin ID:", "The unique identifier used by CoinGecko API (e.g., bitcoin, ethereum)", idField, testIdButton},
            {"🏷️ Display Name:", "The full name of the cryptocurrency", nameField, null},
            {"🔤 Symbol:", "The trading symbol (e.g., BTC, ETH)", symbolField, null},
            {"🎯 Current Target:", "Your current expected price in USD", expectedPriceField, null},
            {"🔥 Entry Target:", "Your ideal entry/buy price in USD", expectedEntryField, null},
            {"📅 Target 3M:", "Your 3-month target price in USD", target3MonthField, null},
            {"🚀 Target Long:", "Your long-term target price in USD", targetLongTermField, null},
            {"💰 Holding Amount:", "Number of coins you own", holdingsField, null},
            {"💵 Average Cost:", "Your average purchase price per coin", avgBuyPriceField, null}
        };
        
        // Add form fields
        for (int i = 0; i < fieldData.length; i++) {
            String labelText = (String) fieldData[i][0];
            String tooltip = (String) fieldData[i][1];
            JTextField field = (JTextField) fieldData[i][2];
            JButton actionButton = (JButton) fieldData[i][3];
            
            // Create label
            JLabel label = createStyledLabel(labelText, tooltip);
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            formPanel.add(label, gbc);
            
            // Create field panel (field + optional button)
            JPanel fieldPanel = new JPanel(new BorderLayout(10, 0));
            fieldPanel.setBackground(SURFACE_COLOR);
            fieldPanel.add(field, BorderLayout.CENTER);
            
            if (actionButton != null) {
                fieldPanel.add(actionButton, BorderLayout.EAST);
            }
            
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 1;
            formPanel.add(fieldPanel, gbc);
        }
        
        return formPanel;
    }
    
    /**
     * Create the button panel
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setBackground(SURFACE_COLOR);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(helpButton);
        
        return buttonPanel;
    }
    
    /**
     * Setup event handlers for buttons and fields
     */
    private void setupEventHandlers() {
        // Add button event handler
        addButton.addActionListener(e -> handleAddCrypto());
        
        // Cancel button event handler
        cancelButton.addActionListener(e -> dispose());
        
        // Help button event handler
        helpButton.addActionListener(e -> showCoinIdHelpDialog());
        
        // Test ID button event handler
        testIdButton.addActionListener(e -> testCryptocurrencyId());
        
        // Enter key support for add button
        getRootPane().setDefaultButton(addButton);
    }
    
    /**
     * Configure dialog properties
     */
    private void configureDialog() {
        setSize(800, 900); // Increased size even more for better visibility
        setLocationRelativeTo(parentComponent);
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(750, 850)); // Set minimum size to prevent shrinking too much
    }
    
    /**
     * Handle adding new cryptocurrency
     */
    private void handleAddCrypto() {
        try {
            // Get field values
            String id = getFieldText(idField);
            String name = getFieldText(nameField);
            String symbol = getFieldText(symbolField).toUpperCase();
            
            // Validate required fields
            if (id.isEmpty() || name.isEmpty() || symbol.isEmpty()) {
                showValidationError("Please fill in all required fields (ID, Name, Symbol)!");
                return;
            }
            
            // Parse numeric fields
            double expectedPrice = parseDoubleField(expectedPriceField, "Current Target");
            double expectedEntry = parseDoubleField(expectedEntryField, "Entry Target");
            double target3Month = parseDoubleField(target3MonthField, "Target 3M");
            double targetLongTerm = parseDoubleField(targetLongTermField, "Target Long");
            double holdings = parseDoubleField(holdingsField, "Holding Amount");
            double avgBuyPrice = parseDoubleField(avgBuyPriceField, "Average Cost");
            
            // Set defaults for optional fields
            if (expectedEntry == 0.0) expectedEntry = expectedPrice * 0.9; // 10% below expected
            if (target3Month == 0.0) target3Month = expectedPrice;
            if (targetLongTerm == 0.0) targetLongTerm = expectedPrice;
            
            // Check for duplicate symbol
            List<CryptoData> cryptoList = dataManager.getCryptoList();
            for (CryptoData existing : cryptoList) {
                if (existing.symbol.equalsIgnoreCase(symbol)) {
                    showValidationError("A cryptocurrency with symbol '" + symbol + "' already exists!");
                    return;
                }
            }
            
            // Show validation progress
            setButtonsEnabled(false);
            addButton.setText("🔄 Validating & Adding...");
            
            // Create the cryptocurrency object
            CryptoData newCrypto = new CryptoData(id, name, symbol, 0.0, expectedPrice, 
                                                expectedEntry, target3Month, targetLongTerm, 
                                                holdings, avgBuyPrice);
            
            // Validate and add cryptocurrency asynchronously
            dataManager.addCryptoWithValidation(newCrypto)
                .thenAccept(success -> {
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            dispose();
                        } else {
                            // Reset button state for retry
                            setButtonsEnabled(true);
                            addButton.setText("✅ Add Cryptocurrency");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        setButtonsEnabled(true);
                        addButton.setText("✅ Add Cryptocurrency");
                        showValidationError("Unexpected error during validation: " + throwable.getMessage());
                    });
                    return null;
                });
                
        } catch (NumberFormatException ex) {
            showValidationError("Please enter valid numbers for prices and amounts!");
        } catch (Exception ex) {
            showValidationError("Error adding cryptocurrency: " + ex.getMessage());
        }
    }
    
    /**
     * Test cryptocurrency ID validity
     */
    private void testCryptocurrencyId() {
        String id = getFieldText(idField);
        if (id.isEmpty()) {
            showValidationError("Please enter a Coin ID to test!");
            return;
        }
        
        testIdButton.setEnabled(false);
        testIdButton.setText("🔄 Testing...");
        
        dataManager.validateCryptocurrencyId(id)
            .thenAccept(result -> {
                SwingUtilities.invokeLater(() -> {
                    testIdButton.setEnabled(true);
                    testIdButton.setText("🔍 Test ID");
                    
                    if (result.isValid) {
                        JOptionPane.showMessageDialog(this,
                            String.format("✅ Valid Cryptocurrency ID!\n\n" +
                                        "🪙 Coin: %s\n" +
                                        "💰 Current Price: $%.4f\n" +
                                        "🔄 API connection working correctly!",
                                        id, result.currentPrice),
                            "ID Validation Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Auto-fill current price if expected price is empty
                        if (getFieldText(expectedPriceField).isEmpty()) {
                            expectedPriceField.setText(String.valueOf(result.currentPrice));
                            expectedPriceField.setForeground(TEXT_PRIMARY);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                            String.format("❌ Invalid Cryptocurrency ID!\n\n" +
                                        "🔍 Issue: %s\n\n" +
                                        "💡 Try:\n" +
                                        "• Check spelling and use lowercase\n" +
                                        "• Visit CoinGecko to find the correct ID\n" +
                                        "• Click Help for detailed instructions",
                                        result.message),
                            "ID Validation Failed",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
            })
            .exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> {
                    testIdButton.setEnabled(true);
                    testIdButton.setText("🔍 Test ID");
                    showValidationError("Error testing ID: " + throwable.getMessage());
                });
                return null;
            });
    }
    
    /**
     * Show help dialog for finding cryptocurrency IDs
     */
    private void showCoinIdHelpDialog() {
        JDialog helpDialog = new JDialog(this, "🔍 How to Find Cryptocurrency ID", true);
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
            Use the "🔍 Test ID" button to verify your ID is correct!
            It will show the current price if the ID is valid.
            """;
        
        instructionsArea.setText(instructions);
        
        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(instructionsArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(550, 450));
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
        helpDialog.setSize(600, 650);
        helpDialog.setLocationRelativeTo(this);
        helpDialog.setResizable(true);
        helpDialog.setVisible(true);
    }
    
    // Helper methods...
    
    /**
     * Create a styled text field
     */
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
            new LineBorder(DIVIDER_COLOR, 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        field.setBackground(SURFACE_COLOR);
        field.setForeground(TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(0, 45));
        
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
        
        return field;
    }
    
    /**
     * Create a styled label
     */
    private JLabel createStyledLabel(String text, String tooltip) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_PRIMARY);
        label.setPreferredSize(new Dimension(180, 30));
        if (tooltip != null && !tooltip.isEmpty()) {
            label.setToolTipText(tooltip);
        }
        return label;
    }
    
    /**
     * Create a modern styled button
     */
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            Color originalColor = bgColor;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), 200));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(originalColor);
                }
            }
        });
        
        return button;
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
    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Enable/disable buttons during processing
     */
    private void setButtonsEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        helpButton.setEnabled(enabled);
        testIdButton.setEnabled(enabled);
    }
    
    /**
     * Show the dialog
     */
    public void showDialog() {
        setVisible(true);
    }
}
