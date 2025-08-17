package ui.panel;

import service.DailyReportScheduler;
import service.EmailService;
import ui.CleanupablePanel;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dedicated Settings Panel for application configuration
 * Handles email service settings, daily reports, and application preferences
 */
public class SettingsPanel extends JPanel implements CleanupablePanel {
    
    // Modern color scheme (matching CryptoMainApp)
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);      // Blue 700
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);     // White
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);  // Light Gray
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);         // Dark Gray
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);    // Medium Gray
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);       // Green 500
    private static final Color DANGER_COLOR = new Color(244, 67, 54);        // Red 500
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);     // Light Gray
    
    // UI Components
    private JFrame parentFrame;
    private JLabel emailStatusLabel;
    private boolean isMinimizedToTray = true; // Default to enabled
    
    /**
     * Constructor for SettingsPanel
     * @param parentFrame Reference to the main application frame
     */
    public SettingsPanel(JFrame parentFrame) {
        LoggerUtil.info(SettingsPanel.class, "Initializing SettingsPanel");
        this.parentFrame = parentFrame;
        setupUI();
        LoggerUtil.info(SettingsPanel.class, "SettingsPanel initialized successfully");
    }
    
    /**
     * Setup the user interface for settings
     */
    private void setupUI() {
        LoggerUtil.info(SettingsPanel.class, "Setting up settings panel UI");
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Create main content with sections
        JPanel contentPanel = createMainContentPanel();
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create header panel with title
     */
    private JPanel createHeaderPanel() {
        LoggerUtil.debug(SettingsPanel.class, "Creating header panel");
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel headerLabel = new JLabel("⚙️ Settings");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(TEXT_PRIMARY);
        headerPanel.add(headerLabel);
        
        return headerPanel;
    }
    
    /**
     * Create main content panel with all settings sections
     */
    private JPanel createMainContentPanel() {
        LoggerUtil.info(SettingsPanel.class, "Creating main content panel");
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BACKGROUND_COLOR);
        
        // Email & Daily Reports Section (Combined)
        JPanel emailReportsSection = createEmailAndReportsSection();
        contentPanel.add(emailReportsSection);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Application Settings Section
        JPanel appSection = createApplicationSettingsSection();
        contentPanel.add(appSection);
        
        return contentPanel;
    }
    
    /**
     * Create combined email and daily reports section
     */
    private JPanel createEmailAndReportsSection() {
        LoggerUtil.info(SettingsPanel.class, "Creating email and reports section");
        JPanel section = createSettingsSection("📧 Email Service & Daily Reports", 
            "Email service is pre-configured for automated daily portfolio reports (sent at 7:00 AM)");
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SURFACE_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Combined status display
        emailStatusLabel = new JLabel();
        emailStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        updateCombinedStatus();
        
        // Buttons section
        JPanel buttonPanel = createEmailButtonsPanel();
        
        contentPanel.add(emailStatusLabel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(buttonPanel);
        
        section.add(contentPanel, BorderLayout.CENTER);
        return section;
    }
    
    /**
     * Create buttons panel for email and reports section
     */
    private JPanel createEmailButtonsPanel() {
        LoggerUtil.debug(SettingsPanel.class, "Creating email buttons panel");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        buttonPanel.setBackground(SURFACE_COLOR);
        
        JButton testEmailButton = createModernButton("📧 Send Test Email", PRIMARY_COLOR);
        JButton testReportButton = createModernButton("📊 Send Test Report", new Color(255, 152, 0));
        JButton portfolioOverviewButton = createModernButton("🎯 Send Portfolio Overview", new Color(156, 39, 176));
        JButton watchlistEmailButton = createModernButton("👀 Send Watchlist Email", new Color(0, 150, 136));
        JButton refreshButton = createModernButton("🔄 Refresh Status", new Color(96, 125, 139));
        
        // Button actions
        setupEmailButtonActions(testEmailButton, testReportButton, portfolioOverviewButton, watchlistEmailButton, refreshButton);
        
        // Add buttons to panel
        buttonPanel.add(testEmailButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(testReportButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(portfolioOverviewButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(watchlistEmailButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(refreshButton);
        
        return buttonPanel;
    }
    
    /**
     * Setup action listeners for email buttons
     */
    private void setupEmailButtonActions(JButton testEmailButton, JButton testReportButton, 
                                       JButton portfolioOverviewButton, JButton watchlistEmailButton, JButton refreshButton) {
        LoggerUtil.debug(SettingsPanel.class, "Setting up email button actions");
        
        testEmailButton.addActionListener(e -> {
            if (!EmailService.isAvailable()) {
                showMessage("Email service is currently unavailable.", "Service Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            boolean testResult = EmailService.sendTestEmail();
            if (testResult) {
                showMessage("Test email sent successfully!", "Test Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showMessage("Failed to send test email. Please check the logs for details.", "Test Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        testReportButton.addActionListener(e -> {
            if (!EmailService.isAvailable()) {
                showMessage("Email service is currently unavailable.", "Service Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Send manual report
            DailyReportScheduler.sendManualReport();
            showMessage("Manual daily report sent! Check your email.", "Report Sent", JOptionPane.INFORMATION_MESSAGE);
        });
        
        portfolioOverviewButton.addActionListener(e -> {
            if (!EmailService.isAvailable()) {
                showMessage("Email service is currently unavailable.", "Service Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Send manual portfolio overview email
            DailyReportScheduler.sendManualPortfolioOverviewEmail();
            showMessage("Portfolio Overview email sent! Check your email.", "Portfolio Overview Sent", JOptionPane.INFORMATION_MESSAGE);
        });
        
        watchlistEmailButton.addActionListener(e -> {
            if (!EmailService.isAvailable()) {
                showMessage("Email service is currently unavailable.", "Service Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Send watchlist email
            sendWatchlistEmail();
        });
        
        refreshButton.addActionListener(e -> updateCombinedStatus());
    }
    
    /**
     * Create application settings section
     */
    private JPanel createApplicationSettingsSection() {
        LoggerUtil.info(SettingsPanel.class, "Creating application settings section");
        JPanel section = createSettingsSection("🔧 Application Settings", 
            "General application preferences and configuration");
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SURFACE_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Log level setting
        JPanel logLevelPanel = createLogLevelPanel();
        
        // Auto-refresh setting
        JPanel autoRefreshPanel = createAutoRefreshPanel();
        
        // Background mode setting
        JPanel backgroundModePanel = createBackgroundModePanel();
        
        contentPanel.add(logLevelPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(autoRefreshPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(backgroundModePanel);
        
        section.add(contentPanel, BorderLayout.CENTER);
        return section;
    }
    
    /**
     * Create log level settings panel
     */
    private JPanel createLogLevelPanel() {
        LoggerUtil.debug(SettingsPanel.class, "Creating log level panel");
        JPanel logLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logLevelPanel.setBackground(SURFACE_COLOR);
        
        JLabel logLabel = new JLabel("Log Level:");
        logLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JComboBox<String> logLevelCombo = new JComboBox<>(new String[]{"DEBUG", "INFO", "WARNING", "ERROR"});
        logLevelCombo.setSelectedItem("INFO");
        
        logLevelPanel.add(logLabel);
        logLevelPanel.add(Box.createHorizontalStrut(10));
        logLevelPanel.add(logLevelCombo);
        
        return logLevelPanel;
    }
    
    /**
     * Create auto-refresh settings panel
     */
    private JPanel createAutoRefreshPanel() {
        LoggerUtil.debug(SettingsPanel.class, "Creating auto-refresh panel");
        JPanel autoRefreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoRefreshPanel.setBackground(SURFACE_COLOR);
        
        JCheckBox autoRefreshCheck = new JCheckBox("Auto-refresh prices every 5 minutes");
        autoRefreshCheck.setBackground(SURFACE_COLOR);
        autoRefreshCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        autoRefreshPanel.add(autoRefreshCheck);
        
        return autoRefreshPanel;
    }
    
    /**
     * Create background mode settings panel
     */
    private JPanel createBackgroundModePanel() {
        LoggerUtil.debug(SettingsPanel.class, "Creating background mode panel");
        JPanel backgroundModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backgroundModePanel.setBackground(SURFACE_COLOR);
        
        JCheckBox backgroundModeCheck = new JCheckBox("Run in background (minimize to system tray)");
        backgroundModeCheck.setBackground(SURFACE_COLOR);
        backgroundModeCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backgroundModeCheck.setSelected(isMinimizedToTray); // Default to enabled for daily reports
        
        backgroundModeCheck.addActionListener(e -> {
            isMinimizedToTray = backgroundModeCheck.isSelected();
            if (isMinimizedToTray) {
                showMessage(
                    "Background mode enabled. The application will minimize to system tray\n" +
                    "and continue sending daily reports even when the window is closed.", 
                    "Background Mode", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        backgroundModePanel.add(backgroundModeCheck);
        
        return backgroundModePanel;
    }
    
    /**
     * Update combined email and daily reports status display
     */
    private void updateCombinedStatus() {
        LoggerUtil.info(SettingsPanel.class, "Updating combined status display");
        boolean isEmailAvailable = EmailService.isAvailable();
        DailyReportScheduler.SchedulerStatus schedulerStatus = DailyReportScheduler.getSchedulerStatus();
        
        String statusText = "<html><div style='font-family: Segoe UI; font-size: 12px;'>";
        statusText += "<b>📧 Email Service:</b> " + (isEmailAvailable ? "✅ Available" : "❌ Unavailable") + "<br/>";
        statusText += "<b>🔧 Configuration:</b> ✅ Pre-configured<br/>";
        statusText += "<b>📊 Scheduler Status:</b> " + (schedulerStatus.isScheduled ? "✅ Active" : "❌ Inactive") + "<br/>";
        statusText += "<b>📈 Data Available:</b> " + (schedulerStatus.dataManagerAvailable ? "✅ Yes" : "❌ No") + "<br/>";
        statusText += "<b>⏰ Next Report:</b> " + schedulerStatus.timeUntilNextReport + "<br/>";
        statusText += "<b>🎯 Daily Reports:</b> " + (isEmailAvailable && schedulerStatus.isScheduled ? "✅ Ready" : "❌ Disabled") + "<br/>";
        statusText += "</div></html>";
        
        emailStatusLabel.setText(statusText);
        
        if (isEmailAvailable && schedulerStatus.isScheduled) {
            emailStatusLabel.setForeground(SUCCESS_COLOR);
        } else {
            emailStatusLabel.setForeground(DANGER_COLOR);
        }
    }
    
    /**
     * Create a settings section with title and description
     */
    private JPanel createSettingsSection(String title, String description) {
        LoggerUtil.debug(SettingsPanel.class, "Creating settings section: " + title);
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(SURFACE_COLOR);
        section.setBorder(new LineBorder(DIVIDER_COLOR, 1, true));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 245, 247));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_SECONDARY);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.CENTER);
        
        section.add(headerPanel, BorderLayout.NORTH);
        return section;
    }
    
    /**
     * Create modern button with specified text and background color
     */
    private JButton createModernButton(String text, Color bgColor) {
        LoggerUtil.debug(SettingsPanel.class, "Creating modern button: " + text);
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    /**
     * Show message dialog with parent frame as owner
     */
    private void showMessage(String message, String title, int messageType) {
        LoggerUtil.debug(SettingsPanel.class, "Showing message: " + title);
        JOptionPane.showMessageDialog(parentFrame, message, title, messageType);
    }
    
    /**
     * Get the current background mode setting
     */
    public boolean isMinimizedToTray() {
        return isMinimizedToTray;
    }
    
    /**
     * Set the background mode setting
     */
    public void setMinimizedToTray(boolean minimizedToTray) {
        this.isMinimizedToTray = minimizedToTray;
    }
    
    /**
     * Refresh the settings display (useful when called from external sources)
     */
    public void refreshDisplay() {
        LoggerUtil.info(SettingsPanel.class, "Refreshing settings display");
        SwingUtilities.invokeLater(this::updateCombinedStatus);
    }
    
    @Override
    public void cleanup() {
        LoggerUtil.info(SettingsPanel.class, "Cleaning up SettingsPanel");
        // No specific cleanup required for settings panel
        // All button listeners will be garbage collected automatically
    }
    
    @Override
    public void activate() {
        LoggerUtil.info(SettingsPanel.class, "Activating SettingsPanel");
        // Refresh the status when panel is activated
        refreshDisplay();
    }
    
    /**
     * Send watchlist email with AI evaluation
     */
    private void sendWatchlistEmail() {
        LoggerUtil.info(SettingsPanel.class, "Sending watchlist email with AI evaluation");
        
        try {
            // Create a temporary data manager to get portfolio data
            // This approach allows us to generate watchlist emails without requiring 
            // direct dependency injection
            data.PortfolioDataManager tempDataManager = new data.PortfolioDataManager();
            java.util.List<model.CryptoData> cryptoList = tempDataManager.getCryptoList();
            
            if (cryptoList == null || cryptoList.isEmpty()) {
                LoggerUtil.warning(SettingsPanel.class, "No cryptocurrency data available for watchlist email");
                showMessage("No cryptocurrency data available for watchlist email.", 
                           "Watchlist Email", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Generate AI analysis for watchlist items
            String watchlistAnalysis = generateWatchlistAnalysis(cryptoList);
            
            // Send the watchlist email
            boolean emailSent = service.EmailService.sendWatchlistEmail(cryptoList, watchlistAnalysis);
            
            if (emailSent) {
                showMessage("Watchlist email with AI evaluation sent successfully!", 
                           "Watchlist Email Sent", JOptionPane.INFORMATION_MESSAGE);
                LoggerUtil.info(SettingsPanel.class, "Watchlist email sent successfully");
            } else {
                showMessage("Failed to send watchlist email. Please check the logs for details.", 
                           "Watchlist Email Failed", JOptionPane.ERROR_MESSAGE);
                LoggerUtil.error(SettingsPanel.class, "Failed to send watchlist email");
            }
            
        } catch (Exception e) {
            LoggerUtil.error(SettingsPanel.class, "Error sending watchlist email: " + e.getMessage(), e);
            showMessage("Failed to send watchlist email: " + e.getMessage(), 
                       "Watchlist Email Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Send a simple watchlist email using available portfolio data as a fallback
     */
    private void sendSimpleWatchlistEmail() {
        try {
            // Create a temporary data manager to get portfolio data
            data.PortfolioDataManager tempDataManager = new data.PortfolioDataManager();
            java.util.List<model.CryptoData> cryptoList = tempDataManager.getCryptoList();
            
            if (cryptoList == null || cryptoList.isEmpty()) {
                showMessage("No cryptocurrency data available for watchlist email.", 
                           "Watchlist Email", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Generate AI analysis for watchlist items
            String watchlistAnalysis = generateWatchlistAnalysis(cryptoList);
            
            // Send the watchlist email
            boolean emailSent = service.EmailService.sendWatchlistEmail(cryptoList, watchlistAnalysis);
            
            if (emailSent) {
                showMessage("Watchlist email with AI evaluation sent successfully!", 
                           "Watchlist Email Sent", JOptionPane.INFORMATION_MESSAGE);
                LoggerUtil.info(SettingsPanel.class, "Watchlist email sent successfully");
            } else {
                showMessage("Failed to send watchlist email. Please check the logs for details.", 
                           "Watchlist Email Failed", JOptionPane.ERROR_MESSAGE);
                LoggerUtil.error(SettingsPanel.class, "Failed to send watchlist email");
            }
            
        } catch (Exception e) {
            LoggerUtil.error(SettingsPanel.class, "Error in sendSimpleWatchlistEmail: " + e.getMessage(), e);
            showMessage("Error sending watchlist email: " + e.getMessage(), 
                       "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Generate AI analysis for watchlist items with detailed crypto-specific advice
     */
    private String generateWatchlistAnalysis(java.util.List<model.CryptoData> cryptoList) {
        LoggerUtil.info(SettingsPanel.class, "Generating comprehensive watchlist analysis with AI advice");
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("🎯 CRYPTO WATCHLIST - AI POWERED ENTRY OPPORTUNITIES\n");
        analysis.append("═══════════════════════════════════════════════\n\n");
        
        // Ensure all cryptos have proper target prices set
        cryptoList.forEach(this::ensureTargetPricesSet);
        
        // Filter for potential entry opportunities with better criteria
        java.util.List<model.CryptoData> opportunities = cryptoList.stream()
            .filter(crypto -> {
                double entryOpportunity = crypto.getEntryOpportunity();
                double currentPrice = crypto.currentPrice;
                double targetPrice = crypto.targetPrice3Month;
                
                // Consider items with good entry potential or significant upside
                return entryOpportunity > 0.05 || // 5% or better entry opportunity
                       (targetPrice > 0 && ((targetPrice - currentPrice) / currentPrice) > 0.15); // 15% upside potential
            })
            .sorted((a, b) -> {
                // Sort by entry opportunity first, then by upside potential
                double aScore = a.getEntryOpportunity() + getUpsidePotential(a);
                double bScore = b.getEntryOpportunity() + getUpsidePotential(b);
                return Double.compare(bScore, aScore);
            })
            .collect(java.util.stream.Collectors.toList());
        
        if (opportunities.isEmpty()) {
            // Get AI market overview analysis
            analysis.append("📊 MARKET ANALYSIS:\n");
            try {
                String marketAnalysis = getMarketOverviewAnalysis(cryptoList);
                analysis.append(marketAnalysis).append("\n\n");
            } catch (Exception e) {
                analysis.append("⚠️ Current market conditions suggest caution.\n");
                analysis.append("Consider waiting for better entry points or dollar-cost averaging.\n\n");
            }
        } else {
            analysis.append(String.format("📊 Found %d potential entry opportunities:\n\n", opportunities.size()));
            
            int count = 0;
            for (model.CryptoData crypto : opportunities) {
                if (count >= 8) break; // Limit to top 8 opportunities for readability
                
                analysis.append(String.format("🪙 %s (%s)\n", crypto.name, crypto.symbol));
                analysis.append(String.format("   💰 Current Price: $%.6f\n", crypto.currentPrice));
                analysis.append(String.format("   🎯 Entry Target: $%.6f\n", crypto.expectedEntry));
                analysis.append(String.format("   📈 3M Target: $%.6f (%.1f%% upside)\n", 
                    crypto.targetPrice3Month, 
                    getUpsidePotential(crypto) * 100));
                analysis.append(String.format("   ⚡ Entry Opportunity: %.1f%%\n", crypto.getEntryOpportunity() * 100));
                
                // Get detailed AI analysis for this specific crypto
                try {
                    String aiAdvice = getDetailedCryptoAnalysis(crypto);
                    if (!aiAdvice.isEmpty()) {
                        analysis.append("   🤖 AI Analysis:\n");
                        // Split analysis into lines and indent each
                        String[] lines = aiAdvice.split("\n");
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) {
                                analysis.append("      ").append(line.trim()).append("\n");
                            }
                        }
                    }
                } catch (Exception e) {
                    LoggerUtil.debug(SettingsPanel.class, "Could not get AI analysis for " + crypto.symbol + ": " + e.getMessage());
                    analysis.append("   🤖 AI Analysis: Technical analysis pending...\n");
                }
                
                analysis.append("\n");
                count++;
            }
        }
        
        // Add general market insights and strategy
        analysis.append("💡 WATCHLIST STRATEGY RECOMMENDATIONS:\n");
        analysis.append("• 📊 Set price alerts at entry target levels\n");
        analysis.append("• 💰 Consider dollar-cost averaging for volatile assets\n");
        analysis.append("• 📈 Monitor technical indicators and market sentiment\n");
        analysis.append("• 🛡️ Use stop-loss orders to manage downside risk\n");
        analysis.append("• ⏰ Review and adjust targets based on market conditions\n");
        analysis.append("• 🧠 Always validate AI recommendations with your own research\n\n");
        
        analysis.append("⚠️ IMPORTANT DISCLAIMER:\n");
        analysis.append("This analysis is for informational purposes only and should not be considered\n");
        analysis.append("financial advice. Cryptocurrency investments carry significant risk of loss.\n");
        analysis.append("Always conduct your own research and consider your risk tolerance before investing.\n");
        
        return analysis.toString();
    }
    
    /**
     * Extract key points from AI analysis text
     */
    private String extractKeyPointsFromAnalysis(String fullAnalysis) {
        if (fullAnalysis == null || fullAnalysis.trim().isEmpty()) {
            return "";
        }
        
        // Extract first meaningful sentence or recommendation
        String[] sentences = fullAnalysis.split("[.!?]");
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 20 && 
                (sentence.toLowerCase().contains("recommend") || 
                 sentence.toLowerCase().contains("suggest") ||
                 sentence.toLowerCase().contains("buy") ||
                 sentence.toLowerCase().contains("sell") ||
                 sentence.toLowerCase().contains("hold"))) {
                return sentence.length() > 100 ? sentence.substring(0, 97) + "..." : sentence;
            }
        }
        
        // Fallback to first substantial sentence
        if (sentences.length > 0 && sentences[0].trim().length() > 20) {
            String first = sentences[0].trim();
            return first.length() > 80 ? first.substring(0, 77) + "..." : first;
        }
        
        return "";
    }
    
    /**
     * Get available watchlist managers (placeholder for proper implementation)
     */
    private java.util.List<data.WatchlistDataManager> getWatchlistManagers() {
        // This is a placeholder - in a proper implementation, we would access
        // the WatchlistDataManager through the main application
        return new java.util.ArrayList<>();
    }
    
    /**
     * Ensure target prices are properly set for crypto data
     */
    private void ensureTargetPricesSet(model.CryptoData crypto) {
        if (crypto.expectedEntry == 0) {
            crypto.expectedEntry = crypto.currentPrice * 0.95; // 5% below current price
        }
        if (crypto.targetPrice3Month == 0) {
            crypto.targetPrice3Month = crypto.currentPrice * 1.20; // 20% above current price
        }
        if (crypto.targetPriceLongTerm == 0) {
            crypto.targetPriceLongTerm = crypto.currentPrice * 1.50; // 50% above current price
        }
    }
    
    /**
     * Get upside potential percentage for a crypto
     */
    private double getUpsidePotential(model.CryptoData crypto) {
        if (crypto.currentPrice == 0 || crypto.targetPrice3Month == 0) {
            return 0.0;
        }
        return (crypto.targetPrice3Month - crypto.currentPrice) / crypto.currentPrice;
    }
    
    /**
     * Get market overview analysis using AI
     */
    private String getMarketOverviewAnalysis(java.util.List<model.CryptoData> cryptoList) {
        try {
            LoggerUtil.info(SettingsPanel.class, "Getting AI market overview analysis");
            
            // Create a comprehensive market analysis prompt
            StringBuilder marketPrompt = new StringBuilder();
            marketPrompt.append("Analyze the current cryptocurrency watchlist market conditions and provide entry opportunities analysis.\n\n");
            marketPrompt.append("PORTFOLIO SUMMARY:\n");
            marketPrompt.append("Total Cryptocurrencies: ").append(cryptoList.size()).append("\n");
            
            double totalValue = cryptoList.stream().mapToDouble(crypto -> crypto.getTotalValue()).sum();
            marketPrompt.append("Total Portfolio Value: $").append(String.format("%.2f", totalValue)).append("\n\n");
            
            marketPrompt.append("TOP HOLDINGS:\n");
            cryptoList.stream()
                .sorted((a, b) -> Double.compare(b.getTotalValue(), a.getTotalValue()))
                .limit(5)
                .forEach(crypto -> {
                    marketPrompt.append(String.format("• %s (%s): $%.6f (Holdings: %.6f)\n", 
                        crypto.name, crypto.symbol, crypto.currentPrice, crypto.holdings));
                });
            
            marketPrompt.append("\nPLEASE PROVIDE:\n");
            marketPrompt.append("1. Overall market sentiment analysis\n");
            marketPrompt.append("2. Recommended watchlist strategy for current conditions\n");
            marketPrompt.append("3. Risk management suggestions\n");
            marketPrompt.append("4. Timing recommendations for entries\n");
            marketPrompt.append("\nKeep the analysis concise, actionable, and focused on entry opportunities.");
            
            // Use portfolio overview analysis as a fallback since getAiResponse is private
            String aiResponse = service.AiAdviceService.getPortfolioOverviewAnalysis(cryptoList);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                return aiResponse;
            } else {
                return "Market analysis temporarily unavailable. Consider waiting for clearer market signals.";
            }
            
        } catch (Exception e) {
            LoggerUtil.error(SettingsPanel.class, "Error getting market overview analysis: " + e.getMessage(), e);
            return "Market conditions are mixed. Exercise caution and consider dollar-cost averaging strategies.";
        }
    }
    
    /**
     * Get detailed AI analysis for a specific cryptocurrency
     */
    private String getDetailedCryptoAnalysis(model.CryptoData crypto) {
        try {
            LoggerUtil.info(SettingsPanel.class, "Getting detailed AI analysis for " + crypto.symbol);
            
            // Create a detailed analysis prompt for this specific crypto
            StringBuilder cryptoPrompt = new StringBuilder();
            cryptoPrompt.append("Provide detailed trading analysis for ").append(crypto.name).append(" (").append(crypto.symbol).append(").\n\n");
            
            cryptoPrompt.append("CURRENT DATA:\n");
            cryptoPrompt.append("• Current Price: $").append(String.format("%.6f", crypto.currentPrice)).append("\n");
            cryptoPrompt.append("• Entry Target: $").append(String.format("%.6f", crypto.expectedEntry)).append("\n");
            cryptoPrompt.append("• 3-Month Target: $").append(String.format("%.6f", crypto.targetPrice3Month)).append("\n");
            cryptoPrompt.append("• Holdings: ").append(String.format("%.6f", crypto.holdings)).append("\n");
            
            if (crypto.avgBuyPrice > 0) {
                double pnlPercent = ((crypto.currentPrice - crypto.avgBuyPrice) / crypto.avgBuyPrice) * 100;
                cryptoPrompt.append("• Average Buy Price: $").append(String.format("%.6f", crypto.avgBuyPrice)).append("\n");
                cryptoPrompt.append("• Current P&L: ").append(String.format("%.1f%%", pnlPercent)).append("\n");
            }
            
            double entryOpportunity = crypto.getEntryOpportunity() * 100;
            double upside = getUpsidePotential(crypto) * 100;
            cryptoPrompt.append("• Entry Opportunity: ").append(String.format("%.1f%%", entryOpportunity)).append("\n");
            cryptoPrompt.append("• Upside Potential: ").append(String.format("%.1f%%", upside)).append("\n\n");
            
            cryptoPrompt.append("ANALYSIS REQUESTED:\n");
            cryptoPrompt.append("1. Entry Strategy: Should I buy now or wait?\n");
            cryptoPrompt.append("2. Technical Analysis: Key support/resistance levels\n");
            cryptoPrompt.append("3. Risk Assessment: What are the main risks?\n");
            cryptoPrompt.append("4. Exit Strategy: When should I take profits?\n");
            cryptoPrompt.append("5. Market Context: How does this fit current market trends?\n\n");
            
            cryptoPrompt.append("Provide specific, actionable advice in 3-4 concise bullet points.");
            
            // Use getDetailedAnalysis method which is public
            String aiResponse = service.AiAdviceService.getDetailedAnalysis(crypto, false);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                return aiResponse;
            } else {
                return "Technical analysis pending for " + crypto.symbol + ". Monitor price action and volume.";
            }
            
        } catch (Exception e) {
            LoggerUtil.debug(SettingsPanel.class, "Error getting detailed analysis for " + crypto.symbol + ": " + e.getMessage());
            return "Analysis unavailable for " + crypto.symbol + " at this time.";
        }
    }
}
