package service;

import util.LoggerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for capturing screenshots of the portfolio UI
 * Creates high-quality images for email reports
 */
public class ScreenshotService {
    
    private static final String SCREENSHOT_DIR = "screenshots";
    private static final String SCREENSHOT_FORMAT = "PNG";
    
    // Screenshot configuration
    private static final int SCREENSHOT_WIDTH = 1200;
    private static final int SCREENSHOT_HEIGHT = 800;
    private static final int SCREENSHOT_DELAY = 1000; // 1 second delay to allow UI to render
    
    static {
        // Create screenshots directory if it doesn't exist
        createScreenshotDirectory();
    }
    
    /**
     * Capture screenshot of the main portfolio window
     * @param mainFrame The main application frame to capture
     * @return File containing the screenshot, or null if capture failed
     */
    public static File capturePortfolioScreenshot(JFrame mainFrame) {
        LoggerUtil.info(ScreenshotService.class, "Capturing portfolio screenshot");
        if (mainFrame == null) {
            LoggerUtil.error(ScreenshotService.class, "Cannot capture screenshot: main frame is null");
            return null;
        }
        
        try {
            LoggerUtil.info(ScreenshotService.class, "Capturing portfolio screenshot");
            
            // Ensure the frame is visible and properly rendered
            if (!mainFrame.isVisible()) {
                LoggerUtil.warning(ScreenshotService.class, "Main frame is not visible, making it visible for screenshot");
                mainFrame.setVisible(true);
                // Wait longer for window to become visible
                Thread.sleep(2000);
            }
            
            // Bring frame to front and ensure it's focused
            mainFrame.toFront();
            mainFrame.requestFocus();
            mainFrame.repaint();
            
            // Force a complete repaint of all components
            SwingUtilities.invokeAndWait(() -> {
                mainFrame.invalidate();
                mainFrame.validate();
                mainFrame.repaint();
            });
            
            // Wait for UI to stabilize and fully render
            Thread.sleep(SCREENSHOT_DELAY * 3); // Triple the normal delay for better rendering
            
            // Get frame dimensions
            Rectangle frameBounds = mainFrame.getBounds();
            LoggerUtil.debug(ScreenshotService.class, "Frame bounds: " + frameBounds);
            
            // Ensure frame has valid bounds
            if (frameBounds.width <= 0 || frameBounds.height <= 0) {
                LoggerUtil.error(ScreenshotService.class, "Frame has invalid bounds: " + frameBounds);
                return null;
            }
            
            // Create robot for screen capture
            Robot robot = new Robot();
            
            // Additional delay before capture to ensure everything is rendered
            Thread.sleep(1000);
            
            // Capture the frame area
            BufferedImage screenshot = robot.createScreenCapture(frameBounds);
            
            // Create optimized screenshot with fixed dimensions
            BufferedImage optimizedScreenshot = createOptimizedScreenshot(screenshot);
            
            // Generate filename with timestamp
            String filename = generateScreenshotFilename();
            File screenshotFile = new File(SCREENSHOT_DIR, filename);
            
            // Save screenshot
            boolean saved = ImageIO.write(optimizedScreenshot, SCREENSHOT_FORMAT, screenshotFile);
            
            if (saved) {
                LoggerUtil.info(ScreenshotService.class, "Screenshot saved: " + screenshotFile.getAbsolutePath());
                return screenshotFile;
            } else {
                LoggerUtil.error(ScreenshotService.class, "Failed to save screenshot to file");
                return null;
            }
            
        } catch (Exception e) {
            LoggerUtil.error(ScreenshotService.class, "Error capturing portfolio screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Capture screenshot of a specific component (like portfolio table)
     * @param component The component to capture
     * @return File containing the screenshot, or null if capture failed
     */
    public static File captureComponentScreenshot(Component component) {
        LoggerUtil.info(ScreenshotService.class, "Capturing component screenshot");
        if (component == null) {
            LoggerUtil.error(ScreenshotService.class, "Cannot capture component screenshot: component is null");
            return null;
        }
        
        try {
            LoggerUtil.info(ScreenshotService.class, "Capturing component screenshot");
            
            // Get component size
            Dimension size = component.getSize();
            
            if (size.width <= 0 || size.height <= 0) {
                LoggerUtil.error(ScreenshotService.class, "Component has invalid size: " + size);
                return null;
            }
            
            // Create buffered image
            BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Configure graphics for high quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Paint component to image
            component.printAll(g2d);
            g2d.dispose();
            
            // Create optimized version
            BufferedImage optimizedImage = createOptimizedScreenshot(image);
            
            // Save to file
            String filename = generateComponentScreenshotFilename();
            File screenshotFile = new File(SCREENSHOT_DIR, filename);
            
            boolean saved = ImageIO.write(optimizedImage, SCREENSHOT_FORMAT, screenshotFile);
            
            if (saved) {
                LoggerUtil.info(ScreenshotService.class, "Component screenshot saved: " + screenshotFile.getAbsolutePath());
                return screenshotFile;
            } else {
                LoggerUtil.error(ScreenshotService.class, "Failed to save component screenshot");
                return null;
            }
            
        } catch (Exception e) {
            LoggerUtil.error(ScreenshotService.class, "Error capturing component screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Capture a full desktop screenshot and crop to application area
     * This is a fallback method when direct frame capture doesn't work
     */
    public static File captureDesktopScreenshot() {
        LoggerUtil.info(ScreenshotService.class, "Capturing desktop screenshot");
        try {
            LoggerUtil.info(ScreenshotService.class, "Capturing desktop screenshot");
            
            // Get screen size
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(screenSize);
            
            // Create robot for screen capture
            Robot robot = new Robot();
            
            // Capture full screen
            BufferedImage fullScreenshot = robot.createScreenCapture(screenRect);
            
            // Create optimized version
            BufferedImage optimizedScreenshot = createOptimizedScreenshot(fullScreenshot);
            
            // Save to file
            String filename = generateDesktopScreenshotFilename();
            File screenshotFile = new File(SCREENSHOT_DIR, filename);
            
            boolean saved = ImageIO.write(optimizedScreenshot, SCREENSHOT_FORMAT, screenshotFile);
            
            if (saved) {
                LoggerUtil.info(ScreenshotService.class, "Desktop screenshot saved: " + screenshotFile.getAbsolutePath());
                return screenshotFile;
            } else {
                LoggerUtil.error(ScreenshotService.class, "Failed to save desktop screenshot");
                return null;
            }
            
        } catch (Exception e) {
            LoggerUtil.error(ScreenshotService.class, "Error capturing desktop screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create optimized screenshot with fixed dimensions and quality
     */
    private static BufferedImage createOptimizedScreenshot(BufferedImage originalImage) {
        LoggerUtil.info(ScreenshotService.class, "Creating optimized screenshot");
        if (originalImage == null) {
            return null;
        }
        
        // Calculate scaling to fit within desired dimensions while maintaining aspect ratio
        double scaleX = (double) SCREENSHOT_WIDTH / originalImage.getWidth();
        double scaleY = (double) SCREENSHOT_HEIGHT / originalImage.getHeight();
        double scale = Math.min(scaleX, scaleY);
        
        int newWidth = (int) (originalImage.getWidth() * scale);
        int newHeight = (int) (originalImage.getHeight() * scale);
        
        // Create new image with optimal size
        BufferedImage optimizedImage = new BufferedImage(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = optimizedImage.createGraphics();
        
        // Configure high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);
        
        // Center the scaled image
        int x = (SCREENSHOT_WIDTH - newWidth) / 2;
        int y = (SCREENSHOT_HEIGHT - newHeight) / 2;
        
        // Draw scaled image
        g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
        
        // Add timestamp watermark
        addTimestampWatermark(g2d);
        
        g2d.dispose();
        
        return optimizedImage;
    }
    
    /**
     * Add timestamp watermark to screenshot
     */
    private static void addTimestampWatermark(Graphics2D g2d) {
        LoggerUtil.info(ScreenshotService.class, "Adding timestamp watermark to screenshot");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // Configure text
        Font font = new Font("Segoe UI", Font.PLAIN, 12);
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics(font);
        
        // Position watermark at bottom right
        int textWidth = metrics.stringWidth(timestamp);
        int x = SCREENSHOT_WIDTH - textWidth - 15;
        int y = SCREENSHOT_HEIGHT - 15;
        
        // Draw semi-transparent background
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(x - 5, y - metrics.getHeight() + 5, textWidth + 10, metrics.getHeight());
        
        // Draw timestamp text
        g2d.setColor(Color.WHITE);
        g2d.drawString(timestamp, x, y);
    }
    
    /**
     * Generate filename for portfolio screenshot
     */
    private static String generateScreenshotFilename() {
        LoggerUtil.info(ScreenshotService.class, "Generating screenshot filename");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "portfolio_" + timestamp + "." + SCREENSHOT_FORMAT.toLowerCase();
    }
    
    /**
     * Generate filename for component screenshot
     */
    private static String generateComponentScreenshotFilename() {
        LoggerUtil.info(ScreenshotService.class, "Generating component screenshot filename");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "component_" + timestamp + "." + SCREENSHOT_FORMAT.toLowerCase();
    }
    
    /**
     * Generate filename for desktop screenshot
     */
    private static String generateDesktopScreenshotFilename() {
        LoggerUtil.info(ScreenshotService.class, "Generating desktop screenshot filename");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "desktop_" + timestamp + "." + SCREENSHOT_FORMAT.toLowerCase();
    }
    
    /**
     * Create screenshots directory if it doesn't exist
     */
    private static void createScreenshotDirectory() {
        LoggerUtil.info(ScreenshotService.class, "Creating screenshot directory if needed");
        try {
            File dir = new File(SCREENSHOT_DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    LoggerUtil.info(ScreenshotService.class, "Created screenshots directory: " + dir.getAbsolutePath());
                } else {
                    LoggerUtil.warning(ScreenshotService.class, "Failed to create screenshots directory");
                }
            }
        } catch (Exception e) {
            LoggerUtil.error(ScreenshotService.class, "Error creating screenshots directory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean up old screenshot files (keep only last 30 days)
     */
    public static void cleanupOldScreenshots() {
        LoggerUtil.info(ScreenshotService.class, "Cleaning up old screenshots");
        try {
            LoggerUtil.info(ScreenshotService.class, "Cleaning up old screenshots");
            
            File dir = new File(SCREENSHOT_DIR);
            if (!dir.exists()) {
                return;
            }
            
            File[] files = dir.listFiles((dir1, name) -> 
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
            
            if (files == null) {
                return;
            }
            
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
            int deletedCount = 0;
            
            for (File file : files) {
                if (file.lastModified() < thirtyDaysAgo) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }
            
            if (deletedCount > 0) {
                LoggerUtil.info(ScreenshotService.class, "Cleaned up " + deletedCount + " old screenshot files");
            }
            
        } catch (Exception e) {
            LoggerUtil.error(ScreenshotService.class, "Error cleaning up old screenshots: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the most recent screenshot file
     */
    public static File getMostRecentScreenshot() {
        LoggerUtil.info(ScreenshotService.class, "Getting most recent screenshot");
        try {
            LoggerUtil.info(ScreenshotService.class, "Getting most recent screenshot");
            
            File dir = new File(SCREENSHOT_DIR);
            if (!dir.exists()) {
                return null;
            }
            
            File[] files = dir.listFiles((dir1, name) -> 
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
            
            if (files == null || files.length == 0) {
                return null;
            }
            
            File mostRecent = files[0];
            for (File file : files) {
                if (file.lastModified() > mostRecent.lastModified()) {
                    mostRecent = file;
                }
            }
            
            return mostRecent;
            
        } catch (Exception e) {
            LoggerUtil.error(ScreenshotService.class, "Error finding most recent screenshot: " + e.getMessage(), e);
            return null;
        }
    }
}
