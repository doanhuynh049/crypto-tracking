import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.io.File;
import javax.swing.JFrame;

/**
 * Scheduler service for sending daily portfolio reports
 * Automatically sends email reports at 7:00 AM every day
 */
public class DailyReportScheduler {
    
    private static final LocalTime REPORT_TIME = LocalTime.of(7, 0); // 7:00 AM
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DailyReportScheduler");
        t.setDaemon(true);
        return t;
    });
    
    private static boolean isScheduled = false;
    private static PortfolioDataManager dataManager;
    private static JFrame mainFrame;
    
    /**
     * Start the daily report scheduler
     * @param portfolioDataManager The portfolio data manager
     * @param applicationFrame The main application frame for screenshots
     */
    public static void startDailyReports(PortfolioDataManager portfolioDataManager, JFrame applicationFrame) {
        if (isScheduled) {
            LoggerUtil.warning(DailyReportScheduler.class, "Daily reports already scheduled");
            return;
        }
        
        dataManager = portfolioDataManager;
        mainFrame = applicationFrame;
        
        // Check if email is configured
        if (!EmailService.isConfigured()) {
            LoggerUtil.warning(DailyReportScheduler.class, "Email service not configured. Daily reports will not be sent.");
            return;
        }
        
        LoggerUtil.info(DailyReportScheduler.class, "Starting daily report scheduler");
        
        // Calculate time until next 7 AM
        long initialDelay = calculateInitialDelay();
        
        // Schedule daily reports
        scheduler.scheduleAtFixedRate(
            DailyReportScheduler::sendDailyReport,
            initialDelay,
            TimeUnit.DAYS.toSeconds(1), // 24 hours in seconds
            TimeUnit.SECONDS
        );
        
        isScheduled = true;
        
        LoggerUtil.info(DailyReportScheduler.class, 
            "Daily reports scheduled. Next report in " + (initialDelay / 3600) + " hours and " + 
            ((initialDelay % 3600) / 60) + " minutes");
    }
    
    /**
     * Stop the daily report scheduler
     */
    public static void stopDailyReports() {
        if (!isScheduled) {
            LoggerUtil.debug(DailyReportScheduler.class, "Daily reports not currently scheduled");
            return;
        }
        
        LoggerUtil.info(DailyReportScheduler.class, "Stopping daily report scheduler");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        isScheduled = false;
        LoggerUtil.info(DailyReportScheduler.class, "Daily report scheduler stopped");
    }
    
    /**
     * Send a manual daily report (for testing)
     */
    public static void sendManualReport() {
        LoggerUtil.info(DailyReportScheduler.class, "Sending manual daily report");
        sendDailyReport();
    }
    
    /**
     * Check if daily reports are currently scheduled
     */
    public static boolean isScheduled() {
        return isScheduled;
    }
    
    /**
     * Get time until next scheduled report
     */
    public static String getTimeUntilNextReport() {
        if (!isScheduled) {
            return "Not scheduled";
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReport = getNextReportTime();
        
        long hours = ChronoUnit.HOURS.between(now, nextReport);
        long minutes = ChronoUnit.MINUTES.between(now, nextReport) % 60;
        
        if (hours > 0) {
            return hours + " hours and " + minutes + " minutes";
        } else {
            return minutes + " minutes";
        }
    }
    
    /**
     * Calculate initial delay in seconds until next 7 AM
     */
    private static long calculateInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReport = getNextReportTime();
        
        return ChronoUnit.SECONDS.between(now, nextReport);
    }
    
    /**
     * Get the next report time (next 7 AM)
     */
    private static LocalDateTime getNextReportTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today7AM = now.toLocalDate().atTime(REPORT_TIME);
        
        if (now.isBefore(today7AM)) {
            return today7AM; // Today at 7 AM
        } else {
            return today7AM.plusDays(1); // Tomorrow at 7 AM
        }
    }
    
    /**
     * Send the daily report
     */
    private static void sendDailyReport() {
        try {
            LoggerUtil.info(DailyReportScheduler.class, "Executing daily report generation");
            
            // Check if email is still configured
            if (!EmailService.isConfigured()) {
                LoggerUtil.error(DailyReportScheduler.class, "Email service not configured. Cannot send daily report.");
                return;
            }
            
            // Check if data manager is available
            if (dataManager == null) {
                LoggerUtil.error(DailyReportScheduler.class, "Portfolio data manager not available");
                return;
            }
            
            // Get crypto data
            List<CryptoData> cryptoList = dataManager.getCryptoList();
            if (cryptoList == null || cryptoList.isEmpty()) {
                LoggerUtil.warning(DailyReportScheduler.class, "No cryptocurrency data available for report");
                return;
            }
            
            LoggerUtil.info(DailyReportScheduler.class, "Generating daily report for " + cryptoList.size() + " cryptocurrencies");
            
            // Capture screenshot
            File screenshotFile = capturePortfolioScreenshot();
            
            // Send email report
            boolean emailSent = EmailService.sendDailyReport(cryptoList, screenshotFile);
            
            if (emailSent) {
                LoggerUtil.info(DailyReportScheduler.class, "Daily report sent successfully");
                
                // Clean up old screenshots
                ScreenshotService.cleanupOldScreenshots();
            } else {
                LoggerUtil.error(DailyReportScheduler.class, "Failed to send daily report");
            }
            
        } catch (Exception e) {
            LoggerUtil.error(DailyReportScheduler.class, "Error generating daily report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Capture portfolio screenshot for the report
     */
    private static File capturePortfolioScreenshot() {
        try {
            LoggerUtil.debug(DailyReportScheduler.class, "Capturing portfolio screenshot for daily report");
            
            File screenshotFile = null;
            
            // Try different screenshot methods
            if (mainFrame != null) {
                // Ensure Portfolio tab is selected before screenshot
                ensurePortfolioTabSelected();
                
                // Wait for UI to update
                Thread.sleep(2000);
                
                // First try to capture the main frame
                screenshotFile = ScreenshotService.capturePortfolioScreenshot(mainFrame);
            }
            
            if (screenshotFile == null) {
                // Fallback to desktop screenshot
                LoggerUtil.debug(DailyReportScheduler.class, "Main frame capture failed, trying desktop screenshot");
                screenshotFile = ScreenshotService.captureDesktopScreenshot();
            }
            
            if (screenshotFile == null) {
                LoggerUtil.warning(DailyReportScheduler.class, "No screenshot captured for daily report");
            } else {
                LoggerUtil.info(DailyReportScheduler.class, "Screenshot captured: " + screenshotFile.getName());
            }
            
            return screenshotFile;
            
        } catch (Exception e) {
            LoggerUtil.error(DailyReportScheduler.class, "Error capturing screenshot for daily report: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Ensure the Portfolio tab is selected in the main application
     */
    private static void ensurePortfolioTabSelected() {
        try {
            if (mainFrame instanceof CryptoMainApp) {
                LoggerUtil.debug(DailyReportScheduler.class, "Switching to Portfolio tab for screenshot");
                CryptoMainApp app = (CryptoMainApp) mainFrame;
                // Use reflection to access the selectPortfolioTab method
                app.selectPortfolioTab();
            }
        } catch (Exception e) {
            LoggerUtil.warning(DailyReportScheduler.class, "Could not ensure Portfolio tab selection: " + e.getMessage());
        }
    }
    
    /**
     * Test the daily report system
     */
    public static boolean testDailyReportSystem() {
        LoggerUtil.info(DailyReportScheduler.class, "Testing daily report system");
        
        try {
            // Check email configuration
            if (!EmailService.isConfigured()) {
                LoggerUtil.error(DailyReportScheduler.class, "Test failed: Email service not configured");
                return false;
            }
            
            // Test email sending
            boolean emailTest = EmailService.sendTestEmail();
            if (!emailTest) {
                LoggerUtil.error(DailyReportScheduler.class, "Test failed: Email test failed");
                return false;
            }
            
            // Test screenshot capture
            File testScreenshot = null;
            if (mainFrame != null) {
                testScreenshot = ScreenshotService.capturePortfolioScreenshot(mainFrame);
            }
            
            if (testScreenshot == null) {
                testScreenshot = ScreenshotService.captureDesktopScreenshot();
            }
            
            if (testScreenshot == null) {
                LoggerUtil.warning(DailyReportScheduler.class, "Test warning: Screenshot capture failed");
            } else {
                LoggerUtil.info(DailyReportScheduler.class, "Test: Screenshot captured successfully");
            }
            
            LoggerUtil.info(DailyReportScheduler.class, "Daily report system test completed successfully");
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(DailyReportScheduler.class, "Daily report system test failed: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get scheduler status information
     */
    public static SchedulerStatus getSchedulerStatus() {
        return new SchedulerStatus(
            isScheduled,
            EmailService.isConfigured(),
            dataManager != null,
            mainFrame != null,
            getTimeUntilNextReport()
        );
    }
    
    /**
     * Scheduler status data class
     */
    public static class SchedulerStatus {
        public final boolean isScheduled;
        public final boolean emailConfigured;
        public final boolean dataManagerAvailable;
        public final boolean mainFrameAvailable;
        public final String timeUntilNextReport;
        
        public SchedulerStatus(boolean isScheduled, boolean emailConfigured, 
                             boolean dataManagerAvailable, boolean mainFrameAvailable, 
                             String timeUntilNextReport) {
            this.isScheduled = isScheduled;
            this.emailConfigured = emailConfigured;
            this.dataManagerAvailable = dataManagerAvailable;
            this.mainFrameAvailable = mainFrameAvailable;
            this.timeUntilNextReport = timeUntilNextReport;
        }
        
        @Override
        public String toString() {
            return String.format("SchedulerStatus{scheduled=%s, email=%s, data=%s, frame=%s, next=%s}", 
                isScheduled, emailConfigured, dataManagerAvailable, mainFrameAvailable, timeUntilNextReport);
        }
    }
}
