package ui.dialog;

import data.WatchlistDataManager;
import model.WatchlistData;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for adding new items to the cryptocurrency watchlist
 */
public class AddWatchlistItemDialog extends JDialog {
    
    private WatchlistDataManager dataManager;
    private boolean itemAdded = false;
    
    // UI Components
    private JTextField symbolField;
    private JTextField nameField;
    private JTextField currentPriceField;
    private JTextField entryTargetField;
    private JTextField target3MonthField;
    private JTextField targetLongTermField;
    private JTextArea notesArea;
    private JButton addButton;
    private JButton cancelButton;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    
    public AddWatchlistItemDialog(JFrame parent, WatchlistDataManager dataManager) {
        super(parent, "Add to Watchlist", true);
        this.dataManager = dataManager;
        
        setupUI();
        setupEventHandlers();
        
        setSize(450, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    /**
     * Setup the dialog UI
     */
    private void setupUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Create header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Create form
        add(createFormPanel(), BorderLayout.CENTER);
        
        // Create button panel
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Create header panel with title and description
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("🎯 Add to Watchlist");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel descLabel = new JLabel("Track a cryptocurrency for entry opportunities");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descLabel.setForeground(TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(descLabel);
        
        return headerPanel;
    }
    
    /**
     * Create the main form panel
     */
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(SURFACE_COLOR);
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Symbol field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createLabel("Symbol (e.g., BTC):", true), gbc);
        gbc.gridy++;
        symbolField = createTextField("Enter cryptocurrency symbol", true);
        formPanel.add(symbolField, gbc);
        
        // Name field
        gbc.gridy++;
        formPanel.add(createLabel("Name (e.g., Bitcoin):", true), gbc);
        gbc.gridy++;
        nameField = createTextField("Enter cryptocurrency name", true);
        formPanel.add(nameField, gbc);
        
        // Current price field
        gbc.gridy++;
        formPanel.add(createLabel("Current Price ($):", true), gbc);
        gbc.gridy++;
        currentPriceField = createTextField("Enter current price", true);
        formPanel.add(currentPriceField, gbc);
        
        // Entry target field
        gbc.gridy++;
        formPanel.add(createLabel("Entry Target Price ($):", true), gbc);
        gbc.gridy++;
        entryTargetField = createTextField("Enter target entry price", true);
        formPanel.add(entryTargetField, gbc);
        
        // 3-month target field
        gbc.gridy++;
        formPanel.add(createLabel("3-Month Target ($):", false), gbc);
        gbc.gridy++;
        target3MonthField = createTextField("Enter 3-month target price", false);
        formPanel.add(target3MonthField, gbc);
        
        // Long-term target field
        gbc.gridy++;
        formPanel.add(createLabel("Long-Term Target ($):", false), gbc);
        gbc.gridy++;
        targetLongTermField = createTextField("Enter long-term target price", false);
        formPanel.add(targetLongTermField, gbc);
        
        // Notes area
        gbc.gridy++;
        formPanel.add(createLabel("Notes:", false), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        
        notesArea = new JTextArea(4, 30);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        notesArea.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        notesArea.setBackground(SURFACE_COLOR);
        notesArea.setForeground(TEXT_PRIMARY);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        
        JScrollPane notesScrollPane = new JScrollPane(notesArea);
        notesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        notesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        formPanel.add(notesScrollPane, gbc);
        
        return formPanel;
    }
    
    /**
     * Create a styled label
     */
    private JLabel createLabel(String text, boolean required) {
        JLabel label = new JLabel(text + (required ? " *" : ""));
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(required ? TEXT_PRIMARY : TEXT_SECONDARY);
        return label;
    }
    
    /**
     * Create a styled text field
     */
    private JTextField createTextField(String placeholder, boolean required) {
        JTextField field = new JTextField(30);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        field.setBackground(SURFACE_COLOR);
        field.setForeground(TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(300, 35));
        
        // Add placeholder-like behavior
        field.setToolTipText(placeholder);
        
        return field;
    }
    
    /**
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(0, 20, 20, 20));
        
        // Cancel button
        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.setForeground(TEXT_SECONDARY);
        cancelButton.setBackground(SURFACE_COLOR);
        cancelButton.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        cancelButton.setPreferredSize(new Dimension(80, 35));
        cancelButton.setFocusPainted(false);
        
        // Add button
        addButton = new JButton("Add to Watchlist");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addButton.setForeground(Color.WHITE);
        addButton.setBackground(PRIMARY_COLOR);
        addButton.setBorder(new LineBorder(PRIMARY_COLOR.darker(), 1));
        addButton.setPreferredSize(new Dimension(140, 35));
        addButton.setFocusPainted(false);
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(addButton);
        
        return buttonPanel;
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Add button handler
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addWatchlistItem();
            }
        });
        
        // Cancel button handler
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Enter key handler for add button
        getRootPane().setDefaultButton(addButton);
    }
    
    /**
     * Add the watchlist item
     */
    private void addWatchlistItem() {
        try {
            // Validate required fields
            String symbol = symbolField.getText().trim().toUpperCase();
            String name = nameField.getText().trim();
            String currentPriceStr = currentPriceField.getText().trim();
            String entryTargetStr = entryTargetField.getText().trim();
            
            if (symbol.isEmpty() || name.isEmpty() || currentPriceStr.isEmpty() || entryTargetStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please fill in all required fields (marked with *).", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Parse numeric fields
            double currentPrice = parsePrice(currentPriceStr, "Current Price");
            double entryTarget = parsePrice(entryTargetStr, "Entry Target");
            
            // Parse optional fields
            double target3Month = 0.0;
            double targetLongTerm = 0.0;
            
            String target3MonthStr = target3MonthField.getText().trim();
            if (!target3MonthStr.isEmpty()) {
                target3Month = parsePrice(target3MonthStr, "3-Month Target");
            }
            
            String targetLongTermStr = targetLongTermField.getText().trim();
            if (!targetLongTermStr.isEmpty()) {
                targetLongTerm = parsePrice(targetLongTermStr, "Long-Term Target");
            }
            
            String notes = notesArea.getText().trim();
            
            // Create watchlist item
            WatchlistData newItem = new WatchlistData(
                symbol.toLowerCase(), // Use lowercase for ID
                name,
                symbol,
                currentPrice,
                entryTarget,
                target3Month,
                targetLongTerm
            );
            
            newItem.setNotes(notes);
            newItem.updateEntryOpportunity();
            
            // Add to data manager
            dataManager.addWatchlistItem(newItem);
            
            itemAdded = true;
            dispose();
            
            LoggerUtil.info(AddWatchlistItemDialog.class, 
                "Added new watchlist item: " + symbol + " (" + name + ")");
            
        } catch (NumberFormatException e) {
            // Error already shown by parsePrice method
        } catch (Exception e) {
            LoggerUtil.error(AddWatchlistItemDialog.class, "Failed to add watchlist item", e);
            JOptionPane.showMessageDialog(this, 
                "Failed to add item: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Parse a price string and show error if invalid
     */
    private double parsePrice(String priceStr, String fieldName) throws NumberFormatException {
        try {
            // Remove any currency symbols or commas
            priceStr = priceStr.replace("$", "").replace(",", "").trim();
            double price = Double.parseDouble(priceStr);
            
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, 
                    fieldName + " must be greater than 0.", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE);
                throw new NumberFormatException("Price must be positive");
            }
            
            return price;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid " + fieldName + ". Please enter a valid number.", 
                "Validation Error", 
                JOptionPane.WARNING_MESSAGE);
            throw e;
        }
    }
    
    /**
     * Check if an item was successfully added
     */
    public boolean isItemAdded() {
        return itemAdded;
    }
}
