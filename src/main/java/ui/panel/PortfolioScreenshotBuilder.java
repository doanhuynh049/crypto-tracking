package ui.panel;

import model.CryptoData;
import util.LoggerUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds a dedicated screenshot of the portfolio for email reports
 * Creates a clean, optimized view without depending on the current UI state
 */
public class PortfolioScreenshotBuilder {
    
    // Screenshot dimensions optimized for email
    private static final int SCREENSHOT_WIDTH = 1200;
    private static final int SCREENSHOT_HEIGHT = 800;
    private static final String SCREENSHOT_DIR = "screenshots";
    private static final String SCREENSHOT_FORMAT = "PNG";
    
    // Color scheme matching the main application
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color SURFACE_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color DIVIDER_COLOR = new Color(224, 224, 224);
    
    // Fonts
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    
    /**
     * Build and save a portfolio screenshot for email
     * @param cryptoList List of cryptocurrency data
     * @return File containing the screenshot, or null if failed
     */
    public static File buildPortfolioScreenshot(List<CryptoData> cryptoList) {
        if (cryptoList == null || cryptoList.isEmpty()) {
            LoggerUtil.warning(PortfolioScreenshotBuilder.class, "No crypto data available for screenshot");
            return null;
        }
        
        try {
            LoggerUtil.info(PortfolioScreenshotBuilder.class, "Building portfolio screenshot for email with " + cryptoList.size() + " cryptocurrencies");
            
            // Create the portfolio image directly (no EDT complications)
            BufferedImage screenshot = createPortfolioImage(cryptoList);
            
            if (screenshot == null) {
                LoggerUtil.error(PortfolioScreenshotBuilder.class, "Failed to create screenshot image");
                return null;
            }
            
            // Generate filename and ensure directory exists
            String filename = generateScreenshotFilename();
            File screenshotFile = new File(SCREENSHOT_DIR, filename);
            
            // Ensure directory exists
            File parentDir = screenshotFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Created screenshots directory: " + dirCreated);
            }
            
            // Save the screenshot
            boolean saved = ImageIO.write(screenshot, SCREENSHOT_FORMAT, screenshotFile);
            
            if (saved && screenshotFile.exists()) {
                LoggerUtil.info(PortfolioScreenshotBuilder.class, "Portfolio screenshot saved successfully: " + screenshotFile.getAbsolutePath());
                LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Screenshot file size: " + screenshotFile.length() + " bytes");
                return screenshotFile;
            } else {
                LoggerUtil.error(PortfolioScreenshotBuilder.class, "Failed to save portfolio screenshot to: " + screenshotFile.getAbsolutePath());
                return null;
            }
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error building portfolio screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create the portfolio image (must be called on EDT)
     */
    private static BufferedImage createPortfolioImage(List<CryptoData> cryptoList) {
        try {
            LoggerUtil.info(PortfolioScreenshotBuilder.class, "Creating portfolio image on EDT with " + cryptoList.size() + " cryptos");
            
            // Instead of complex Swing components, let's create a simple image directly
            BufferedImage image = new BufferedImage(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Configure high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Fill background
            g2d.setColor(BACKGROUND_COLOR);
            g2d.fillRect(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Filled background with color: " + BACKGROUND_COLOR);
            
            // Draw header
            drawHeader(g2d, cryptoList);
            
            // Draw portfolio statistics
            drawStatistics(g2d, cryptoList);
            
            // Draw crypto table
            drawCryptoTable(g2d, cryptoList);
            
            // Draw footer
            drawFooter(g2d);
            
            g2d.dispose();
            
            LoggerUtil.info(PortfolioScreenshotBuilder.class, "Successfully created portfolio image using direct drawing");
            return image;
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error creating portfolio image: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Draw header section directly to Graphics2D
     */
    private static void drawHeader(Graphics2D g2d, List<CryptoData> cryptoList) {
        try {
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Drawing header section");
            
            // Header background
            g2d.setColor(PRIMARY_COLOR);
            g2d.fillRect(0, 0, SCREENSHOT_WIDTH, 100);
            
            // Title
            g2d.setColor(Color.WHITE);
            g2d.setFont(TITLE_FONT);
            g2d.drawString("💰 Crypto Portfolio Overview", 30, 45);
            
            // Date and count
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm"));
            String subtitle = dateStr + " • " + cryptoList.size() + " Cryptocurrencies";
            g2d.setFont(NORMAL_FONT);
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.drawString(subtitle, 30, 75);
            
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Header drawn successfully");
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error drawing header: " + e.getMessage(), e);
        }
    }
    
    /**
     * Draw statistics section directly to Graphics2D
     */
    private static void drawStatistics(Graphics2D g2d, List<CryptoData> cryptoList) {
        try {
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Drawing statistics section");
            
            // Calculate portfolio statistics
            double totalValue = 0;
            double totalCost = 0;
            double bestPerformance = Double.NEGATIVE_INFINITY;
            String bestPerformer = "N/A";
            
            for (CryptoData crypto : cryptoList) {
                double value = crypto.getTotalValue();
                double cost = crypto.holdings * crypto.avgBuyPrice;
                double performance = crypto.getProfitLossPercentage() * 100;
                
                totalValue += value;
                totalCost += cost;
                
                if (performance > bestPerformance) {
                    bestPerformance = performance;
                    bestPerformer = crypto.symbol;
                }
            }
            
            double totalProfitLoss = totalCost > 0 ? ((totalValue - totalCost) / totalCost) * 100 : 0;
            
            // Statistics panel background
            int statsY = 120;
            int statsHeight = 100;
            g2d.setColor(SURFACE_COLOR);
            g2d.fillRect(20, statsY, SCREENSHOT_WIDTH - 40, statsHeight);
            
            // Border
            g2d.setColor(DIVIDER_COLOR);
            g2d.drawRect(20, statsY, SCREENSHOT_WIDTH - 40, statsHeight);
            
            // Draw stat cards
            int cardWidth = (SCREENSHOT_WIDTH - 100) / 4;
            int cardX = 40;
            
            // Total Value
            drawStatCard(g2d, cardX, statsY + 20, cardWidth, "Total Value", 
                String.format("$%.2f", totalValue), SUCCESS_COLOR);
            
            cardX += cardWidth + 15;
            
            // Total P&L
            Color plColor = totalProfitLoss >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
            drawStatCard(g2d, cardX, statsY + 20, cardWidth, "Total P&L", 
                String.format("%+.2f%%", totalProfitLoss), plColor);
            
            cardX += cardWidth + 15;
            
            // Total Coins
            drawStatCard(g2d, cardX, statsY + 20, cardWidth, "Total Coins", 
                String.valueOf(cryptoList.size()), PRIMARY_COLOR);
            
            cardX += cardWidth + 15;
            
            // Best Performer
            drawStatCard(g2d, cardX, statsY + 20, cardWidth, "Best Performer", 
                bestPerformer, SUCCESS_COLOR);
            
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Statistics drawn successfully");
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error drawing statistics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Draw a single stat card
     */
    private static void drawStatCard(Graphics2D g2d, int x, int y, int width, String label, String value, Color valueColor) {
        // Label
        g2d.setColor(TEXT_SECONDARY);
        g2d.setFont(SMALL_FONT);
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        g2d.drawString(label, x + (width - labelWidth) / 2, y + 20);
        
        // Value
        g2d.setColor(valueColor);
        g2d.setFont(HEADER_FONT);
        fm = g2d.getFontMetrics();
        int valueWidth = fm.stringWidth(value);
        g2d.drawString(value, x + (width - valueWidth) / 2, y + 45);
    }
    
    /**
     * Draw crypto table directly to Graphics2D
     */
    private static void drawCryptoTable(Graphics2D g2d, List<CryptoData> cryptoList) {
        try {
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Drawing crypto table");
            
            int tableY = 240;
            int tableHeight = 450;
            int rowHeight = 30;
            
            // Table background
            g2d.setColor(SURFACE_COLOR);
            g2d.fillRect(20, tableY, SCREENSHOT_WIDTH - 40, tableHeight);
            
            // Table border
            g2d.setColor(DIVIDER_COLOR);
            g2d.drawRect(20, tableY, SCREENSHOT_WIDTH - 40, tableHeight);
            
            // Column headers - Added AI Advice column
            String[] headers = {"Crypto", "Price", "Holdings", "Value", "Avg Buy", "P&L %", "Target", "AI Advice"};
            int[] columnWidths = {160, 90, 100, 90, 90, 70, 90, 140};
            int[] columnX = new int[headers.length];
            
            columnX[0] = 30;
            for (int i = 1; i < columnX.length; i++) {
                columnX[i] = columnX[i-1] + columnWidths[i-1] + 10;
            }
            
            // Draw header background
            g2d.setColor(new Color(245, 245, 247));
            g2d.fillRect(20, tableY, SCREENSHOT_WIDTH - 40, rowHeight);
            
            // Draw header text
            g2d.setColor(TEXT_PRIMARY);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 13));
            for (int i = 0; i < headers.length; i++) {
                g2d.drawString(headers[i], columnX[i], tableY + 20);
            }
            
            // Draw header border
            g2d.setColor(DIVIDER_COLOR);
            g2d.drawLine(20, tableY + rowHeight, SCREENSHOT_WIDTH - 20, tableY + rowHeight);
            
            // Draw data rows
            g2d.setFont(NORMAL_FONT);
            int currentY = tableY + rowHeight;
            int maxRows = Math.min(cryptoList.size(), (tableHeight - rowHeight) / rowHeight);
            
            for (int i = 0; i < maxRows; i++) {
                if (i >= cryptoList.size()) break;
                
                CryptoData crypto = cryptoList.get(i);
                currentY += rowHeight;
                
                // Alternate row background
                if (i % 2 == 1) {
                    g2d.setColor(new Color(248, 249, 250));
                    g2d.fillRect(20, currentY - rowHeight, SCREENSHOT_WIDTH - 40, rowHeight);
                }
                
                // Draw row data
                g2d.setColor(TEXT_PRIMARY);
                
                // Cryptocurrency name
                String cryptoName = crypto.name + " (" + crypto.symbol + ")";
                if (cryptoName.length() > 25) {
                    cryptoName = cryptoName.substring(0, 22) + "...";
                }
                g2d.drawString(cryptoName, columnX[0], currentY - 8);
                
                // Price
                g2d.drawString(String.format("$%.4f", crypto.currentPrice), columnX[1], currentY - 8);
                
                // Holdings
                g2d.drawString(String.format("%.6f", crypto.holdings), columnX[2], currentY - 8);
                
                // Total Value
                g2d.drawString(String.format("$%.2f", crypto.getTotalValue()), columnX[3], currentY - 8);
                
                // Avg Buy Price
                g2d.drawString(String.format("$%.4f", crypto.avgBuyPrice), columnX[4], currentY - 8);
                
                // P&L % (with color)
                double profitLoss = crypto.getProfitLossPercentage() * 100;
                String plText = String.format("%+.2f%%", profitLoss);
                g2d.setColor(profitLoss >= 0 ? SUCCESS_COLOR : DANGER_COLOR);
                g2d.drawString(plText, columnX[5], currentY - 8);
                g2d.setColor(TEXT_PRIMARY);
                
                // 3M Target
                g2d.drawString(String.format("$%.4f", crypto.targetPrice3Month), columnX[6], currentY - 8);
                
                // AI Advice (with status icons and color coding)
                String aiAdvice = crypto.getAiAdviceWithStatus();
                if (aiAdvice != null) {
                    // Color code based on AI status
                    if (aiAdvice.startsWith("🤖")) {
                        g2d.setColor(new Color(46, 125, 50)); // AI success - dark green
                    } else if (aiAdvice.startsWith("📊")) {
                        g2d.setColor(new Color(25, 118, 210)); // Rule-based - blue
                    } else if (aiAdvice.startsWith("❌")) {
                        g2d.setColor(DANGER_COLOR); // Error - red
                    } else if (aiAdvice.startsWith("🔄")) {
                        g2d.setColor(new Color(255, 152, 0)); // Loading - orange
                    } else {
                        g2d.setColor(TEXT_PRIMARY); // Default
                    }
                    
                    // Truncate if too long for column
                    if (aiAdvice.length() > 18) {
                        aiAdvice = aiAdvice.substring(0, 15) + "...";
                    }
                    g2d.drawString(aiAdvice, columnX[7], currentY - 8);
                }
                g2d.setColor(TEXT_PRIMARY); // Reset color
                
                // Row separator
                g2d.setColor(DIVIDER_COLOR);
                g2d.drawLine(20, currentY, SCREENSHOT_WIDTH - 20, currentY);
            }
            
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Crypto table drawn successfully with " + maxRows + " rows");
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error drawing crypto table: " + e.getMessage(), e);
        }
    }
    
    /**
     * Draw footer section directly to Graphics2D
     */
    private static void drawFooter(Graphics2D g2d) {
        try {
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Drawing footer section");
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("'Generated on' yyyy-MM-dd 'at' HH:mm:ss"));
            String footerText = "🤖 " + timestamp + " by Crypto Portfolio Manager";
            
            g2d.setColor(TEXT_SECONDARY);
            g2d.setFont(SMALL_FONT);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(footerText);
            int x = (SCREENSHOT_WIDTH - textWidth) / 2;
            int y = SCREENSHOT_HEIGHT - 20;
            
            g2d.drawString(footerText, x, y);
            
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Footer drawn successfully");
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error drawing footer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert panel to BufferedImage
     */
    private static BufferedImage createScreenshotFromPanel(JPanel panel) {
        if (panel == null) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Cannot create screenshot from null panel");
            return null;
        }
        
        try {
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Creating BufferedImage from portfolio panel");
            
            // Create a temporary frame to properly initialize the components
            JFrame tempFrame = new JFrame();
            tempFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            tempFrame.setUndecorated(true);
            tempFrame.setSize(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
            tempFrame.add(panel);
            
            // Set panel bounds and properties
            panel.setBounds(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
            panel.setSize(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
            panel.setPreferredSize(new Dimension(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT));
            
            // Pack and validate the frame to initialize all components
            tempFrame.pack();
            tempFrame.setSize(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
            tempFrame.validate();
            tempFrame.repaint();
            
            // Force layout of all child components
            panel.doLayout();
            panel.validate();
            panel.repaint();
            
            // Give components time to initialize properly
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Create image with proper color model
            BufferedImage image = new BufferedImage(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Configure high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            
            // Fill background first
            g2d.setColor(BACKGROUND_COLOR);
            g2d.fillRect(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
            
            // Paint the panel and all its components
            panel.paintAll(g2d);
            
            g2d.dispose();
            
            // Clean up the temporary frame
            tempFrame.remove(panel);
            tempFrame.dispose();
            
            LoggerUtil.debug(PortfolioScreenshotBuilder.class, "Successfully created BufferedImage of size: " + 
                SCREENSHOT_WIDTH + "x" + SCREENSHOT_HEIGHT);
            
            return image;
            
        } catch (Exception e) {
            LoggerUtil.error(PortfolioScreenshotBuilder.class, "Error creating screenshot from panel: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate filename for screenshot
     */
    private static String generateScreenshotFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "portfolio_email_" + timestamp + "." + SCREENSHOT_FORMAT.toLowerCase();
    }
    
    /**
     * Custom cell renderer for profit/loss column
     */
    private static class ProfitLossCellRenderer extends JLabel implements TableCellRenderer {
        
        public ProfitLossCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(NORMAL_FONT);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            setText(value.toString());
            
            // Color based on profit/loss
            String text = value.toString();
            if (text.startsWith("+")) {
                setForeground(SUCCESS_COLOR);
            } else if (text.startsWith("-")) {
                setForeground(DANGER_COLOR);
            } else {
                setForeground(TEXT_PRIMARY);
            }
            
            if (isSelected) {
                setBackground(new Color(220, 225, 230));
            } else {
                setBackground(SURFACE_COLOR);
            }
            
            return this;
        }
    }
}
