import javax.swing.*;
import java.awt.*;

/**
 * Portfolio content panel that can be embedded in the main application
 * This is the content part of CryptoPortfolioGUI without the JFrame window
 * 
 * This class now acts as a coordinator between UI building and data management.
 */
public class PortfolioContentPanel extends JPanel {
    
    private PortfolioDataManager dataManager;
    private PortfolioUIBuilder uiBuilder;
    
    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    
    public PortfolioContentPanel() {
        // Initialize data manager first
        dataManager = new PortfolioDataManager();
        
        // Initialize UI builder with data manager reference
        uiBuilder = new PortfolioUIBuilder(dataManager);
        
        // Set UI builder reference in data manager for callbacks
        dataManager.setUIBuilder(uiBuilder);
        
        // Setup the UI
        setupUI();
        
        // Load initial data and prices
        dataManager.loadInitialPrices();
        
        // Fetch AI advice once during initialization
        dataManager.refreshAiAdvice();
        
        // Start auto refresh
        dataManager.startAutoRefresh();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        // Create status panel
        JPanel statusPanel = uiBuilder.createStatusPanel();
        add(statusPanel, BorderLayout.NORTH);
        
        // Create table panel
        JPanel tablePanel = uiBuilder.createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
        
        // Create control panel
        JPanel controlPanel = uiBuilder.createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
        
        // Setup click-to-deselect after all components are created
        SwingUtilities.invokeLater(() -> {
            uiBuilder.setupClickToDeselect();
        });
    }
    
    // Getter methods for accessing components if needed by parent containers
    public JTable getCryptoTable() {
        return uiBuilder.getCryptoTable();
    }
    
    public PortfolioDataManager getDataManager() {
        return dataManager;
    }
    
    public PortfolioUIBuilder getUIBuilder() {
        return uiBuilder;
    }
}
