package ui.panel;

import data.PortfolioDataManager;
import ui.CleanupablePanel;
import util.LoggerUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Portfolio content panel that can be embedded in the main application
 * This is the content part of CryptoPortfolioGUI without the JFrame window
 * 
 * This class now acts as a coordinator between UI building and data management.
 */
public class PortfolioContentPanel extends JPanel implements CleanupablePanel {
    
    private PortfolioDataManager dataManager;
    private PortfolioUIBuilder uiBuilder;
    
    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    
    public PortfolioContentPanel() {
        LoggerUtil.info(PortfolioContentPanel.class, "Initializing Portfolio Content Panel");
        
        try {
            // Initialize data manager first
            dataManager = new PortfolioDataManager();
            
            // Initialize UI builder with data manager reference
            uiBuilder = new PortfolioUIBuilder(dataManager);
            
            // Set UI builder reference in data manager for callbacks
            dataManager.setUIBuilder(uiBuilder);
            
            // Setup the UI
            setupUI();
            
            // Load initial data and prices
            LoggerUtil.info(PortfolioContentPanel.class, "Portfolio Content Panel initialized successfully");
        } catch (Exception e) {
            LoggerUtil.error(PortfolioContentPanel.class, "Failed to initialize Portfolio Content Panel", e);
            throw e;
        }
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
    
    @Override
    public void cleanup() {
        LoggerUtil.info(PortfolioContentPanel.class, "🛑 Cleaning up PortfolioContentPanel - stopping background operations");
        
        if (dataManager != null) {
            // Stop auto-refresh timer and cancel any ongoing operations
            dataManager.stopAutoRefresh();
            dataManager.cancelAllOperations();
        }
        
        LoggerUtil.info(PortfolioContentPanel.class, "✅ PortfolioContentPanel cleanup completed");
    }

    @Override
    public void activate() {
        LoggerUtil.info(PortfolioContentPanel.class, "🚀 Activating PortfolioContentPanel - starting background operations");
        
        if (dataManager != null) {
            // Start auto-refresh timer
            dataManager.startAutoRefresh();
            
            // Refresh data to ensure we have current information
            SwingUtilities.invokeLater(() -> {
                if (uiBuilder != null) {
                    uiBuilder.getStatusLabel().setText("✅ Panel activated");
                }
            });
        }
        
        LoggerUtil.info(PortfolioContentPanel.class, "✅ PortfolioContentPanel activation completed");
    }

    @Override
    public boolean hasActiveOperations() {
        if (dataManager == null) {
            return false;
        }
        
        return dataManager.hasActiveOperations();
    }
}
