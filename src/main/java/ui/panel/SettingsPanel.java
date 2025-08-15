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
        LoggerUtil.debug(SettingsPanel.class, "Creating main content panel");
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
        JButton testSystemButton = createModernButton("🔧 Test System", new Color(156, 39, 176));
        JButton refreshButton = createModernButton("🔄 Refresh Status", new Color(96, 125, 139));
        
        // Button actions
        setupEmailButtonActions(testEmailButton, testReportButton, testSystemButton, refreshButton);
        
        // Add buttons to panel
        buttonPanel.add(testEmailButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(testReportButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(testSystemButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(refreshButton);
        
        return buttonPanel;
    }
    
    /**
     * Setup action listeners for email buttons
     */
    private void setupEmailButtonActions(JButton testEmailButton, JButton testReportButton, 
                                       JButton testSystemButton, JButton refreshButton) {
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
        
        testSystemButton.addActionListener(e -> {
            boolean testResult = DailyReportScheduler.testDailyReportSystem();
            if (testResult) {
                showMessage("Daily report system test completed successfully!", "System Test Passed", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showMessage("Daily report system test failed. Check the logs for details.", "System Test Failed", JOptionPane.ERROR_MESSAGE);
            }
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
}
