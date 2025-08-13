package ui.dialog;

import model.WatchlistData;
import model.TechnicalIndicators;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Dialog for displaying detailed technical analysis for watchlist items
 */
public class TechnicalAnalysisDetailDialog extends JDialog {
    
    private WatchlistData watchlistItem;
    private DecimalFormat percentFormat = new DecimalFormat("+#0.00%;-#0.00%");
    private DecimalFormat priceFormat = new DecimalFormat("$#,##0.00");
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    
    public TechnicalAnalysisDetailDialog(JFrame parent, WatchlistData item) {
        super(parent, "Technical Analysis - " + item.symbol, true);
        this.watchlistItem = item;
        
        setupUI();
        
        setSize(600, 700);
        setLocationRelativeTo(parent);
        setResizable(true);
    }
    
    /**
     * Setup the dialog UI
     */
    private void setupUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Create header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Create main content
        JScrollPane scrollPane = new JScrollPane(createContentPanel());
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create button panel
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Create header panel with crypto info
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Crypto info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(PRIMARY_COLOR);
        
        JLabel symbolLabel = new JLabel(watchlistItem.symbol + " - " + watchlistItem.name);
        symbolLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        symbolLabel.setForeground(Color.WHITE);
        symbolLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel priceLabel = new JLabel("Current: " + priceFormat.format(watchlistItem.currentPrice) + 
                                     " | Target: " + priceFormat.format(watchlistItem.expectedEntry));
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        priceLabel.setForeground(Color.WHITE);
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Entry status
        String statusText = watchlistItem.getEntryStatus().toString();
        JLabel statusLabel = new JLabel("Entry Status: " + statusText);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoPanel.add(symbolLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(statusLabel);
        
        headerPanel.add(infoPanel, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    /**
     * Create main content panel
     */
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Entry Analysis Section
        contentPanel.add(createEntryAnalysisPanel());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Technical Indicators Section
        contentPanel.add(createTechnicalIndicatorsPanel());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Price Targets Section
        contentPanel.add(createPriceTargetsPanel());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Recommendation Section
        contentPanel.add(createRecommendationPanel());
        
        return contentPanel;
    }
    
    /**
     * Create entry analysis panel
     */
    private JPanel createEntryAnalysisPanel() {
        JPanel panel = createSectionPanel("📊 Entry Analysis");
        
        // Entry opportunity metrics
        double entryOpportunity = watchlistItem.getEntryOpportunityPercentage();
        double entryScore = watchlistItem.getEntryOpportunityScore();
        
        addMetric(panel, "Entry Opportunity:", percentFormat.format(entryOpportunity), 
                 getColorForPercentage(entryOpportunity, true));
        addMetric(panel, "Entry Quality Score:", String.format("%.0f/100", entryScore), 
                 getColorForScore(entryScore));
        addMetric(panel, "Days Tracked:", watchlistItem.getDaysSinceAdded() + " days", TEXT_SECONDARY);
        
        // Entry recommendation
        String recommendation = getEntryRecommendation();
        addMetric(panel, "Recommendation:", recommendation, getRecommendationColor(recommendation));
        
        return panel;
    }
    
    /**
     * Create technical indicators panel
     */
    private JPanel createTechnicalIndicatorsPanel() {
        JPanel panel = createSectionPanel("📈 Technical Indicators");
        
        if (watchlistItem.hasTechnicalAnalysis()) {
            TechnicalIndicators indicators = watchlistItem.technicalIndicators;
            
            // RSI
            if (indicators.getRsi() > 0) {
                addMetric(panel, "RSI (14):", String.format("%.1f", indicators.getRsi()), 
                         getRSIColor(indicators.getRsi()));
                addIndicatorBar(panel, indicators.getRsi(), 0, 100);
            }
            
            // Moving Averages
            if (indicators.getSma20() > 0) {
                double ma20Diff = (watchlistItem.currentPrice - indicators.getSma20()) / indicators.getSma20();
                addMetric(panel, "Price vs SMA20:", percentFormat.format(ma20Diff), 
                         getColorForPercentage(ma20Diff, false));
            }
            
            if (indicators.getSma50() > 0) {
                double ma50Diff = (watchlistItem.currentPrice - indicators.getSma50()) / indicators.getSma50();
                addMetric(panel, "Price vs SMA50:", percentFormat.format(ma50Diff), 
                         getColorForPercentage(ma50Diff, false));
            }
            
            // Volume indicators
            if (indicators.getVolumeRatio() > 0) {
                addMetric(panel, "Volume Ratio:", String.format("%.2fx", indicators.getVolumeRatio()), 
                         getVolumeColor(indicators.getVolumeRatio()));
            }
            
        } else {
            JLabel noDataLabel = new JLabel("Technical analysis not available");
            noDataLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            noDataLabel.setForeground(TEXT_SECONDARY);
            panel.add(noDataLabel);
        }
        
        return panel;
    }
    
    /**
     * Create price targets panel
     */
    private JPanel createPriceTargetsPanel() {
        JPanel panel = createSectionPanel("🎯 Price Targets");
        
        // Current vs Entry Target
        double entryDiff = (watchlistItem.currentPrice - watchlistItem.expectedEntry) / watchlistItem.expectedEntry;
        addMetric(panel, "Entry Target:", priceFormat.format(watchlistItem.expectedEntry), TEXT_PRIMARY);
        addMetric(panel, "Distance to Entry:", percentFormat.format(entryDiff), 
                 getColorForPercentage(entryDiff, true));
        
        // 3-Month Target
        if (watchlistItem.targetPrice3Month > 0) {
            double upside3M = watchlistItem.getPotentialUpside3Month();
            addMetric(panel, "3-Month Target:", priceFormat.format(watchlistItem.targetPrice3Month), TEXT_PRIMARY);
            addMetric(panel, "3-Month Upside:", percentFormat.format(upside3M), 
                     getColorForPercentage(upside3M, false));
        }
        
        // Long-Term Target
        if (watchlistItem.targetPriceLongTerm > 0) {
            double upsideLT = watchlistItem.getPotentialUpsideLongTerm();
            addMetric(panel, "Long-Term Target:", priceFormat.format(watchlistItem.targetPriceLongTerm), TEXT_PRIMARY);
            addMetric(panel, "Long-Term Upside:", percentFormat.format(upsideLT), 
                     getColorForPercentage(upsideLT, false));
        }
        
        return panel;
    }
    
    /**
     * Create recommendation panel
     */
    private JPanel createRecommendationPanel() {
        JPanel panel = createSectionPanel("💡 AI Analysis");
        
        // AI Advice
        String aiAdvice = watchlistItem.getAiAdviceWithStatus();
        JTextArea adviceArea = new JTextArea(aiAdvice);
        adviceArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        adviceArea.setForeground(TEXT_PRIMARY);
        adviceArea.setBackground(SURFACE_COLOR);
        adviceArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        adviceArea.setLineWrap(true);
        adviceArea.setWrapStyleWord(true);
        adviceArea.setEditable(false);
        
        panel.add(adviceArea);
        
        // Notes if available
        if (watchlistItem.notes != null && !watchlistItem.notes.trim().isEmpty()) {
            panel.add(Box.createVerticalStrut(10));
            JLabel notesLabel = new JLabel("Notes:");
            notesLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            notesLabel.setForeground(TEXT_PRIMARY);
            panel.add(notesLabel);
            
            JTextArea notesArea = new JTextArea(watchlistItem.notes);
            notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            notesArea.setForeground(TEXT_SECONDARY);
            notesArea.setBackground(new Color(245, 245, 245));
            notesArea.setBorder(new EmptyBorder(10, 10, 10, 10));
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            notesArea.setEditable(false);
            panel.add(notesArea);
        }
        
        return panel;
    }
    
    /**
     * Create a section panel with title
     */
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE_COLOR);
        panel.setBorder(new LineBorder(new Color(230, 230, 230), 1));
        
        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        titlePanel.setBackground(new Color(245, 245, 247));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_PRIMARY);
        titlePanel.add(titleLabel);
        
        panel.add(titlePanel);
        
        // Content area
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SURFACE_COLOR);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.add(contentPanel);
        
        return contentPanel;
    }
    
    /**
     * Add a metric row to a panel
     */
    private void addMetric(JPanel panel, String label, String value, Color valueColor) {
        JPanel metricPanel = new JPanel(new BorderLayout());
        metricPanel.setBackground(SURFACE_COLOR);
        metricPanel.setBorder(new EmptyBorder(3, 0, 3, 0));
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        labelComponent.setForeground(TEXT_SECONDARY);
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valueComponent.setForeground(valueColor);
        
        metricPanel.add(labelComponent, BorderLayout.WEST);
        metricPanel.add(valueComponent, BorderLayout.EAST);
        
        panel.add(metricPanel);
    }
    
    /**
     * Add an indicator bar (for RSI, etc.)
     */
    private void addIndicatorBar(JPanel panel, double value, double min, double max) {
        JPanel barPanel = new JPanel();
        barPanel.setLayout(new BoxLayout(barPanel, BoxLayout.X_AXIS));
        barPanel.setBackground(SURFACE_COLOR);
        barPanel.setBorder(new EmptyBorder(5, 0, 10, 0));
        
        // Create progress bar
        JProgressBar progressBar = new JProgressBar((int)min, (int)max);
        progressBar.setValue((int)value);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(200, 8));
        
        // Color based on value
        if (value < 30) {
            progressBar.setForeground(SUCCESS_COLOR); // Oversold - good
        } else if (value > 70) {
            progressBar.setForeground(DANGER_COLOR); // Overbought - bad
        } else {
            progressBar.setForeground(WARNING_COLOR); // Neutral
        }
        
        barPanel.add(progressBar);
        panel.add(barPanel);
    }
    
    /**
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.setPreferredSize(new Dimension(80, 35));
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(closeButton);
        return buttonPanel;
    }
    
    // Helper methods for color coding
    
    private Color getColorForPercentage(double percentage, boolean reverseLogic) {
        if (reverseLogic) {
            // For entry opportunity - negative is good (below target)
            if (percentage <= -0.10) return SUCCESS_COLOR;
            else if (percentage <= -0.05) return new Color(139, 195, 74);
            else if (percentage <= 0.05) return WARNING_COLOR;
            else return DANGER_COLOR;
        } else {
            // For upside - positive is good
            if (percentage >= 0.20) return SUCCESS_COLOR;
            else if (percentage >= 0.10) return new Color(139, 195, 74);
            else if (percentage >= 0) return WARNING_COLOR;
            else return DANGER_COLOR;
        }
    }
    
    private Color getColorForScore(double score) {
        if (score >= 80) return SUCCESS_COLOR;
        else if (score >= 60) return new Color(139, 195, 74);
        else if (score >= 40) return WARNING_COLOR;
        else return DANGER_COLOR;
    }
    
    private Color getRSIColor(double rsi) {
        if (rsi <= 30) return SUCCESS_COLOR; // Oversold - good entry
        else if (rsi >= 70) return DANGER_COLOR; // Overbought - avoid
        else return WARNING_COLOR; // Neutral
    }
    
    private Color getVolumeColor(double ratio) {
        if (ratio >= 1.5) return SUCCESS_COLOR; // High volume
        else if (ratio >= 1.0) return WARNING_COLOR; // Normal volume
        else return TEXT_SECONDARY; // Low volume
    }
    
    private String getEntryRecommendation() {
        double score = watchlistItem.getEntryOpportunityScore();
        if (score >= 85) return "STRONG BUY";
        else if (score >= 70) return "BUY";
        else if (score >= 40) return "HOLD/WAIT";
        else return "AVOID";
    }
    
    private Color getRecommendationColor(String recommendation) {
        switch (recommendation) {
            case "STRONG BUY": return SUCCESS_COLOR;
            case "BUY": return new Color(139, 195, 74);
            case "HOLD/WAIT": return WARNING_COLOR;
            case "AVOID": return DANGER_COLOR;
            default: return TEXT_PRIMARY;
        }
    }
}
