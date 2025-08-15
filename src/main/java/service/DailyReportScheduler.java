package service;

import model.CryptoData;
import data.PortfolioDataManager;
import ui.panel.PortfolioScreenshotBuilder;
import util.LoggerUtil;

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
import javax.swing.SwingUtilities;

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
        
        // Check if email service is available
        if (!EmailService.isAvailable()) {
            LoggerUtil.warning(DailyReportScheduler.class, "Email service not available. Daily reports will not be sent.");
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
        
        // If called from EDT (like a button click), run in background thread
        if (SwingUtilities.isEventDispatchThread()) {
            // Run in background thread to avoid blocking EDT
            new Thread(() -> {
                try {
                    sendDailyReport();
                } catch (Exception e) {
                    LoggerUtil.error(DailyReportScheduler.class, "Error in manual report thread: " + e.getMessage(), e);
                }
            }, "ManualReportThread").start();
        } else {
            // Not on EDT, safe to run directly
            sendDailyReport();
        }
    }
    
    /**
     * Send a manual portfolio overview email (for testing)
     */
    public static void sendManualPortfolioOverviewEmail() {
        LoggerUtil.info(DailyReportScheduler.class, "Sending manual portfolio overview email");
        
        // If called from EDT (like a button click), run in background thread
        if (SwingUtilities.isEventDispatchThread()) {
            // Run in background thread to avoid blocking EDT
            new Thread(() -> {
                try {
                    if (dataManager == null) {
                        LoggerUtil.error(DailyReportScheduler.class, "Portfolio data manager not available");
                        return;
                    }
                    
                    List<CryptoData> cryptoList = dataManager.getCryptoList();
                    if (cryptoList == null || cryptoList.isEmpty()) {
                        LoggerUtil.warning(DailyReportScheduler.class, "No cryptocurrency data available for portfolio overview email");
                        return;
                    }
                    
                    sendPortfolioOverviewEmail(cryptoList);
                } catch (Exception e) {
                    LoggerUtil.error(DailyReportScheduler.class, "Error in manual portfolio overview email thread: " + e.getMessage(), e);
                }
            }, "ManualPortfolioOverviewThread").start();
        } else {
            // Not on EDT, safe to run directly
            if (dataManager == null) {
                LoggerUtil.error(DailyReportScheduler.class, "Portfolio data manager not available");
                return;
            }
            
            List<CryptoData> cryptoList = dataManager.getCryptoList();
            if (cryptoList == null || cryptoList.isEmpty()) {
                LoggerUtil.warning(DailyReportScheduler.class, "No cryptocurrency data available for portfolio overview email");
                return;
            }
            
            sendPortfolioOverviewEmail(cryptoList);
        }
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
            
            // Check if email service is available
            if (!EmailService.isAvailable()) {
                LoggerUtil.error(DailyReportScheduler.class, "Email service not available. Cannot send daily report.");
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
            
            // Build dedicated portfolio screenshot (no UI dependency)
            File screenshotFile = capturePortfolioScreenshot();
            
            // Send email report
            boolean emailSent = EmailService.sendDailyReport(cryptoList, screenshotFile);
            
            if (emailSent) {
                LoggerUtil.info(DailyReportScheduler.class, "Daily report sent successfully");
                
                // Send second email for Portfolio Overview
                sendPortfolioOverviewEmail(cryptoList);
                
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
     * Send Portfolio Overview email as a second email
     */
    private static void sendPortfolioOverviewEmail(List<CryptoData> cryptoList) {
        try {
            LoggerUtil.info(DailyReportScheduler.class, "Sending Portfolio Overview email");
            
            // Check if email service is still available
            if (!EmailService.isAvailable()) {
                LoggerUtil.error(DailyReportScheduler.class, "Email service not available for Portfolio Overview email");
                return;
            }
            
            // Build Portfolio Overview screenshot
            File overviewScreenshotFile = capturePortfolioOverviewScreenshot(cryptoList);
            
            // Send Portfolio Overview email
            boolean overviewEmailSent = EmailService.sendPortfolioOverviewReport(cryptoList, overviewScreenshotFile);
            
            if (overviewEmailSent) {
                LoggerUtil.info(DailyReportScheduler.class, "Portfolio Overview email sent successfully");
            } else {
                LoggerUtil.error(DailyReportScheduler.class, "Failed to send Portfolio Overview email");
            }
            
        } catch (Exception e) {
            LoggerUtil.error(DailyReportScheduler.class, "Error sending Portfolio Overview email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Capture Portfolio Overview screenshot for the second email
     */
    private static File capturePortfolioOverviewScreenshot(List<CryptoData> cryptoList) {
        try {
            LoggerUtil.debug(DailyReportScheduler.class, "Building Portfolio Overview screenshot for email");
            
            if (dataManager == null) {
                LoggerUtil.error(DailyReportScheduler.class, "Portfolio data manager not available for overview screenshot");
                return null;
            }
            
            // Create a PortfolioOverviewPanel instance for screenshot
            ui.panel.PortfolioOverviewPanel overviewPanel = new ui.panel.PortfolioOverviewPanel(dataManager);
            
            // Build screenshot using PortfolioScreenshotBuilder with overview panel
            File screenshotFile = ui.panel.PortfolioScreenshotBuilder.buildPortfolioOverviewScreenshot(overviewPanel);
            
            if (screenshotFile != null) {
                LoggerUtil.info(DailyReportScheduler.class, "Portfolio Overview screenshot ready: " + screenshotFile.getName());
            } else {
                LoggerUtil.warning(DailyReportScheduler.class, "Failed to build Portfolio Overview screenshot");
            }
            
            return screenshotFile;
            
        } catch (Exception e) {
            LoggerUtil.error(DailyReportScheduler.class, "Error creating Portfolio Overview screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Capture portfolio screenshot for the report
     */
    private static File capturePortfolioScreenshot() {
        try {
            LoggerUtil.debug(DailyReportScheduler.class, "Building dedicated portfolio screenshot for daily report");
            
            // Check if data manager is available
            if (dataManager == null) {
                LoggerUtil.error(DailyReportScheduler.class, "Portfolio data manager not available for screenshot");
                return null;
            }
            
            // Get crypto data
            List<CryptoData> cryptoList = dataManager.getCryptoList();
            if (cryptoList == null || cryptoList.isEmpty()) {
                LoggerUtil.warning(DailyReportScheduler.class, "No cryptocurrency data available for screenshot");
                return null;
            }
            
            // Build dedicated portfolio screenshot - no fallbacks to screen capture
            File screenshotFile = PortfolioScreenshotBuilder.buildPortfolioScreenshot(cryptoList);
            
            if (screenshotFile != null) {
                LoggerUtil.info(DailyReportScheduler.class, "Dedicated portfolio screenshot ready: " + screenshotFile.getName());
            } else {
                LoggerUtil.warning(DailyReportScheduler.class, "Failed to build dedicated portfolio screenshot - continuing without image");
            }
            
            return screenshotFile;
            
        } catch (Exception e) {
            LoggerUtil.error(DailyReportScheduler.class, "Error creating portfolio screenshot: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Test the daily report system
     */
    public static boolean testDailyReportSystem() {
        LoggerUtil.info(DailyReportScheduler.class, "Testing daily report system");
        
        try {
            // Check email service availability
            if (!EmailService.isAvailable()) {
                LoggerUtil.error(DailyReportScheduler.class, "Test failed: Email service not available");
                return false;
            }
            
            // Test email sending
            boolean emailTest = EmailService.sendTestEmail();
            if (!emailTest) {
                LoggerUtil.error(DailyReportScheduler.class, "Test failed: Email test failed");
                return false;
            }
            
            // Test portfolio screenshot generation
            File testScreenshot = null;
            if (dataManager != null) {
                List<CryptoData> cryptoList = dataManager.getCryptoList();
                if (cryptoList != null && !cryptoList.isEmpty()) {
                    testScreenshot = PortfolioScreenshotBuilder.buildPortfolioScreenshot(cryptoList);
                }
            }
            
            if (testScreenshot != null) {
                LoggerUtil.info(DailyReportScheduler.class, "Test: Portfolio screenshot generated successfully");
            } else {
                LoggerUtil.warning(DailyReportScheduler.class, "Test warning: Portfolio screenshot generation failed - this is not critical for daily reports");
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
            EmailService.isAvailable(),
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
