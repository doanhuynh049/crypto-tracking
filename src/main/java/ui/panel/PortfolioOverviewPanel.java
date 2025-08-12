package ui.panel;

import model.CryptoData;
import model.PortfolioRebalanceRecommendation;
import data.PortfolioDataManager;
import service.PortfolioRebalanceService;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Overview Panel that displays portfolio allocation and provides AI-powered rebalancing
 * Features:
 * - Portfolio holdings overview with percentages
 * - Visual allocation chart
 * - AI-powered portfolio rebalancing recommendations
 * - Interactive rebalancing with new money allocation
 */
public class PortfolioOverviewPanel extends JPanel {
    
    // Dependencies
    private PortfolioDataManager dataManager;
    
    // UI Components
    private JTable overviewTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private JLabel totalValueLabel;
    private JLabel totalProfitLossLabel;
    private JTextField newMoneyField;
    private JButton analyzeButton;
    private JButton applyRecommendationButton;
    private JTextArea recommendationArea;
    private JProgressBar analysisProgressBar;
    
    // Current AI recommendation
    private PortfolioRebalanceRecommendation currentRecommendation;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);      // Blue 700
    private static final Color PRIMARY_DARK = new Color(13, 71, 161);        // Blue 900
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);     // White
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);  // Light Gray
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);       // Green 500
    private static final Color DANGER_COLOR = new Color(244, 67, 54);        // Red 500
    private static final Color WARNING_COLOR = new Color(255, 152, 0);       // Orange 500
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);         // Dark Gray
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);    // Medium Gray
    
    // Typography
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    
    // Formatters
    private static final DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat percentFormat = new DecimalFormat("0.00%");
    private static final DecimalFormat amountFormat = new DecimalFormat("#,##0.0000");
    
    public PortfolioOverviewPanel(PortfolioDataManager dataManager) {
        this.dataManager = dataManager;
        LoggerUtil.info(PortfolioOverviewPanel.class, "Initializing Portfolio Overview Panel");
        
        setupUI();
        refreshData();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create main container with scroll support
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BACKGROUND_COLOR);
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        
        // Center panel with split layout
        JSplitPane centerSplitPane = createCenterPanel();
        mainContainer.add(centerSplitPane, BorderLayout.CENTER);
        
        // Bottom panel for AI rebalancing
        JPanel bottomPanel = createRebalancingPanel();
        mainContainer.add(bottomPanel, BorderLayout.SOUTH);
        
        // Add scroll support
        JScrollPane scrollPane = new JScrollPane(mainContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Title
        JLabel titleLabel = new JLabel("📊 Portfolio Overview");
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);
        
        // Summary stats panel
        JPanel statsPanel = createSummaryStatsPanel();
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statsPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createSummaryStatsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        statsPanel.setBackground(BACKGROUND_COLOR);
        
        // Total value
        totalValueLabel = new JLabel("Total: $0.00");
        totalValueLabel.setFont(TITLE_FONT);
        totalValueLabel.setForeground(TEXT_PRIMARY);
        
        // Total profit/loss
        totalProfitLossLabel = new JLabel("P&L: $0.00");
        totalProfitLossLabel.setFont(TITLE_FONT);
        
        statsPanel.add(totalValueLabel);
        statsPanel.add(createSeparator());
        statsPanel.add(totalProfitLossLabel);
        
        return statsPanel;
    }
    
    private JSplitPane createCenterPanel() {
        // Left side: Overview table
        JPanel leftPanel = createOverviewTablePanel();
        
        // Right side: Allocation chart
        JPanel rightPanel = createAllocationChartPanel();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);
        
        return splitPane;
    }
    
    private JPanel createOverviewTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(224, 224, 224), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        // Panel title
        JLabel titleLabel = new JLabel("🔍 Holdings Breakdown");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        // Create table
        createOverviewTable();
        JScrollPane scrollPane = new JScrollPane(overviewTable);
        scrollPane.setBorder(new LineBorder(new Color(224, 224, 224), 1));
        scrollPane.setPreferredSize(new Dimension(380, 300));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void createOverviewTable() {
        String[] columnNames = {
            "Symbol", "Name", "Holdings", "Value", "% of Portfolio", "P&L", "P&L %"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        overviewTable = new JTable(tableModel);
        overviewTable.setFont(SMALL_FONT);
        overviewTable.setRowHeight(28);
        overviewTable.setShowGrid(true);
        overviewTable.setGridColor(new Color(240, 240, 240));
        overviewTable.setSelectionBackground(new Color(230, 247, 255));
        overviewTable.setSelectionForeground(TEXT_PRIMARY);
        
        // Custom renderer for colored cells
        overviewTable.setDefaultRenderer(Object.class, new OverviewTableCellRenderer());
        
        // Set column widths
        if (overviewTable.getColumnModel().getColumnCount() > 0) {
            overviewTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Symbol
            overviewTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Name
            overviewTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Holdings
            overviewTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Value
            overviewTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // % Portfolio
            overviewTable.getColumnModel().getColumn(5).setPreferredWidth(70);  // P&L
            overviewTable.getColumnModel().getColumn(6).setPreferredWidth(60);  // P&L %
        }
    }
    
    private JPanel createAllocationChartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(224, 224, 224), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        // Panel title
        JLabel titleLabel = new JLabel("🥧 Portfolio Allocation");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        // Chart area
        chartPanel = new JPanel();
        chartPanel.setBackground(SURFACE_COLOR);
        chartPanel.setPreferredSize(new Dimension(300, 300));
        chartPanel.setBorder(new LineBorder(new Color(224, 224, 224), 1));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRebalancingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(224, 224, 224), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // Title
        JLabel titleLabel = new JLabel("🤖 AI-Powered Portfolio Rebalancing");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        // Input panel
        JPanel inputPanel = createRebalancingInputPanel();
        
        // Recommendation panel
        JPanel recommendationPanel = createRecommendationPanel();
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.WEST);
        panel.add(recommendationPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRebalancingInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 0, 20));
        
        // New money input
        JLabel newMoneyLabel = new JLabel("💰 Additional Investment Amount:");
        newMoneyLabel.setFont(BODY_FONT);
        newMoneyLabel.setForeground(TEXT_PRIMARY);
        newMoneyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        newMoneyField = new JTextField("1000.00");
        newMoneyField.setFont(BODY_FONT);
        newMoneyField.setPreferredSize(new Dimension(150, 35));
        newMoneyField.setMaximumSize(new Dimension(150, 35));
        newMoneyField.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        newMoneyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Analyze button
        analyzeButton = createStyledButton("🔍 Get AI Recommendation", PRIMARY_COLOR);
        analyzeButton.addActionListener(this::handleAnalyzeRequest);
        analyzeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Apply button
        applyRecommendationButton = createStyledButton("✅ Apply Recommendation", SUCCESS_COLOR);
        applyRecommendationButton.addActionListener(this::handleApplyRecommendation);
        applyRecommendationButton.setEnabled(false);
        applyRecommendationButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Progress bar
        analysisProgressBar = new JProgressBar();
        analysisProgressBar.setStringPainted(true);
        analysisProgressBar.setString("Ready to analyze");
        analysisProgressBar.setPreferredSize(new Dimension(200, 25));
        analysisProgressBar.setMaximumSize(new Dimension(200, 25));
        analysisProgressBar.setVisible(false);
        analysisProgressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(newMoneyLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(newMoneyField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(analyzeButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(applyRecommendationButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(analysisProgressBar);
        
        return panel;
    }
    
    private JPanel createRecommendationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_COLOR);
        
        // Recommendation area
        recommendationArea = new JTextArea();
        recommendationArea.setFont(SMALL_FONT);
        recommendationArea.setBackground(new Color(250, 250, 250));
        recommendationArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        recommendationArea.setEditable(false);
        recommendationArea.setLineWrap(true);
        recommendationArea.setWrapStyleWord(true);
        recommendationArea.setText("💡 Enter an additional investment amount and click 'Get AI Recommendation' to receive personalized portfolio rebalancing advice based on current market conditions and your holdings.");
        
        JScrollPane scrollPane = new JScrollPane(recommendationArea);
        scrollPane.setBorder(new LineBorder(new Color(200, 200, 200), 1));
        scrollPane.setPreferredSize(new Dimension(400, 150));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(BODY_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(200, 35));
        button.setMaximumSize(new Dimension(200, 35));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private JLabel createSeparator() {
        JLabel separator = new JLabel("|");
        separator.setFont(TITLE_FONT);
        separator.setForeground(TEXT_SECONDARY);
        return separator;
    }
    
    /**
     * Refresh the data displayed in the overview panel
     */
    public void refreshData() {
        LoggerUtil.debug(PortfolioOverviewPanel.class, "Refreshing portfolio overview data");
        
        List<CryptoData> cryptoList = dataManager.getCryptoList();
        if (cryptoList == null || cryptoList.isEmpty()) {
            LoggerUtil.warning(PortfolioOverviewPanel.class, "No cryptocurrency data available");
            return;
        }
        
        // Clear existing data
        tableModel.setRowCount(0);
        
        // Calculate totals
        double totalValue = 0.0;
        double totalProfitLoss = 0.0;
        
        for (CryptoData crypto : cryptoList) {
            totalValue += crypto.getTotalValue();
            totalProfitLoss += crypto.getProfitLoss();
        }
        
        // Update table data
        for (CryptoData crypto : cryptoList) {
            if (crypto.getTotalValue() > 0) { // Only show holdings with value
                double portfolioPercentage = totalValue > 0 ? (crypto.getTotalValue() / totalValue) : 0.0;
                
                Object[] rowData = {
                    crypto.symbol.toUpperCase(),
                    crypto.name,
                    amountFormat.format(crypto.holdings),
                    priceFormat.format(crypto.getTotalValue()),
                    percentFormat.format(portfolioPercentage),
                    priceFormat.format(crypto.getProfitLoss()),
                    percentFormat.format(crypto.getProfitLossPercentage())
                };
                tableModel.addRow(rowData);
            }
        }
        
        // Update summary labels
        totalValueLabel.setText("Total: " + priceFormat.format(totalValue));
        totalProfitLossLabel.setText("P&L: " + priceFormat.format(totalProfitLoss));
        totalProfitLossLabel.setForeground(totalProfitLoss >= 0 ? SUCCESS_COLOR : DANGER_COLOR);
        
        // Update chart
        updateAllocationChart(cryptoList, totalValue);
        
        LoggerUtil.info(PortfolioOverviewPanel.class, 
            String.format("Portfolio overview updated - Total Value: %s, P&L: %s", 
                priceFormat.format(totalValue), priceFormat.format(totalProfitLoss)));
    }
    
    private void updateAllocationChart(List<CryptoData> cryptoList, double totalValue) {
        chartPanel.removeAll();
        
        if (totalValue <= 0) {
            JLabel noDataLabel = new JLabel("No portfolio data available", SwingConstants.CENTER);
            noDataLabel.setFont(BODY_FONT);
            noDataLabel.setForeground(TEXT_SECONDARY);
            chartPanel.add(noDataLabel);
            chartPanel.revalidate();
            chartPanel.repaint();
            return;
        }
        
        // Create a simple pie chart representation
        chartPanel.setLayout(new BorderLayout());
        
        // Create custom pie chart panel
        PieChartPanel pieChart = new PieChartPanel(cryptoList, totalValue);
        chartPanel.add(pieChart, BorderLayout.CENTER);
        
        // Create legend
        JPanel legendPanel = createChartLegend(cryptoList, totalValue);
        chartPanel.add(legendPanel, BorderLayout.SOUTH);
        
        chartPanel.revalidate();
        chartPanel.repaint();
    }
    
    private JPanel createChartLegend(List<CryptoData> cryptoList, double totalValue) {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setBackground(SURFACE_COLOR);
        legendPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Show top holdings in legend
        int count = 0;
        for (CryptoData crypto : cryptoList) {
            if (crypto.getTotalValue() > 0 && count < 6) { // Show top 6 holdings
                double percentage = (crypto.getTotalValue() / totalValue) * 100;
                if (percentage >= 1.0) { // Only show holdings >= 1%
                    JPanel legendItem = createLegendItem(crypto.symbol, percentage, count);
                    legendPanel.add(legendItem);
                    count++;
                }
            }
        }
        
        return legendPanel;
    }
    
    private JPanel createLegendItem(String symbol, double percentage, int colorIndex) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        item.setBackground(SURFACE_COLOR);
        
        // Color indicator
        JPanel colorBox = new JPanel();
        colorBox.setBackground(getChartColor(colorIndex));
        colorBox.setPreferredSize(new Dimension(12, 12));
        colorBox.setBorder(new LineBorder(Color.GRAY, 1));
        
        // Label
        JLabel label = new JLabel(String.format("%s (%.1f%%)", symbol, percentage));
        label.setFont(SMALL_FONT);
        label.setForeground(TEXT_PRIMARY);
        
        item.add(colorBox);
        item.add(label);
        
        return item;
    }
    
    private Color getChartColor(int index) {
        Color[] colors = {
            new Color(52, 152, 219),   // Blue
            new Color(155, 89, 182),   // Purple
            new Color(46, 204, 113),   // Green
            new Color(241, 196, 15),   // Yellow
            new Color(230, 126, 34),   // Orange
            new Color(231, 76, 60),    // Red
            new Color(149, 165, 166),  // Gray
            new Color(26, 188, 156)    // Teal
        };
        return colors[index % colors.length];
    }
    
    private void handleAnalyzeRequest(ActionEvent e) {
        try {
            double newMoney = Double.parseDouble(newMoneyField.getText().replace(",", ""));
            if (newMoney <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid positive amount for additional investment.", 
                    "Invalid Amount", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Start analysis
            analyzeButton.setEnabled(false);
            analysisProgressBar.setVisible(true);
            analysisProgressBar.setIndeterminate(true);
            analysisProgressBar.setString("🤖 Getting AI recommendation...");
            
            recommendationArea.setText("🔄 Analyzing your portfolio and market conditions...\n\nThis may take a few moments as our AI evaluates:\n• Current portfolio allocation\n• Market trends and opportunities\n• Risk-adjusted recommendations\n• Optimal rebalancing strategy");
            
            // Get AI recommendation asynchronously
            CompletableFuture.supplyAsync(() -> {
                return PortfolioRebalanceService.getRebalanceRecommendation(dataManager.getCryptoList(), newMoney);
            }).thenAccept(recommendation -> {
                SwingUtilities.invokeLater(() -> {
                    handleAnalysisComplete(recommendation);
                });
            }).exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> {
                    handleAnalysisError(throwable);
                });
                return null;
            });
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid numeric amount for additional investment.", 
                "Invalid Number Format", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleAnalysisComplete(PortfolioRebalanceRecommendation recommendation) {
        currentRecommendation = recommendation;
        
        // Update UI
        analyzeButton.setEnabled(true);
        analysisProgressBar.setVisible(false);
        applyRecommendationButton.setEnabled(recommendation != null && recommendation.isValid());
        
        if (recommendation != null && recommendation.isValid()) {
            recommendationArea.setText(recommendation.getFormattedRecommendation());
            LoggerUtil.info(PortfolioOverviewPanel.class, "AI portfolio rebalancing recommendation received successfully");
        } else {
            recommendationArea.setText("❌ Unable to generate AI recommendation at this time.\n\nThis could be due to:\n• Temporary AI service unavailability\n• Network connectivity issues\n• Insufficient portfolio data\n\nPlease try again in a few moments.");
            LoggerUtil.warning(PortfolioOverviewPanel.class, "Failed to get AI portfolio recommendation");
        }
    }
    
    private void handleAnalysisError(Throwable throwable) {
        analyzeButton.setEnabled(true);
        analysisProgressBar.setVisible(false);
        applyRecommendationButton.setEnabled(false);
        
        String errorMessage = "❌ Error during AI analysis: " + throwable.getMessage() + 
                             "\n\nPlease check your internet connection and try again.";
        recommendationArea.setText(errorMessage);
        
        LoggerUtil.error(PortfolioOverviewPanel.class, "Error during AI portfolio analysis", throwable);
    }
    
    private void handleApplyRecommendation(ActionEvent e) {
        if (currentRecommendation == null || !currentRecommendation.isValid()) {
            JOptionPane.showMessageDialog(this, 
                "No valid recommendation available to apply.", 
                "No Recommendation", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show confirmation dialog
        String confirmMessage = String.format(
            "Are you sure you want to apply the AI recommendation?\n\n" +
            "This will update your portfolio targets based on the AI analysis.\n" +
            "Additional investment: %s\n\n" +
            "Note: This updates your target allocations, not actual holdings.",
            priceFormat.format(currentRecommendation.getNewMoneyAmount())
        );
        
        int result = JOptionPane.showConfirmDialog(this, 
            confirmMessage, 
            "Apply AI Recommendation", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            // Apply the recommendation
            boolean success = PortfolioRebalanceService.applyRecommendation(dataManager, currentRecommendation);
            
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "✅ AI recommendation applied successfully!\n\nYour portfolio targets have been updated based on the AI analysis.", 
                    "Recommendation Applied", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh data
                dataManager.savePortfolioData();
                refreshData();
                currentRecommendation = null;
                applyRecommendationButton.setEnabled(false);
                
                LoggerUtil.info(PortfolioOverviewPanel.class, "AI portfolio recommendation applied successfully");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "❌ Failed to apply recommendation.\n\nPlease try again or check the application logs for more details.", 
                    "Application Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Custom table cell renderer for the overview table
     */
    private class OverviewTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Set font
            c.setFont(SMALL_FONT);
            
            // Color coding for P&L columns
            if (column == 5 || column == 6) { // P&L columns
                try {
                    String strValue = value.toString().replace("$", "").replace("%", "").replace(",", "");
                    double numValue = Double.parseDouble(strValue);
                    if (!isSelected) {
                        c.setForeground(numValue >= 0 ? SUCCESS_COLOR : DANGER_COLOR);
                    }
                } catch (NumberFormatException e) {
                    // Keep default color
                }
            } else {
                if (!isSelected) {
                    c.setForeground(TEXT_PRIMARY);
                }
            }
            
            // Percentage formatting for portfolio percentage column
            if (column == 4 && value instanceof String) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            
            return c;
        }
    }
    
    /**
     * Custom pie chart panel
     */
    private class PieChartPanel extends JPanel {
        private List<CryptoData> cryptoData;
        private double totalValue;
        
        public PieChartPanel(List<CryptoData> cryptoData, double totalValue) {
            this.cryptoData = cryptoData;
            this.totalValue = totalValue;
            setBackground(SURFACE_COLOR);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (cryptoData == null || totalValue <= 0) {
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height) - 40;
            int x = (width - size) / 2;
            int y = (height - size) / 2;
            
            // Draw pie slices
            double startAngle = 0;
            int colorIndex = 0;
            
            for (CryptoData crypto : cryptoData) {
                if (crypto.getTotalValue() > 0) {
                    double percentage = crypto.getTotalValue() / totalValue;
                    double arcAngle = 360 * percentage;
                    
                    if (percentage >= 0.01) { // Only draw slices >= 1%
                        g2d.setColor(getChartColor(colorIndex));
                        g2d.fillArc(x, y, size, size, (int) startAngle, (int) arcAngle);
                        
                        // Draw border
                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.drawArc(x, y, size, size, (int) startAngle, (int) arcAngle);
                        
                        colorIndex++;
                    }
                    startAngle += arcAngle;
                }
            }
            
            // Draw outer border
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, size, size);
            
            g2d.dispose();
        }
    }
}
