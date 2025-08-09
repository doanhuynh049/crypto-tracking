import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);     // Light Gray
    
    // Typography
    private static final Font NAV_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font NAV_FONT_SELECTED = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    
    public CryptoMainApp() {
        initializeNavigationItems();
        setupUI();
        selectNavigationItem(navigationItems.get(0)); // Select first item by default
    }
    
    private void initializeNavigationItems() {
        navigationItems = new ArrayList<>();
        navigationItems.add(new NavigationItem("💰", "My Portfolio", "View and manage your crypto portfolio"));
        navigationItems.add(new NavigationItem("📊", "Market Overview", "Real-time market data and trends"));
        navigationItems.add(new NavigationItem("📈", "Trading View", "Advanced trading charts and analysis"));
        navigationItems.add(new NavigationItem("🎯", "Watchlist", "Track your favorite cryptocurrencies"));
        navigationItems.add(new NavigationItem("📰", "News & Updates", "Latest crypto news and market updates"));
        navigationItems.add(new NavigationItem("⚙️", "Settings", "Application preferences and configuration"));
    }
    
    private void setupUI() {
        setTitle("🚀 Crypto Portfolio Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.setBorder(new LineBorder(DIVIDER_COLOR, 1, false));
        
        return contentPanel;
    }
    
    private void selectNavigationItem(NavigationItem item) {
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
        // Remove current content
        if (currentContentPanel != null) {
            rightPanel.remove(currentContentPanel);
        }
        
        // Create new content based on selected item
        switch (item.title) {
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
    }
    
    private JPanel createPortfolioContent() {
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
        
        return portfolioPanel;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
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
        return createPlaceholderContent("📊 Market Overview", 
            "Real-time market data, trending cryptocurrencies, and market analysis");
    }
    
    private JPanel createTradingViewContent() {
        return createPlaceholderContent("📈 Trading View", 
            "Advanced charts, technical indicators, and trading tools");
    }
    
    private JPanel createWatchlistContent() {
        return createPlaceholderContent("🎯 Watchlist", 
            "Track your favorite cryptocurrencies and set price alerts");
    }
    
    private JPanel createNewsContent() {
        return createPlaceholderContent("📰 News & Updates", 
            "Latest cryptocurrency news, market updates, and insights");
    }
    
    private JPanel createSettingsContent() {
        return createPlaceholderContent("⚙️ Settings", 
            "Application preferences, API settings, and user configuration");
    }
    
    private JPanel createDefaultContent(NavigationItem item) {
        return createPlaceholderContent(item.icon + " " + item.title, item.description);
    }
    
    private JPanel createPlaceholderContent(String title, String description) {
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
        // Enable modern rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        SwingUtilities.invokeLater(() -> new CryptoMainApp());
    }
}
