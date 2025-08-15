package main;

import data.PortfolioDataManager;
import service.DailyReportScheduler;
import service.EmailService;
import ui.CleanupablePanel;
import ui.panel.PortfolioContentPanel;
import ui.panel.PortfolioOverviewPanel;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Crypto Application with navigation panel and content area
 * Features: Left navigation panel with portfolio sections, right content panel
 */
public class CryptoMainApp extends JFrame {
    
    // UI Components
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel currentContentPanel;
    private List<NavigationItem> navigationItems;
    private NavigationItem selectedItem;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);      // Blue 700
    private static final Color PRIMARY_DARK = new Color(13, 71, 161);        // Blue 900
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);     // White
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);  // Light Gray
    private static final Color NAV_COLOR = new Color(236, 239, 241);         // Light Blue Gray
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);         // Dark Gray
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);    // Medium Gray
    private static final Color ACCENT_COLOR = new Color(76, 175, 80);        // Green 500
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);       // Green 500
    private static final Color DANGER_COLOR = new Color(244, 67, 54);        // Red 500
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);     // Light Gray
    
    // Typography
    private static final Font NAV_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font NAV_FONT_SELECTED = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    
    // System tray support
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private boolean isMinimizedToTray = false;
    
    public CryptoMainApp() {
        LoggerUtil.logAppStart("CryptoMainApp");
        LoggerUtil.info(CryptoMainApp.class, "Initializing Crypto Portfolio Manager");
        
        try {
            initializeNavigationItems();
            setupUI();
            selectNavigationItem(navigationItems.get(0)); // Select first item by default
            
            // Initialize system tray if supported
            initializeSystemTray();
            
            initializeDailyReports();
            LoggerUtil.info(CryptoMainApp.class, "Application initialization completed successfully");
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to initialize application", e);
        }
    }
    
    /**
     * Initialize daily report scheduler
     */
    private void initializeDailyReports() {
        try {
            SwingUtilities.invokeLater(() -> {
                LoggerUtil.info(CryptoMainApp.class, "Initializing daily report scheduler");
                try {
                    PortfolioDataManager dataManager = portfolioContent.getDataManager();
                    DailyReportScheduler.startDailyReports(dataManager, this);
                    LoggerUtil.info(CryptoMainApp.class, "Daily report scheduler started");
                } catch (Exception e) {
                    LoggerUtil.error(CryptoMainApp.class, "Failed to start daily report scheduler", e);
                }
            });
            LoggerUtil.info(CryptoMainApp.class, "Daily report scheduler initialization completed");
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to initialize daily report scheduler", e);
        }
    }
    
    private void initializeNavigationItems() {
        LoggerUtil.info(CryptoMainApp.class, "Initializing navigation items");
        navigationItems = new ArrayList<>();
        navigationItems.add(new NavigationItem("📊", "Portfolio Overview", "Portfolio allocation and AI-powered rebalancing"));
        navigationItems.add(new NavigationItem("💰", "My Portfolio", "View and manage your crypto portfolio"));
        navigationItems.add(new NavigationItem("📈", "Market Overview", "Real-time market data and trends"));
        navigationItems.add(new NavigationItem("📈", "Trading View", "Advanced trading charts and analysis"));
        navigationItems.add(new NavigationItem("🎯", "Watchlist", "Track your favorite cryptocurrencies"));
        navigationItems.add(new NavigationItem("📰", "News & Updates", "Latest crypto news and market updates"));
        navigationItems.add(new NavigationItem("⚙️", "Settings", "Application preferences and configuration"));
    }
    
    private void setupUI() {
        LoggerUtil.info(CryptoMainApp.class, "Setting up user interface");
        setTitle("🚀 Crypto Portfolio Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Add window listener for proper shutdown and minimize to tray
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SystemTray.isSupported() && isMinimizedToTray) {
                    // Hide to system tray instead of closing
                    setVisible(false);
                    LoggerUtil.info(CryptoMainApp.class, "Application minimized to system tray");
                } else {
                    // Normal shutdown
                    shutdownApplication();
                }
            }
            
            @Override
            public void windowIconified(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    // Minimize to system tray
                    minimizeToTray();
                }
            }
        });
        
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Create main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BACKGROUND_COLOR);
        
        // Create left navigation panel
        leftPanel = createNavigationPanel();
        mainContainer.add(leftPanel, BorderLayout.WEST);
        
        // Create right content panel
        rightPanel = createContentPanel();
        mainContainer.add(rightPanel, BorderLayout.CENTER);
        
        add(mainContainer, BorderLayout.CENTER);
        
        // Set window properties
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }
    
    private JPanel createNavigationPanel() {
        LoggerUtil.info(CryptoMainApp.class, "Creating navigation panel");
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BorderLayout());
        navPanel.setBackground(NAV_COLOR);
        navPanel.setPreferredSize(new Dimension(250, 0));
        navPanel.setBorder(new EmptyBorder(0, 0, 0, 1));
        
        // Create header
        JPanel headerPanel = createNavigationHeader();
        navPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create navigation items
        JPanel itemsPanel = createNavigationItems();
        navPanel.add(itemsPanel, BorderLayout.CENTER);
        
        // Create footer
        JPanel footerPanel = createNavigationFooter();
        navPanel.add(footerPanel, BorderLayout.SOUTH);
        
        return navPanel;
    }
    
    private JPanel createNavigationHeader() {
        LoggerUtil.debug(CryptoMainApp.class, "Creating navigation header");
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(NAV_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // App title
        JLabel titleLabel = new JLabel("CryptoManager");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Portfolio & Trading Platform");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(subtitleLabel);
        
        return headerPanel;
    }
    
    private JPanel createNavigationItems() {
        LoggerUtil.debug(CryptoMainApp.class, "Creating navigation item buttons");
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(NAV_COLOR);
        itemsPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        for (NavigationItem item : navigationItems) {
            JPanel navButton = createNavigationButton(item);
            itemsPanel.add(navButton);
            itemsPanel.add(Box.createVerticalStrut(2));
        }
        
        return itemsPanel;
    }
    
    private JPanel createNavigationButton(NavigationItem item) {
        LoggerUtil.debug(CryptoMainApp.class, "Creating navigation button for: " + item.title);
        JPanel button = new JPanel(new BorderLayout());
        button.setBackground(NAV_COLOR);
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Icon and text container
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setBackground(NAV_COLOR);
        contentPanel.setOpaque(false);
        
        // Icon
        JLabel iconLabel = new JLabel(item.icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 12));
        
        // Text
        JLabel textLabel = new JLabel(item.title);
        textLabel.setFont(NAV_FONT);
        textLabel.setForeground(TEXT_PRIMARY);
        
        contentPanel.add(iconLabel);
        contentPanel.add(textLabel);
        button.add(contentPanel, BorderLayout.CENTER);
        
        // Store references for styling
        item.buttonPanel = button;
        item.textLabel = textLabel;
        
        // Add hover and click effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (selectedItem != item) {
                    button.setBackground(new Color(220, 225, 230));
                    contentPanel.setBackground(new Color(220, 225, 230));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedItem != item) {
                    button.setBackground(NAV_COLOR);
                    contentPanel.setBackground(NAV_COLOR);
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                selectNavigationItem(item);
            }
        });
        
        return button;
    }
    
    private JPanel createNavigationFooter() {
        LoggerUtil.debug(CryptoMainApp.class, "Creating navigation footer");
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(NAV_COLOR);
        footerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        versionLabel.setForeground(TEXT_SECONDARY);
        
        footerPanel.add(versionLabel);
        
        return footerPanel;
    }
    
    private JPanel createContentPanel() {
        LoggerUtil.info(CryptoMainApp.class, "Creating content panel");
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(new LineBorder(DIVIDER_COLOR, 1, false));
        
        return contentPanel;
    }
    
    private void selectNavigationItem(NavigationItem item) {
        LoggerUtil.logUserAction("Selected navigation item: " + item.title);
        
        // Update previous selection
        if (selectedItem != null) {
            selectedItem.buttonPanel.setBackground(NAV_COLOR);
            selectedItem.textLabel.setFont(NAV_FONT);
            // Reset content panel background for previous item
            for (Component comp : selectedItem.buttonPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    comp.setBackground(NAV_COLOR);
                }
            }
        }
        
        // Update new selection
        selectedItem = item;
        item.buttonPanel.setBackground(PRIMARY_COLOR);
        item.textLabel.setFont(NAV_FONT_SELECTED);
        item.textLabel.setForeground(SURFACE_COLOR);
        
        // Update content panel background for selected item
        for (Component comp : item.buttonPanel.getComponents()) {
            if (comp instanceof JPanel) {
                comp.setBackground(PRIMARY_COLOR);
            }
        }
        
        // Load content for selected item
        loadContentForItem(item);
    }
    
    private void loadContentForItem(NavigationItem item) {
        LoggerUtil.debug(CryptoMainApp.class, "Loading content for: " + item.title);
        
        try {
            // Cleanup current panel if it implements CleanupablePanel
            if (currentContentPanel instanceof ui.CleanupablePanel) {
                LoggerUtil.info(CryptoMainApp.class, "Cleaning up current panel before switching");
                ((ui.CleanupablePanel) currentContentPanel).cleanup();
            }
            
            // Remove current content
            if (currentContentPanel != null) {
                rightPanel.remove(currentContentPanel);
            }
            
            // Create new content based on selected item
            switch (item.title) {
                case "Portfolio Overview":
                    currentContentPanel = createPortfolioOverviewContent();
                    break;
                case "My Portfolio":
                    currentContentPanel = createPortfolioContent();
                    break;
                case "Market Overview":
                    currentContentPanel = createMarketOverviewContent();
                    break;
                case "Trading View":
                    currentContentPanel = createTradingViewContent();
                    break;
                case "Watchlist":
                    currentContentPanel = createWatchlistContent();
                    break;
                case "News & Updates":
                    currentContentPanel = createNewsContent();
                    break;
                case "Settings":
                    currentContentPanel = createSettingsContent();
                    break;
                default:
                    currentContentPanel = createDefaultContent(item);
                    break;
            }
            
            rightPanel.add(currentContentPanel, BorderLayout.CENTER);
            rightPanel.revalidate();
            rightPanel.repaint();
            
            // Activate new panel if it implements CleanupablePanel
            if (currentContentPanel instanceof ui.CleanupablePanel) {
                LoggerUtil.info(CryptoMainApp.class, "Activating new panel after switching");
                ((ui.CleanupablePanel) currentContentPanel).activate();
            }
            
            LoggerUtil.debug(CryptoMainApp.class, "Content loaded successfully for: " + item.title);
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to load content for: " + item.title, e);
        }
    }
    
    private JPanel createPortfolioContent() {
        LoggerUtil.debug(CryptoMainApp.class, "Creating portfolio content panel");
        
        try {
            JPanel portfolioPanel = new JPanel(new BorderLayout());
            portfolioPanel.setBackground(BACKGROUND_COLOR);
            
            // Create header for portfolio section
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.setBackground(BACKGROUND_COLOR);
            headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));
            
            JLabel headerLabel = new JLabel("💰 My Portfolio");
            headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            headerLabel.setForeground(TEXT_PRIMARY);
            
            headerPanel.add(headerLabel);
            portfolioPanel.add(headerPanel, BorderLayout.NORTH);
            
            // Create container for the portfolio content
            JPanel contentContainer = new JPanel(new BorderLayout());
            contentContainer.setBackground(BACKGROUND_COLOR);
            contentContainer.setBorder(new EmptyBorder(0, 20, 20, 20));
            
            // Use the dedicated PortfolioContentPanel
            PortfolioContentPanel portfolioContent = new PortfolioContentPanel();
            contentContainer.add(portfolioContent, BorderLayout.CENTER);
            portfolioPanel.add(contentContainer, BorderLayout.CENTER);
            LoggerUtil.info(CryptoMainApp.class, "Portfolio content panel created successfully");
            return portfolioPanel;
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to create portfolio content", e);
            return createErrorContent("Failed to load portfolio");
        }
    }
    
    private JPanel createPortfolioOverviewContent() {
        LoggerUtil.debug(CryptoMainApp.class, "Creating portfolio overview content panel");
        
        try {
            JPanel overviewPanel = new JPanel(new BorderLayout());
            overviewPanel.setBackground(BACKGROUND_COLOR);
            
            // Create header for overview section
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.setBackground(BACKGROUND_COLOR);
            headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));
            
            JLabel headerLabel = new JLabel("📊 Portfolio Overview");
            headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            headerLabel.setForeground(TEXT_PRIMARY);
            
            headerPanel.add(headerLabel);
            overviewPanel.add(headerPanel, BorderLayout.NORTH);
            
            // Create container for the overview content
            JPanel contentContainer = new JPanel(new BorderLayout());
            contentContainer.setBackground(BACKGROUND_COLOR);
            contentContainer.setBorder(new EmptyBorder(0, 20, 20, 20));
            PortfolioDataManager dataManager = new PortfolioDataManager();
            PortfolioOverviewPanel overviewContent = new PortfolioOverviewPanel(dataManager);
            contentContainer.add(overviewContent, BorderLayout.CENTER);
            
            overviewPanel.add(contentContainer, BorderLayout.CENTER);
            
            LoggerUtil.info(CryptoMainApp.class, "Portfolio overview content panel created successfully");
            return overviewPanel;
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to create portfolio overview content", e);
            return createErrorContent("Failed to load portfolio overview");
        }
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        LoggerUtil.info(CryptoMainApp.class, "Creating modern button: " + text);
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
    
    private JPanel createMarketOverviewContent() {
        LoggerUtil.info(CryptoMainApp.class, "Creating market overview content");
        return createPlaceholderContent("📊 Market Overview", 
            "Real-time market data, trending cryptocurrencies, and market analysis");
    }
    
    private JPanel createTradingViewContent() {
        LoggerUtil.info(CryptoMainApp.class, "Creating trading view content");
        return createPlaceholderContent("📈 Trading View", 
            "Advanced charts, technical indicators, and trading tools");
    }
    
    private JPanel createWatchlistContent() {
        LoggerUtil.debug(CryptoMainApp.class, "Creating watchlist content panel");
        
        try {
            // Create the WatchlistPanel
            ui.panel.WatchlistPanel watchlistPanel = new ui.panel.WatchlistPanel();
            
            // Import portfolio data into the watchlist for unified view
            SwingUtilities.invokeLater(() -> {
                try {
                    // Create a portfolio data manager to get existing portfolio data
                    PortfolioDataManager portfolioDataManager = new PortfolioDataManager();
                    
                    // Import portfolio data into watchlist
                    boolean imported = watchlistPanel.getDataManager().importPortfolioData(portfolioDataManager.getCryptoList());
                    
                    if (imported) {
                        LoggerUtil.info(CryptoMainApp.class, "Successfully imported portfolio data into watchlist");
                    } else {
                        LoggerUtil.info(CryptoMainApp.class, "No portfolio data to import into watchlist");
                    }
                } catch (Exception e) {
                    LoggerUtil.error(CryptoMainApp.class, "Failed to import portfolio data into watchlist", e);
                }
            });
            
            return watchlistPanel;
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to create watchlist panel", e);
            // Fallback to placeholder if there's an error
            return createPlaceholderContent("🎯 Watchlist", 
                "Error loading watchlist panel: " + e.getMessage());
        }
    }
    
    private JPanel createNewsContent() {
        LoggerUtil.info(CryptoMainApp.class, "Creating news content");
        return createPlaceholderContent("📰 News & Updates", 
            "Latest cryptocurrency news, market updates, and insights");
    }
    
    private JPanel createSettingsContent() {
        LoggerUtil.info(CryptoMainApp.class, "Creating settings content");
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBackground(BACKGROUND_COLOR);
        settingsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel headerLabel = new JLabel("⚙️ Settings");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(TEXT_PRIMARY);
        headerPanel.add(headerLabel);
        
        // Create main content with tabs or sections
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
        
        settingsPanel.add(headerPanel, BorderLayout.NORTH);
        settingsPanel.add(contentPanel, BorderLayout.CENTER);
        
        return settingsPanel;
    }
    
    /**
     * Create combined email and daily reports section
     */
    private JPanel createEmailAndReportsSection() {
        LoggerUtil.info(CryptoMainApp.class, "Creating email and reports section");
        JPanel section = createSettingsSection("📧 Email Service & Daily Reports", 
            "Email service is pre-configured for automated daily portfolio reports (sent at 7:00 AM)");
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SURFACE_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Combined status display
        JLabel statusLabel = new JLabel();
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        updateCombinedStatus(statusLabel);
        
        // Buttons section
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        buttonPanel.setBackground(SURFACE_COLOR);
        
        JButton testEmailButton = createModernButton("📧 Send Test Email", PRIMARY_COLOR);
        JButton testReportButton = createModernButton("📊 Send Test Report", new Color(255, 152, 0));
        JButton testSystemButton = createModernButton("🔧 Test System", new Color(156, 39, 176));
        JButton refreshButton = createModernButton("🔄 Refresh Status", new Color(96, 125, 139));
        
        // Button actions
        testEmailButton.addActionListener(e -> {
            if (!EmailService.isAvailable()) {
                JOptionPane.showMessageDialog(this, 
                    "Email service is currently unavailable.", 
                    "Service Unavailable", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            boolean testResult = EmailService.sendTestEmail();
            if (testResult) {
                JOptionPane.showMessageDialog(this, 
                    "Test email sent successfully!", 
                    "Test Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to send test email. Please check the logs for details.", 
                    "Test Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        testReportButton.addActionListener(e -> {
            if (!EmailService.isAvailable()) {
                JOptionPane.showMessageDialog(this, 
                    "Email service is currently unavailable.", 
                    "Service Unavailable", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Send manual report
            DailyReportScheduler.sendManualReport();
            JOptionPane.showMessageDialog(this, 
                "Manual daily report sent! Check your email.", 
                "Report Sent", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        testSystemButton.addActionListener(e -> {
            boolean testResult = DailyReportScheduler.testDailyReportSystem();
            if (testResult) {
                JOptionPane.showMessageDialog(this, 
                    "Daily report system test completed successfully!", 
                    "System Test Passed", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Daily report system test failed. Check the logs for details.", 
                    "System Test Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        refreshButton.addActionListener(e -> updateCombinedStatus(statusLabel));
        
        // Add buttons to panel
        buttonPanel.add(testEmailButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(testReportButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(testSystemButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(refreshButton);
        
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(buttonPanel);
        
        section.add(contentPanel, BorderLayout.CENTER);
        return section;
    }
    
    /**
     * Update combined email and daily reports status display
     */
    private void updateCombinedStatus(JLabel statusLabel) {
        LoggerUtil.info(CryptoMainApp.class, "Updating combined status display");
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
        
        statusLabel.setText(statusText);
        
        if (isEmailAvailable && schedulerStatus.isScheduled) {
            statusLabel.setForeground(SUCCESS_COLOR);
        } else {
            statusLabel.setForeground(DANGER_COLOR);
        }
    }

    /**
     * Create application settings section
     */
    private JPanel createApplicationSettingsSection() {
        LoggerUtil.info(CryptoMainApp.class, "Creating application settings section");
        JPanel section = createSettingsSection("🔧 Application Settings", 
            "General application preferences and configuration");
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SURFACE_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Log level setting
        JPanel logLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logLevelPanel.setBackground(SURFACE_COLOR);
        
        JLabel logLabel = new JLabel("Log Level:");
        logLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JComboBox<String> logLevelCombo = new JComboBox<>(new String[]{"DEBUG", "INFO", "WARNING", "ERROR"});
        logLevelCombo.setSelectedItem("INFO");
        
        logLevelPanel.add(logLabel);
        logLevelPanel.add(Box.createHorizontalStrut(10));
        logLevelPanel.add(logLevelCombo);
        
        // Auto-refresh setting
        JPanel autoRefreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoRefreshPanel.setBackground(SURFACE_COLOR);
        
        JCheckBox autoRefreshCheck = new JCheckBox("Auto-refresh prices every 5 minutes");
        autoRefreshCheck.setBackground(SURFACE_COLOR);
        autoRefreshCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        autoRefreshPanel.add(autoRefreshCheck);
        
        // Background mode setting
        JPanel backgroundModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backgroundModePanel.setBackground(SURFACE_COLOR);
        
        JCheckBox backgroundModeCheck = new JCheckBox("Run in background (minimize to system tray)");
        backgroundModeCheck.setBackground(SURFACE_COLOR);
        backgroundModeCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backgroundModeCheck.setSelected(true); // Default to enabled for daily reports
        backgroundModeCheck.addActionListener(e -> {
            isMinimizedToTray = backgroundModeCheck.isSelected();
            if (isMinimizedToTray) {
                JOptionPane.showMessageDialog(this, 
                    "Background mode enabled. The application will minimize to system tray\n" +
                    "and continue sending daily reports even when the window is closed.", 
                    "Background Mode", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        backgroundModePanel.add(backgroundModeCheck);
        
        contentPanel.add(logLevelPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(autoRefreshPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(backgroundModePanel);
        
        section.add(contentPanel, BorderLayout.CENTER);
        return section;
    }
    
    /**
     * Create a settings section with title and description
     */
    private JPanel createSettingsSection(String title, String description) {
        LoggerUtil.info(CryptoMainApp.class, "Creating settings section: " + title);
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
     * Create field panel with label and text field
     */
    private JPanel createFieldPanel(String labelText, String placeholder) {
        LoggerUtil.info(CryptoMainApp.class, "Creating field panel for: " + labelText);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(SURFACE_COLOR);
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setPreferredSize(new Dimension(120, 25));
        
        JTextField field = new JTextField(20);
        field.setText(placeholder);
        field.setForeground(TEXT_SECONDARY);
        styleTextField(field);
        
        // Add placeholder behavior
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
            }
        });
        
        panel.add(label);
        panel.add(field);
        return panel;
    }
    
    /**
     * Style text field with modern appearance
     */
    private void styleTextField(JTextField field) {
        LoggerUtil.info(CryptoMainApp.class, "Styling text field");
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
            new LineBorder(DIVIDER_COLOR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(SURFACE_COLOR);
    }
    
    /**
     * Update daily report status display
     */
    private void updateDailyReportStatus(JLabel statusLabel) {
        LoggerUtil.info(CryptoMainApp.class, "Updating daily report status");
        DailyReportScheduler.SchedulerStatus status = DailyReportScheduler.getSchedulerStatus();
        
        String statusText = "<html><div style='font-family: Segoe UI; font-size: 12px;'>";
        statusText += "<b>Scheduler Status:</b> " + (status.isScheduled ? "✅ Active" : "❌ Inactive") + "<br/>";
        statusText += "<b>Email Service:</b> " + (status.emailConfigured ? "✅ Available" : "❌ Unavailable") + "<br/>";
        statusText += "<b>Data Available:</b> " + (status.dataManagerAvailable ? "✅ Yes" : "❌ No") + "<br/>";
        statusText += "<b>Next Report:</b> " + status.timeUntilNextReport + "<br/>";
        statusText += "</div></html>";
        
        statusLabel.setText(statusText);
        
        if (status.isScheduled && status.emailConfigured) {
            statusLabel.setForeground(SUCCESS_COLOR);
        } else {
            statusLabel.setForeground(DANGER_COLOR);
        }
    }
    
    /**
     * Public method to select the Portfolio tab (used by daily report scheduler)
     */
    public void selectPortfolioTab() {
        LoggerUtil.info(CryptoMainApp.class, "Selecting portfolio tab programmatically");
        SwingUtilities.invokeLater(() -> {
            try {
                // Find the Portfolio navigation item
                for (NavigationItem item : navigationItems) {
                    if ("My Portfolio".equals(item.title)) {
                        selectNavigationItem(item);
                        LoggerUtil.debug(CryptoMainApp.class, "Portfolio tab selected for screenshot");
                        break;
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error(CryptoMainApp.class, "Error selecting Portfolio tab: " + e.getMessage(), e);
            }
        });
    }
    
    private JPanel createDefaultContent(NavigationItem item) {
        LoggerUtil.info(CryptoMainApp.class, "Creating default content for: " + item.title);
        return createPlaceholderContent(item.icon + " " + item.title, item.description);
    }
    
    private JPanel createPlaceholderContent(String title, String description) {
        LoggerUtil.info(CryptoMainApp.class, "Creating placeholder content: " + title);
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(SURFACE_COLOR);
        centerPanel.setBorder(new EmptyBorder(60, 40, 60, 40));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel descLabel = new JLabel("<html><div style='text-align: center;'>" + description + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        descLabel.setForeground(TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel statusLabel = new JLabel("Coming Soon");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        statusLabel.setForeground(PRIMARY_COLOR);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(descLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(statusLabel);
        
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        
        return contentPanel;
    }
    
    private JPanel createErrorContent(String errorMessage) {
        LoggerUtil.warning(CryptoMainApp.class, "Creating error content panel: " + errorMessage);
        
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBackground(BACKGROUND_COLOR);
        errorPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(SURFACE_COLOR);
        centerPanel.setBorder(new EmptyBorder(60, 40, 60, 40));
        
        JLabel errorLabel = new JLabel("⚠️ Error");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        errorLabel.setForeground(DANGER_COLOR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel messageLabel = new JLabel("<html><div style='text-align: center;'>" + errorMessage + "</div></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messageLabel.setForeground(TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel retryLabel = new JLabel("Please try again or check the logs for more details");
        retryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        retryLabel.setForeground(TEXT_SECONDARY);
        retryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerPanel.add(errorLabel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(messageLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(retryLabel);
        
        errorPanel.add(centerPanel, BorderLayout.CENTER);
        
        return errorPanel;
    }
    
    /**
     * Initialize system tray support for background operation
     */
    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            LoggerUtil.warning(CryptoMainApp.class, "System tray not supported on this platform");
            return;
        }
        
        try {
            LoggerUtil.info(CryptoMainApp.class, "Initializing system tray");
            
            systemTray = SystemTray.getSystemTray();
            
            // Create tray icon image (use a simple colored square as fallback)
            Image trayImage = createTrayIconImage();
            
            // Create popup menu for tray icon
            PopupMenu popup = new PopupMenu();
            
            MenuItem openItem = new MenuItem("Open Portfolio");
            openItem.addActionListener(e -> restoreFromTray());
            
            MenuItem dailyReportItem = new MenuItem("Send Daily Report Now");
            dailyReportItem.addActionListener(e -> {
                DailyReportScheduler.sendManualReport();
                showTrayMessage("Daily Report", "Portfolio report sent successfully!");
            });
            
            MenuItem exitItem = new MenuItem("Exit Application");
            exitItem.addActionListener(e -> shutdownApplication());
            
            popup.add(openItem);
            popup.addSeparator();
            popup.add(dailyReportItem);
            popup.addSeparator();
            popup.add(exitItem);
            
            // Create tray icon
            trayIcon = new TrayIcon(trayImage, "Crypto Portfolio Manager", popup);
            trayIcon.setImageAutoSize(true);
            
            // Add double-click listener to restore window
            trayIcon.addActionListener(e -> restoreFromTray());
            
            LoggerUtil.info(CryptoMainApp.class, "System tray initialized successfully");
            
        } catch (Exception e) {
            LoggerUtil.error(CryptoMainApp.class, "Failed to initialize system tray", e);
        }
    }
    
    /**
     * Create a simple tray icon image
     */
    private Image createTrayIconImage() {
        LoggerUtil.info(CryptoMainApp.class, "Creating tray icon image");
        // Create a simple 16x16 colored square as the tray icon
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Fill with blue color (matching our theme)
        g2d.setColor(PRIMARY_COLOR);
        g2d.fillRect(0, 0, 16, 16);
        
        // Add a small dollar sign
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("$", 5, 12);
        
        g2d.dispose();
        return image;
    }
    
    /**
     * Minimize application to system tray
     */
    private void minimizeToTray() {
        LoggerUtil.info(CryptoMainApp.class, "Minimizing application to system tray");
        if (systemTray != null && trayIcon != null) {
            try {
                setVisible(false);
                systemTray.add(trayIcon);
                isMinimizedToTray = true;
                
                showTrayMessage("Crypto Portfolio Manager", 
                    "Application minimized to tray. Daily reports will continue running in background.");
                
                LoggerUtil.info(CryptoMainApp.class, "Application minimized to system tray");
            } catch (AWTException e) {
                LoggerUtil.error(CryptoMainApp.class, "Failed to minimize to tray", e);
            }
        }
    }
    
    /**
     * Restore application from system tray
     */
    private void restoreFromTray() {
        LoggerUtil.info(CryptoMainApp.class, "Restoring application from system tray");
        if (isMinimizedToTray) {
            setVisible(true);
            setState(JFrame.NORMAL);
            toFront();
            requestFocus();
            
            systemTray.remove(trayIcon);
            isMinimizedToTray = false;
            
            LoggerUtil.info(CryptoMainApp.class, "Application restored from system tray");
        }
    }
    
    /**
     * Show notification message in system tray
     */
    private void showTrayMessage(String title, String message) {
        LoggerUtil.info(CryptoMainApp.class, "Showing tray message: " + title);
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
    
    /**
     * Properly shutdown the application
     */
    private void shutdownApplication() {
        LoggerUtil.info(CryptoMainApp.class, "Shutting down application");
        
        // Cleanup current panel if it implements CleanupablePanel
        if (currentContentPanel instanceof CleanupablePanel) {
            LoggerUtil.info(CryptoMainApp.class, "Cleaning up current panel during shutdown");
            ((CleanupablePanel) currentContentPanel).cleanup();
        }
        
        // Stop daily report scheduler
        DailyReportScheduler.stopDailyReports();
        
        // Remove from system tray if present
        if (isMinimizedToTray && systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        
        LoggerUtil.logAppShutdown("CryptoMainApp");
        LoggerUtil.shutdown();
        System.exit(0);
    }
    
    // Navigation item data class
    private static class NavigationItem {
        String icon;
        String title;
        String description;
        JPanel buttonPanel;
        JLabel textLabel;
        
        public NavigationItem(String icon, String title, String description) {
            this.icon = icon;
            this.title = title;
            this.description = description;
        }
    }
    
    public static void main(String[] args) {
        LoggerUtil.info(CryptoMainApp.class, "Starting main method");
        LoggerUtil.logSystemEvent("Application startup initiated");
        
        // Enable modern rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        LoggerUtil.info("Starting Crypto Portfolio Manager with enhanced rendering");
        
        SwingUtilities.invokeLater(() -> {
            try {
                new CryptoMainApp();
            } catch (Exception e) {
                LoggerUtil.error("Failed to start application", e);
                System.exit(1);
            }
        });
    }
}
