import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dedicated dialog class for displaying AI analysis of cryptocurrencies.
 * This class focuses purely on AI analysis without fallback functionality.
 */
public class AiAnalysisDialog {
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    
    // Dialog instance
    private JDialog dialog;
    private JTextArea analysisArea; // Keep for compatibility
    private JTextPane analysisPane; // For HTML formatted display
    private CryptoData crypto;
    private Component parentComponent;
    
    /**
     * Constructor for AiAnalysisDialog
     * @param parent The parent component to center the dialog relative to
     * @param crypto The cryptocurrency data to analyze
     */
    public AiAnalysisDialog(Component parent, CryptoData crypto) {
        this.parentComponent = parent;
        this.crypto = crypto;
        LoggerUtil.info(AiAnalysisDialog.class, "Creating AI Analysis Dialog for " + crypto.symbol);
        initializeDialog();
    }
    
    /**
     * Initialize and setup the dialog
     */
    private void initializeDialog() {
        // Create the main dialog
        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parentComponent), 
                           "AI Analysis for " + crypto.name + " (" + crypto.symbol + ")", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SURFACE_COLOR);
        
        // Create and add components
        dialog.add(createTitlePanel(), BorderLayout.NORTH);
        dialog.add(createAnalysisPanel(), BorderLayout.CENTER);
        dialog.add(createButtonPanel(), BorderLayout.SOUTH);
        
        // Configure dialog properties
        configureDialog();
        
        LoggerUtil.debug(AiAnalysisDialog.class, "AI Analysis Dialog initialized for " + crypto.symbol);
    }
    
    /**
     * Create the title panel with cryptocurrency information
     */
    private JPanel createTitlePanel() {
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
        
        return titlePanel;
    }
    
    /**
     * Create the analysis content panel with enhanced UI
     */
    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 249, 250));
        
        // Create header panel with crypto info
        JPanel headerPanel = createCryptoInfoPanel();
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create analysis text pane (using JTextPane for better formatting)
        JTextPane analysisPane = new JTextPane();
        analysisPane.setContentType("text/html");
        analysisPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        analysisPane.setBackground(Color.WHITE);
        analysisPane.setForeground(TEXT_PRIMARY);
        analysisPane.setEditable(false);
        analysisPane.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        // Store reference for updates
        this.analysisArea = new JTextArea(); // Keep for compatibility
        
        // Set initial loading content with HTML formatting
        String initialContent = createLoadingHTML();
        analysisPane.setText(initialContent);
        
        // Create styled scroll pane
        JScrollPane scrollPane = new JScrollPane(analysisPane);
        scrollPane.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            new EmptyBorder(10, 15, 15, 15),
            javax.swing.BorderFactory.createLineBorder(new Color(230, 230, 230), 1)
        ));
        scrollPane.setPreferredSize(new Dimension(650, 500));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(Color.WHITE);
        
        // Store reference to the text pane for updates
        panel.putClientProperty("analysisPane", analysisPane);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create crypto information header panel
     */
    private JPanel createCryptoInfoPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(248, 249, 250));
        headerPanel.setBorder(new EmptyBorder(15, 20, 10, 20));
        
        // Left side - crypto basics
        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftInfo.setBackground(new Color(248, 249, 250));
        
        JLabel priceLabel = new JLabel(String.format("💰 $%.2f", crypto.currentPrice));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        priceLabel.setForeground(new Color(34, 139, 34));
        
        JLabel holdingsLabel = new JLabel(String.format("  📊 %.4f %s", crypto.holdings, crypto.symbol));
        holdingsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        holdingsLabel.setForeground(new Color(108, 117, 125));
        
        leftInfo.add(priceLabel);
        leftInfo.add(holdingsLabel);
        
        // Center - Cache status
        JPanel centerInfo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerInfo.setBackground(new Color(248, 249, 250));
        
        String cacheInfo = AiResponseCache.getCacheInfo(crypto.symbol);
        JLabel cacheLabel = new JLabel("🔄 " + cacheInfo);
        cacheLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cacheLabel.setForeground(new Color(108, 117, 125));
        
        centerInfo.add(cacheLabel);
        
        // Right side - P&L
        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightInfo.setBackground(new Color(248, 249, 250));
        
        double profitLoss = crypto.getProfitLossPercentage() * 100;
        String plText = String.format("📈 %.2f%%", profitLoss);
        Color plColor = profitLoss >= 0 ? new Color(34, 139, 34) : new Color(220, 53, 69);
        String plEmoji = profitLoss >= 0 ? "📈" : "📉";
        
        JLabel plLabel = new JLabel(String.format("%s %.2f%%", plEmoji, profitLoss));
        plLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        plLabel.setForeground(plColor);
        
        rightInfo.add(plLabel);
        
        headerPanel.add(leftInfo, BorderLayout.WEST);
        headerPanel.add(centerInfo, BorderLayout.CENTER);
        headerPanel.add(rightInfo, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Create HTML loading content
     */
    private String createLoadingHTML() {
        return "<html><body style='font-family: Segoe UI, Arial, sans-serif; padding: 20px; background-color: white;'>" +
               "<div style='text-align: center; margin-bottom: 30px;'>" +
               "<h2 style='color: #1976D2; margin-bottom: 10px;'>🤖 AI Analysis Generation</h2>" +
               "<div style='background: linear-gradient(90deg, #E3F2FD, #BBDEFB, #E3F2FD); height: 4px; border-radius: 2px; margin: 10px 0;'></div>" +
               "</div>" +
               "<div style='text-align: center; padding: 40px; background-color: #F8F9FA; border-radius: 12px; border: 2px dashed #1976D2;'>" +
               "<div style='font-size: 48px; margin-bottom: 20px;'>🔄</div>" +
               "<h3 style='color: #1976D2; margin-bottom: 15px;'>Analyzing " + crypto.name + "</h3>" +
               "<p style='color: #6C757D; font-size: 14px; line-height: 1.6;'>" +
               "Our AI is processing market data, technical indicators,<br>" +
               "and investment metrics to provide comprehensive analysis..." +
               "</p>" +
               "<div style='margin-top: 20px;'>" +
               "<div style='background: #1976D2; height: 3px; border-radius: 2px; animation: pulse 2s infinite;'></div>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }
    
    /**
     * Create the button panel with action buttons
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(SURFACE_COLOR);
        
        JButton refreshButton = createModernButton("🔄 Refresh Analysis", PRIMARY_COLOR);
        JButton forceRefreshButton = createModernButton("⚡ Force Fresh AI", new Color(255, 152, 0));
        JButton closeButton = createModernButton("✅ Close", new Color(108, 117, 125));
        
        // Setup button event handlers
        refreshButton.addActionListener(e -> refreshAnalysis(false));
        forceRefreshButton.addActionListener(e -> refreshAnalysis(true));
        closeButton.addActionListener(e -> closeDialog());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(forceRefreshButton);
        buttonPanel.add(closeButton);
        
        return buttonPanel;
    }
    
    /**
     * Create a modern styled button
     * @param text The button text
     * @param bgColor The background color
     * @return The styled button
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
        
        // Add hover effect
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
     * Configure dialog properties
     */
    private void configureDialog() {
        dialog.setSize(650, 700);
        dialog.setLocationRelativeTo(parentComponent);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }
    
    /**
     * Refresh the AI analysis
     * @param forceRefresh True to bypass cache and get fresh AI response
     */
    private void refreshAnalysis(boolean forceRefresh) {
        LoggerUtil.info(AiAnalysisDialog.class, "Refreshing AI analysis for " + crypto.symbol + (forceRefresh ? " (forced refresh)" : ""));
        
        // Get the analysis pane
        JTextPane analysisPane = getAnalysisPane();
        if (analysisPane != null) {
            analysisPane.setText(createRefreshingHTML(forceRefresh));
        }
        
        // Generate new analysis in background
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                LoggerUtil.debug(AiAnalysisDialog.class, "Generating AI analysis for " + crypto.symbol);
                return AiAdviceService.getDetailedAnalysis(crypto, forceRefresh);
            }
            
            @Override
            protected void done() {
                try {
                    String analysis = get();
                    JTextPane pane = getAnalysisPane();
                    if (pane != null) {
                        // Convert plain text analysis to formatted HTML
                        String htmlContent = convertAnalysisToHTML(analysis);
                        pane.setText(htmlContent);
                        pane.setCaretPosition(0); // Scroll to top
                    }
                    
                    // Check if we got a real AI response or just an error
                    if (analysis != null && !analysis.startsWith("AI analysis unavailable") && !analysis.startsWith("Error getting AI analysis")) {
                        LoggerUtil.info(AiAnalysisDialog.class, "AI analysis refreshed successfully for " + crypto.symbol);
                    } else {
                        LoggerUtil.warning(AiAnalysisDialog.class, "AI analysis failed for " + crypto.symbol);
                    }
                } catch (Exception ex) {
                    JTextPane pane = getAnalysisPane();
                    if (pane != null) {
                        String errorHTML = createErrorHTML("Error generating analysis: " + ex.getMessage());
                        pane.setText(errorHTML);
                    }
                    LoggerUtil.error(AiAnalysisDialog.class, "Failed to refresh AI analysis for " + crypto.symbol, ex);
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Generate and load the initial AI analysis
     */
    private void loadInitialAnalysis() {
        LoggerUtil.info(AiAnalysisDialog.class, "Loading initial AI analysis for " + crypto.symbol);
        
        // Generate analysis in background
        SwingWorker<String, Void> analysisWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                LoggerUtil.debug(AiAnalysisDialog.class, "Generating initial AI analysis for " + crypto.symbol);
                return AiAdviceService.getDetailedAnalysis(crypto, false); // Use cache by default
            }
            
            @Override
            protected void done() {
                try {
                    String analysis = get();
                    JTextPane pane = getAnalysisPane();
                    if (pane != null) {
                        // Convert plain text analysis to formatted HTML
                        String htmlContent = convertAnalysisToHTML(analysis);
                        pane.setText(htmlContent);
                        pane.setCaretPosition(0); // Scroll to top
                    }
                    
                    // Check if we got a real AI response or just an error
                    if (analysis != null && !analysis.startsWith("AI analysis unavailable") && !analysis.startsWith("Error getting AI analysis")) {
                        LoggerUtil.info(AiAnalysisDialog.class, "Initial AI analysis loaded successfully for " + crypto.symbol);
                    } else {
                        LoggerUtil.warning(AiAnalysisDialog.class, "AI analysis failed for " + crypto.symbol);
                    }
                } catch (Exception ex) {
                    JTextPane pane = getAnalysisPane();
                    if (pane != null) {
                        String errorHTML = createErrorHTML("Error generating analysis: " + ex.getMessage());
                        pane.setText(errorHTML);
                    }
                    LoggerUtil.error(AiAnalysisDialog.class, "Failed to load initial AI analysis for " + crypto.symbol, ex);
                }
            }
        };
        analysisWorker.execute();
    }
    
    /**
     * Close the dialog
     */
    private void closeDialog() {
        LoggerUtil.debug(AiAnalysisDialog.class, "Closing AI Analysis Dialog for " + crypto.symbol);
        if (dialog != null) {
            dialog.dispose();
        }
    }
    
    /**
     * Show the dialog and load the initial analysis
     */
    public void showDialog() {
        LoggerUtil.info(AiAnalysisDialog.class, "Showing AI Analysis Dialog for " + crypto.symbol);
        
        // Load initial analysis
        loadInitialAnalysis();
        
        // Show the dialog
        SwingUtilities.invokeLater(() -> {
            dialog.setVisible(true);
        });
    }
    
    /**
     * Get the dialog instance (for testing purposes)
     */
    public JDialog getDialog() {
        return dialog;
    }
    
    /**
     * Get the analysis pane from the dialog
     */
    private JTextPane getAnalysisPane() {
        if (dialog != null) {
            Component[] components = dialog.getContentPane().getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    Object pane = panel.getClientProperty("analysisPane");
                    if (pane instanceof JTextPane) {
                        return (JTextPane) pane;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Convert plain text analysis to formatted HTML
     */
    private String convertAnalysisToHTML(String analysis) {
        if (analysis == null || analysis.trim().isEmpty()) {
            return createErrorHTML("No analysis data available");
        }
        
        // Check if it's an error message - show simple error
        if (analysis.startsWith("AI analysis unavailable") || analysis.startsWith("Error getting AI analysis")) {
            return createErrorHTML(analysis);
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Segoe UI, Arial, sans-serif; line-height: 1.6; padding: 20px; background-color: white;'>");
        
        // Split analysis into sections
        String[] lines = analysis.split("\n");
        boolean inSection = false;
        String currentSection = "";
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check for section headers (lines with ═══ or similar patterns)
            if (line.contains("═══") || line.contains("---")) {
                if (inSection) {
                    html.append("</div>");
                }
                continue;
            }
            
            // Check for major section headers (ALL CAPS or specific patterns)
            if (line.matches("^[🔥📊💰📈🎯🔍][A-Z ]+.*") || 
                line.matches("^[A-Z][A-Z ]{10,}.*") ||
                line.contains("ANALYSIS") || line.contains("SUMMARY") || 
                line.contains("RECOMMENDATION") || line.contains("POSITION")) {
                
                if (inSection) {
                    html.append("</div>");
                }
                
                // Start new section
                html.append("<div style='margin-bottom: 25px;'>");
                html.append("<h3 style='color: #1976D2; margin-bottom: 15px; padding-bottom: 8px; border-bottom: 2px solid #E3F2FD;'>");
                html.append(formatSectionHeader(line));
                html.append("</h3>");
                inSection = true;
                currentSection = line;
                continue;
            }
            
            // Format regular content lines
            if (inSection) {
                if (line.startsWith("•") || line.startsWith("-")) {
                    // Bullet points
                    html.append("<div style='margin: 8px 0; padding-left: 20px;'>");
                    html.append("<span style='color: #1976D2; font-weight: bold;'>▸</span> ");
                    html.append(formatContentLine(line.substring(1).trim()));
                    html.append("</div>");
                } else if (line.contains(":")) {
                    // Key-value pairs
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        html.append("<div style='margin: 8px 0; padding: 10px; background-color: #F8F9FA; border-radius: 6px;'>");
                        html.append("<strong style='color: #495057;'>").append(parts[0].trim()).append(":</strong> ");
                        html.append("<span style='color: #6C757D;'>").append(formatContentLine(parts[1].trim())).append("</span>");
                        html.append("</div>");
                    } else {
                        html.append("<p style='margin: 8px 0; color: #495057;'>").append(formatContentLine(line)).append("</p>");
                    }
                } else {
                    // Regular paragraphs
                    html.append("<p style='margin: 8px 0; color: #495057;'>").append(formatContentLine(line)).append("</p>");
                }
            } else {
                // Content outside sections
                html.append("<div style='margin: 10px 0; padding: 15px; background-color: #F8F9FA; border-radius: 8px; border-left: 4px solid #1976D2;'>");
                html.append(formatContentLine(line));
                html.append("</div>");
            }
        }
        
        if (inSection) {
            html.append("</div>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Format section headers with icons
     */
    private String formatSectionHeader(String header) {
        // Clean up the header and add appropriate styling
        String cleaned = header.replaceAll("[═─━]+", "").trim();
        
        // Add appropriate icons based on content
        if (cleaned.contains("POSITION") || cleaned.contains("SUMMARY")) {
            return "📊 " + cleaned;
        } else if (cleaned.contains("RECOMMENDATION") || cleaned.contains("ACTION")) {
            return "💡 " + cleaned;
        } else if (cleaned.contains("RISK") || cleaned.contains("WARNING")) {
            return "⚠️ " + cleaned;
        } else if (cleaned.contains("PRICE") || cleaned.contains("TARGET")) {
            return "🎯 " + cleaned;
        } else if (cleaned.contains("ANALYSIS") || cleaned.contains("TECHNICAL")) {
            return "🔍 " + cleaned;
        } else {
            return "📈 " + cleaned;
        }
    }
    
    /**
     * Format content lines with emphasis and colors
     */
    private String formatContentLine(String content) {
        if (content == null) return "";
        
        try {
            // Format percentages
            content = content.replaceAll("([+-]?\\d+\\.?\\d*)%", "<strong style='color: #28A745;'>$1%</strong>");
            
            // Format currency amounts - fix the dollar sign escaping
            content = content.replaceAll("\\$([\\d,]+\\.?\\d*)", "<strong style='color: #17A2B8;'>\\$$1</strong>");
            
            // Format positive indicators
            content = content.replaceAll("(BUY|STRONG BUY|HOLD|POSITIVE|BULLISH|UP|INCREASE)", 
                                       "<span style='color: #28A745; font-weight: bold;'>$1</span>");
            
            // Format negative indicators
            content = content.replaceAll("(SELL|STRONG SELL|AVOID|NEGATIVE|BEARISH|DOWN|DECREASE)", 
                                       "<span style='color: #DC3545; font-weight: bold;'>$1</span>");
            
            // Format neutral indicators
            content = content.replaceAll("(NEUTRAL|WAIT|MONITOR|WATCH)", 
                                       "<span style='color: #FFC107; font-weight: bold;'>$1</span>");
            
        } catch (Exception e) {
            LoggerUtil.error(AiAnalysisDialog.class, "Error formatting content line: " + e.getMessage());
            // Return original content if formatting fails
        }
        
        return content;
    }
    
    /**
     * Create HTML refreshing content
     */
    private String createRefreshingHTML(boolean forceRefresh) {
        String refreshType = forceRefresh ? "Fresh AI Analysis" : "Refreshing Analysis";
        String description = forceRefresh ? 
            "Bypassing cache to get the latest AI insights..." : 
            "Checking for updates and latest analysis...";
            
        return "<html><body style='font-family: Segoe UI, Arial, sans-serif; padding: 20px; background-color: white;'>" +
               "<div style='text-align: center; margin-bottom: 30px;'>" +
               "<h2 style='color: #FF9800; margin-bottom: 10px;'>🔄 " + refreshType + "</h2>" +
               "<div style='background: linear-gradient(90deg, #FFE0B2, #FFCC80, #FFE0B2); height: 4px; border-radius: 2px; margin: 10px 0;'></div>" +
               "</div>" +
               "<div style='text-align: center; padding: 40px; background-color: #FFF8E1; border-radius: 12px; border: 2px dashed #FF9800;'>" +
               "<div style='font-size: 48px; margin-bottom: 20px;'>⚡</div>" +
               "<h3 style='color: #FF9800; margin-bottom: 15px;'>Updating " + crypto.name + "</h3>" +
               "<p style='color: #F57C00; font-size: 14px; line-height: 1.6;'>" +
               description +
               "</p>" +
               "<div style='margin-top: 20px;'>" +
               "<div style='background: #FF9800; height: 3px; border-radius: 2px; animation: pulse 2s infinite;'></div>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }

    /**
     * Create error HTML content
     */
    private String createErrorHTML(String errorMessage) {
        return "<html><body style='font-family: Segoe UI, Arial, sans-serif; padding: 20px; background-color: white;'>" +
               "<div style='text-align: center; margin-bottom: 30px;'>" +
               "<h2 style='color: #F44336; margin-bottom: 10px;'>❌ Analysis Error</h2>" +
               "<div style='background: linear-gradient(90deg, #FFEBEE, #FFCDD2, #FFEBEE); height: 4px; border-radius: 2px; margin: 10px 0;'></div>" +
               "</div>" +
               "<div style='text-align: center; padding: 40px; background-color: #FFEBEE; border-radius: 12px; border: 2px solid #F44336;'>" +
               "<div style='font-size: 48px; margin-bottom: 20px;'>⚠️</div>" +
               "<h3 style='color: #F44336; margin-bottom: 15px;'>Unable to Generate Analysis</h3>" +
               "<p style='color: #D32F2F; font-size: 14px; line-height: 1.6;'>" +
               errorMessage +
               "</p>" +
               "<div style='margin-top: 20px; padding: 15px; background-color: #FFF; border-radius: 8px; border-left: 4px solid #F44336;'>" +
               "<p style='color: #666; font-size: 12px; margin: 0;'>" +
               "💡 Try using the \"⚡ Force Fresh AI\" button to bypass the cache and get a new analysis." +
               "</p>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }

    /**
     * Create circuit breaker status HTML
     */
    private String createCircuitBreakerHTML() {
        return "<html><body style='font-family: Segoe UI, Arial, sans-serif; padding: 20px; background-color: white;'>" +
               "<div style='text-align: center; margin-bottom: 30px;'>" +
               "<h2 style='color: #FF6B35; margin-bottom: 10px;'>🚫 AI Service Temporarily Disabled</h2>" +
               "<div style='background: linear-gradient(90deg, #FFE5D9, #FFCAB0, #FFE5D9); height: 4px; border-radius: 2px; margin: 10px 0;'></div>" +
               "</div>" +
               "<div style='text-align: center; padding: 40px; background-color: #FFF3E0; border-radius: 12px; border: 2px solid #FF6B35;'>" +
               "<div style='font-size: 48px; margin-bottom: 20px;'>⏱️</div>" +
               "<h3 style='color: #FF6B35; margin-bottom: 15px;'>Rate Limit Protection Active</h3>" +
               "<p style='color: #8D6E63; font-size: 14px; line-height: 1.6;'>" +
               "The AI service has been temporarily disabled due to rate limits.<br>" +
               "This helps prevent quota exhaustion and ensures service availability.<br><br>" +
               "The service will automatically resume in a few minutes." +
               "</p>" +
               "<div style='margin-top: 20px; padding: 15px; background-color: #F3E5F5; border-radius: 8px;'>" +
               "<p style='color: #7B1FA2; font-size: 13px; margin: 0;'>" +
               "💡 <strong>Tip:</strong> In the meantime, you'll receive rule-based analysis " +
               "that provides valuable insights based on your portfolio data." +
               "</p>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }
}
