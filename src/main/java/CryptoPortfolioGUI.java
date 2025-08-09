import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class CryptoPortfolioGUI extends JFrame {
    private DefaultTableModel tableModel;
    private JTable cryptoTable;
    private JButton refreshButton;
    private JButton addCryptoButton;
    private Timer refreshTimer;
    private DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("+#0.00%;-#0.00%");
    
    // Crypto data storage
    private List<CryptoData> cryptoList;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(45, 52, 54);
    private static final Color SECONDARY_COLOR = new Color(99, 110, 114);
    private static final Color ACCENT_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color LIGHT_GRAY = new Color(248, 249, 250);
    
    public CryptoPortfolioGUI() {
        initializeCryptoList();
        setupUI();
        loadInitialPrices();
        startAutoRefresh();
    }
    
    private void initializeCryptoList() {
        cryptoList = new ArrayList<>();
        // Initialize with popular cryptocurrencies
        cryptoList.add(new CryptoData("bitcoin", "Bitcoin", "BTC", 0.0, 50000.0));
        cryptoList.add(new CryptoData("ethereum", "Ethereum", "ETH", 0.0, 3000.0));
        cryptoList.add(new CryptoData("binancecoin", "Binance Coin", "BNB", 0.0, 400.0));
        cryptoList.add(new CryptoData("cardano", "Cardano", "ADA", 0.0, 1.0));
        cryptoList.add(new CryptoData("solana", "Solana", "SOL", 0.0, 100.0));
        cryptoList.add(new CryptoData("dogecoin", "Dogecoin", "DOGE", 0.0, 0.25));
        cryptoList.add(new CryptoData("polkadot", "Polkadot", "DOT", 0.0, 25.0));
        cryptoList.add(new CryptoData("avalanche-2", "Avalanche", "AVAX", 0.0, 50.0));
        cryptoList.add(new CryptoData("polygon", "Polygon", "MATIC", 0.0, 2.0));
        cryptoList.add(new CryptoData("chainlink", "Chainlink", "LINK", 0.0, 20.0));
    }
    
    private void setupUI() {
        // Set modern look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        setTitle("💰 Modern Crypto Portfolio Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 249, 250));
        
        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(248, 249, 250));
        
        // Create modern header
        JPanel headerPanel = createModernHeader();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create table model with status column
        String[] columnNames = {"💎", "Name", "Current Price", "Target Price", "Difference", "% Change", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Target Price column is editable
            }
        };
        
        // Create modern table
        cryptoTable = new JTable(tableModel);
        setupModernTable();
        
        // Create modern scroll pane
        JScrollPane scrollPane = new JScrollPane(cryptoTable);
        setupModernScrollPane(scrollPane);
        
        // Wrap table in card
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(224, 224, 224), 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(cardPanel, BorderLayout.CENTER);
        
        // Create modern button panel
        JPanel buttonPanel = createModernButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Set window properties
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(248, 249, 250));
        headerPanel.setBorder(new EmptyBorder(0, 0, 30, 0));
        
        // Title
        JLabel titleLabel = new JLabel("💰 Crypto Portfolio Tracker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(33, 37, 41));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Real-time cryptocurrency price monitoring");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(subtitleLabel);
        
        return headerPanel;
    }
    
    private void setupModernTable() {
        cryptoTable.setRowHeight(45);
        cryptoTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cryptoTable.setBackground(Color.WHITE);
        cryptoTable.setForeground(new Color(33, 37, 41));
        cryptoTable.setSelectionBackground(new Color(0, 123, 255, 25));
        cryptoTable.setSelectionForeground(new Color(33, 37, 41));
        cryptoTable.setGridColor(new Color(222, 226, 230));
        cryptoTable.setShowGrid(true);
        cryptoTable.setIntercellSpacing(new Dimension(1, 1));
        
        // Set column widths
        cryptoTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // Symbol
        cryptoTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Name
        cryptoTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Current Price
        cryptoTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // Target Price
        cryptoTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // Difference
        cryptoTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // % Change
        cryptoTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Status
        
        // Modern header
        JTableHeader header = cryptoTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 249, 250));
        header.setForeground(new Color(73, 80, 87));
        header.setBorder(new EmptyBorder(12, 8, 12, 8));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 45));
        
        // Enhanced cell renderer
        cryptoTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
    }
    
    private void setupModernScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
    }
    
    private JPanel createModernButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 25));
        buttonPanel.setBackground(new Color(248, 249, 250));
        
        refreshButton = createModernButton("🔄 Refresh Prices", new Color(0, 123, 255));
        refreshButton.addActionListener(e -> refreshPrices());
        
        addCryptoButton = createModernButton("➕ Add Crypto", new Color(40, 167, 69));
        addCryptoButton.addActionListener(e -> showAddCryptoDialog());
        
        JButton editButton = createModernButton("✏️ Edit Target", new Color(255, 193, 7));
        editButton.addActionListener(e -> editExpectedPrice());
        
        JButton removeButton = createModernButton("🗑️ Remove", new Color(220, 53, 69));
        removeButton.addActionListener(e -> removeCrypto());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(addCryptoButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        
        return buttonPanel;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 40));
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
            tableModel.setRowCount(0); // Clear existing data
            for (CryptoData crypto : cryptoList) {
                addCryptoToTable(crypto);
            }
            refreshPrices();
        });
    }
    
    private void addCryptoToTable(CryptoData crypto) {
        String status = getStatusEmoji(crypto);
        Object[] rowData = {
            crypto.symbol.toUpperCase(),
            crypto.name,
            priceFormat.format(crypto.currentPrice),
            priceFormat.format(crypto.expectedPrice),
            priceFormat.format(crypto.currentPrice - crypto.expectedPrice),
            percentFormat.format(crypto.getPercentageChange()),
            status
        };
        tableModel.addRow(rowData);
    }
    
    private void refreshPrices() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                refreshButton.setEnabled(false);
                refreshButton.setText("Refreshing...");
                
                // Build API URL for all cryptocurrencies
                StringBuilder cryptoIds = new StringBuilder();
                for (int i = 0; i < cryptoList.size(); i++) {
                    if (i > 0) cryptoIds.append(",");
                    cryptoIds.append(cryptoList.get(i).id);
                }
                
                String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" 
                               + cryptoIds.toString() + "&vs_currencies=usd";
                
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject json = new JSONObject(response.toString());
                    
                    // Update prices
                    for (int i = 0; i < cryptoList.size(); i++) {
                        CryptoData crypto = cryptoList.get(i);
                        if (json.has(crypto.id)) {
                            double newPrice = json.getJSONObject(crypto.id).getDouble("usd");
                            crypto.currentPrice = newPrice;
                            
                            // Update table
                            final int index = i; // Make effectively final for lambda
                            SwingUtilities.invokeLater(() -> {
                                tableModel.setValueAt(priceFormat.format(newPrice), index, 2);
                                tableModel.setValueAt(priceFormat.format(newPrice - crypto.expectedPrice), index, 4);
                                tableModel.setValueAt(percentFormat.format(crypto.getPercentageChange()), index, 5);
                            });
                        }
                    }
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CryptoPortfolioGUI.this, 
                            "Error fetching prices: " + e.getMessage(), 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh Prices");
            }
        };
        
        worker.execute();
    }
    
    private void showAddCryptoDialog() {
        JDialog dialog = new JDialog(this, "Add Cryptocurrency", true);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField symbolField = new JTextField();
        JTextField expectedPriceField = new JTextField();
        
        dialog.add(new JLabel("Crypto ID (e.g., bitcoin):"));
        dialog.add(idField);
        dialog.add(new JLabel("Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Symbol:"));
        dialog.add(symbolField);
        dialog.add(new JLabel("Expected Price:"));
        dialog.add(expectedPriceField);
        
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        
        addButton.addActionListener(e -> {
            try {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String symbol = symbolField.getText().trim().toUpperCase();
                double expectedPrice = Double.parseDouble(expectedPriceField.getText());
                
                if (!id.isEmpty() && !name.isEmpty() && !symbol.isEmpty()) {
                    CryptoData newCrypto = new CryptoData(id, name, symbol, 0.0, expectedPrice);
                    cryptoList.add(newCrypto);
                    addCryptoToTable(newCrypto);
                    dialog.dispose();
                    refreshPrices();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Please fill all fields!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid expected price!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.add(addButton);
        dialog.add(cancelButton);
        
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void editExpectedPrice() {
        int selectedRow = cryptoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a cryptocurrency to edit!");
            return;
        }
        
        CryptoData crypto = cryptoList.get(selectedRow);
        String input = JOptionPane.showInputDialog(this, 
            "Enter new expected price for " + crypto.name + ":", 
            crypto.expectedPrice);
        
        if (input != null) {
            try {
                double newExpectedPrice = Double.parseDouble(input);
                crypto.expectedPrice = newExpectedPrice;
                
                // Update table
                tableModel.setValueAt(priceFormat.format(newExpectedPrice), selectedRow, 3);
                tableModel.setValueAt(priceFormat.format(crypto.currentPrice - newExpectedPrice), selectedRow, 4);
                tableModel.setValueAt(percentFormat.format(crypto.getPercentageChange()), selectedRow, 5);
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid price format!");
            }
        }
    }
    
    private void startAutoRefresh() {
        // Auto-refresh every 30 seconds
        refreshTimer = new Timer(30000, e -> refreshPrices());
        refreshTimer.start();
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
        }
    }
    
    private String getStatusEmoji(CryptoData crypto) {
        double difference = crypto.currentPrice - crypto.expectedPrice;
        double percentChange = Math.abs(crypto.getPercentageChange()) * 100;
        
        if (difference > 0) {
            if (percentChange > 10) return "🚀"; // High gain
            else if (percentChange > 5) return "📈"; // Good gain
            else return "✅"; // Small gain
        } else if (difference < 0) {
            if (percentChange > 10) return "🔴"; // High loss
            else if (percentChange > 5) return "📉"; // Moderate loss
            else return "⚠️"; // Small loss
        } else {
            return "⚖️"; // At target
        }
    }
    
    // Modern table cell renderer
    private class ModernTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            
            if (!isSelected) {
                c.setBackground(Color.WHITE);
                
                if (column == 4 || column == 5) { // Difference and % Change columns
                    if (row < cryptoList.size()) {
                        CryptoData crypto = cryptoList.get(row);
                        double difference = crypto.currentPrice - crypto.expectedPrice;
                        
                        if (difference > 0) {
                            c.setForeground(SUCCESS_COLOR);
                            setFont(new Font("Segoe UI", Font.BOLD, 13));
                        } else if (difference < 0) {
                            c.setForeground(DANGER_COLOR);
                            setFont(new Font("Segoe UI", Font.BOLD, 13));
                        } else {
                            c.setForeground(new Color(33, 37, 41));
                        }
                    }
                } else if (column == 6) { // Status column
                    c.setForeground(new Color(33, 37, 41));
                    setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Larger emoji
                    setHorizontalAlignment(CENTER);
                } else {
                    c.setForeground(new Color(33, 37, 41));
                }
            }
            
            return c;
        }
    }

    // Data class to hold cryptocurrency information
    private static class CryptoData {
        String id;
        String name;
        String symbol;
        double currentPrice;
        double expectedPrice;
        
        public CryptoData(String id, String name, String symbol, double currentPrice, double expectedPrice) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.currentPrice = currentPrice;
            this.expectedPrice = expectedPrice;
        }
        
        public double getPercentageChange() {
            if (expectedPrice == 0) return 0;
            return (currentPrice - expectedPrice) / expectedPrice;
        }
    }
    
    public static void main(String[] args) {
        // Set look and feel - using default for compatibility
        SwingUtilities.invokeLater(() -> new CryptoPortfolioGUI());
    }
}
