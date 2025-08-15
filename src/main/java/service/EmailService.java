package service;

import model.CryptoData;
import service.AiAdviceService;
import util.LoggerUtil;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Date;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Email service for sending daily portfolio reports
 * Supports HTML emails with attachments (portfolio screenshots)
 */
public class EmailService {
    
    // Email configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final boolean USE_TLS = true;
    
    // Email templates
    private static final String EMAIL_SUBJECT_TEMPLATE = "📊 Daily Crypto Portfolio Report - %s";
    
    /**
     * Get email credentials from secure configuration
     */
    private static EmailConfig getEmailConfig() {
        LoggerUtil.info(EmailService.class, "Executing getEmailConfig()");
        // Obfuscated email configuration - credentials are encoded for security
        return new EmailConfig(
            decodeCredential("cXVvY3RoaWVuMDQ5QGdtYWlsLmNvbQ=="),
            decodeCredential("endsciBnbHpvIGNnY28gbGNwag=="),
            decodeCredential("cXVvY3RoaWVuMDQ5QGdtYWlsLmNvbQ==")
        );
    }
    
    /**
     * Decode base64 encoded credentials
     */
    private static String decodeCredential(String encoded) {
        LoggerUtil.info(EmailService.class, "Executing decodeCredential(encoded=" + encoded + ")");
        try {
            return new String(java.util.Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Error decoding credential: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Check if email service is available
     */
    public static boolean isAvailable() {
        LoggerUtil.info(EmailService.class, "Executing isAvailable()");
        try {
            EmailConfig config = getEmailConfig();
            return config.fromEmail != null && !config.fromEmail.isEmpty() &&
                   config.password != null && !config.password.isEmpty() &&
                   config.toEmail != null && !config.toEmail.isEmpty();
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Email service unavailable: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send daily portfolio report via email
     */
    public static boolean sendDailyReport(List<CryptoData> cryptoList, File screenshotFile) {
        LoggerUtil.info(EmailService.class, "Executing sendDailyReport(cryptoList.size=" + (cryptoList != null ? cryptoList.size() : 0) + ", screenshotFile=" + (screenshotFile != null ? screenshotFile.getName() : "null") + ")");
        if (!isAvailable()) {
            LoggerUtil.error(EmailService.class, "Email service not available");
            return false;
        }
        
        try {
            LoggerUtil.info(EmailService.class, "Sending daily portfolio report email");
            
            EmailConfig config = getEmailConfig();
            
            // Create email session
            Session session = createEmailSession(config);
            
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(config.toEmail));
            
            // Set subject with date
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            message.setSubject(String.format(EMAIL_SUBJECT_TEMPLATE, currentDate));
            message.setSentDate(new Date());
            
            // Create multipart content
            Multipart multipart = new MimeMultipart();
            
            // Add HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            String htmlContent = createHtmlReport(cryptoList);
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);
            
            // Add screenshot attachment if available
            if (screenshotFile != null && screenshotFile.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(screenshotFile);
                attachmentPart.setFileName("portfolio-" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".png");
                multipart.addBodyPart(attachmentPart);
                LoggerUtil.debug(EmailService.class, "Added screenshot attachment: " + screenshotFile.getName());
            }
            
            message.setContent(multipart);
            
            // Send email
            Transport.send(message);
            
            LoggerUtil.info(EmailService.class, "Daily portfolio report sent successfully to: " + config.toEmail);
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Failed to send daily report email: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send Portfolio Overview report via email
     */
    public static boolean sendPortfolioOverviewReport(List<CryptoData> cryptoList, File screenshotFile) {
        LoggerUtil.info(EmailService.class, "Executing sendPortfolioOverviewReport(cryptoList.size=" + (cryptoList != null ? cryptoList.size() : 0) + ", screenshotFile=" + (screenshotFile != null ? screenshotFile.getName() : "null") + ")");
        if (!isAvailable()) {
            LoggerUtil.error(EmailService.class, "Email service not available for Portfolio Overview");
            return false;
        }
        
        try {
            LoggerUtil.info(EmailService.class, "Sending Portfolio Overview email");
            
            EmailConfig config = getEmailConfig();
            
            // Create email session
            Session session = createEmailSession(config);
            
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(config.toEmail));
            
            // Set subject with date
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            message.setSubject("📊 Portfolio Overview Report - " + currentDate);
            message.setSentDate(new Date());
            
            // Create multipart content
            Multipart multipart = new MimeMultipart();
            
            // Add HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            String htmlContent = createPortfolioOverviewHtmlReport(cryptoList);
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);
            
            // Add screenshot attachment if available
            if (screenshotFile != null && screenshotFile.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(screenshotFile);
                attachmentPart.setFileName("portfolio-overview-" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".png");
                multipart.addBodyPart(attachmentPart);
                LoggerUtil.debug(EmailService.class, "Added Portfolio Overview screenshot attachment: " + screenshotFile.getName());
            }
            
            message.setContent(multipart);
            
            // Send email
            Transport.send(message);
            
            LoggerUtil.info(EmailService.class, "Portfolio Overview report sent successfully to: " + config.toEmail);
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Failed to send Portfolio Overview email: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send test email to verify configuration
     */
    public static boolean sendTestEmail() {
        LoggerUtil.info(EmailService.class, "Executing sendTestEmail()");
        if (!isAvailable()) {
            LoggerUtil.error(EmailService.class, "Email service not available for test");
            return false;
        }
        
        try {
            LoggerUtil.info(EmailService.class, "Sending test email");
            
            EmailConfig config = getEmailConfig();
            Session session = createEmailSession(config);
            
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(config.toEmail));
            message.setSubject("🧪 Crypto Portfolio - Test Email");
            message.setSentDate(new Date());
            
            String testContent = createTestEmailContent();
            message.setContent(testContent, "text/html; charset=utf-8");
            
            Transport.send(message);
            
            LoggerUtil.info(EmailService.class, "Test email sent successfully");
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Failed to send test email: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create email session with SMTP configuration
     */
    private static Session createEmailSession(EmailConfig config) {
        LoggerUtil.info(EmailService.class, "Executing createEmailSession(config.fromEmail=" + (config != null ? config.fromEmail : "null") + ")");
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        
        if (USE_TLS) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        }
        
        // Create authenticator
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.fromEmail, config.password);
            }
        };
        
        return Session.getInstance(props, auth);
    }
    
    /**
     * Create HTML email content for daily report
     */
    private static String createHtmlReport(List<CryptoData> cryptoList) {
        LoggerUtil.info(EmailService.class, "Executing createHtmlReport(cryptoList.size=" + (cryptoList != null ? cryptoList.size() : 0) + ")");
        StringBuilder html = new StringBuilder();
        
        // Calculate portfolio summary
        PortfolioSummary summary = calculatePortfolioSummary(cryptoList);
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Daily Crypto Portfolio Report</title>");
        html.append("<style>");
        html.append(getEmailStyles());
        html.append("</style>");
        html.append("</head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>📊 Daily Crypto Portfolio Report</h1>");
        html.append("<p class='date'>").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm"))).append("</p>");
        html.append("</div>");
        
        // Portfolio Summary
        html.append("<div class='summary'>");
        html.append("<h2>💰 Portfolio Summary</h2>");
        html.append("<div class='summary-grid'>");
        
        html.append("<div class='summary-item'>");
        html.append("<div class='summary-label'>Total Value</div>");
        html.append("<div class='summary-value'>$").append(String.format("%.2f", summary.totalValue)).append("</div>");
        html.append("</div>");
        
        html.append("<div class='summary-item'>");
        html.append("<div class='summary-label'>Total P&L</div>");
        String plClass = summary.totalProfitLoss >= 0 ? "positive" : "negative";
        html.append("<div class='summary-value ").append(plClass).append("'>");
        html.append(summary.totalProfitLoss >= 0 ? "+" : "").append(String.format("%.2f%%", summary.totalProfitLoss));
        html.append("</div>");
        html.append("</div>");
        
        html.append("<div class='summary-item'>");
        html.append("<div class='summary-label'>Total Coins</div>");
        html.append("<div class='summary-value'>").append(cryptoList.size()).append("</div>");
        html.append("</div>");
        
        html.append("<div class='summary-item'>");
        html.append("<div class='summary-label'>Best Performer</div>");
        html.append("<div class='summary-value'>").append(summary.bestPerformer).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        
        // Individual Crypto Performance
        html.append("<div class='crypto-list'>");
        html.append("<h2>🪙 Individual Performance & AI Analysis</h2>");
        
        for (CryptoData crypto : cryptoList) {
            double profitLoss = crypto.getProfitLossPercentage() * 100;
            String cryptoPlClass = profitLoss >= 0 ? "positive" : "negative";
            
            html.append("<div class='crypto-item'>");
            html.append("<div class='crypto-header'>");
            html.append("<h3>").append(crypto.name).append(" (").append(crypto.symbol).append(")</h3>");
            html.append("<div class='crypto-price'>$").append(String.format("%.4f", crypto.currentPrice)).append("</div>");
            html.append("</div>");
            
            html.append("<div class='crypto-details'>");
            html.append("<div class='detail-item'>");
            html.append("<span class='label'>Holdings:</span>");
            html.append("<span class='value'>").append(String.format("%.6f", crypto.holdings)).append(" ").append(crypto.symbol).append("</span>");
            html.append("</div>");
            
            html.append("<div class='detail-item'>");
            html.append("<span class='label'>Total Value:</span>");
            html.append("<span class='value'>$").append(String.format("%.2f", crypto.getTotalValue())).append("</span>");
            html.append("</div>");
            
            html.append("<div class='detail-item'>");
            html.append("<span class='label'>P&L:</span>");
            html.append("<span class='value ").append(cryptoPlClass).append("'>");
            html.append(profitLoss >= 0 ? "+" : "").append(String.format("%.2f%%", profitLoss));
            html.append("</span>");
            html.append("</div>");
            
            html.append("<div class='detail-item'>");
            html.append("<span class='label'>3M Target:</span>");
            html.append("<span class='value'>$").append(String.format("%.4f", crypto.targetPrice3Month)).append("</span>");
            html.append("</div>");
            
            // Add AI Analysis section for each crypto
            html.append("<div class='ai-analysis-section'>");
            html.append("<h4 style='color: #1976D2; margin: 15px 0 10px 0; font-size: 14px;'>🤖 AI Analysis & Recommendation</h4>");
            
            String aiAnalysis = getAiAnalysisForEmail(crypto);
            html.append("<div class='ai-analysis-content'>");
            html.append(formatAiAnalysisForEmail(aiAnalysis));
            html.append("</div>");
            html.append("</div>");
            
            html.append("</div>");
            html.append("</div>");
        }
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>🤖 This report was automatically generated by your Crypto Portfolio Tracker</p>");
        html.append("<p>📊 Detailed portfolio overview is attached as an image</p>");
        html.append("<p><small>Generated on ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</small></p>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Create HTML email content for Portfolio Overview report
     */
    private static String createPortfolioOverviewHtmlReport(List<CryptoData> cryptoList) {
        LoggerUtil.info(EmailService.class, "Executing createPortfolioOverviewHtmlReport(cryptoList.size=" + (cryptoList != null ? cryptoList.size() : 0) + ")");
        StringBuilder html = new StringBuilder();
        
        // Calculate portfolio summary
        PortfolioSummary summary = calculatePortfolioSummary(cryptoList);
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Portfolio Overview Report</title>");
        html.append("<style>");
        html.append(getEmailStyles());
        html.append("</style>");
        html.append("</head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🎯 AI Portfolio Overview Report</h1>");
        html.append("<p class='date'>").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm"))).append("</p>");
        html.append("</div>");

        // Enhanced AI Portfolio Overview Analysis Section
        html.append("<div class='ai-portfolio-main'>");
        html.append("<h2>🤖 AI Portfolio Analysis & Recommendations</h2>");
        
        // Portfolio Summary Stats Box
        html.append("<div class='portfolio-stats-box'>");
        html.append("<h3>📊 Portfolio Quick Stats</h3>");
        html.append("<div class='stats-grid'>");
        
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-label'>Total Value</div>");
        html.append("<div class='stat-value primary'>$").append(String.format("%.2f", summary.totalValue)).append("</div>");
        html.append("</div>");
        
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-label'>Assets</div>");
        html.append("<div class='stat-value'>").append(cryptoList.size()).append(" Holdings</div>");
        html.append("</div>");
        
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-label'>Performance</div>");
        String plClass = summary.totalProfitLoss >= 0 ? "positive" : "negative";
        html.append("<div class='stat-value ").append(plClass).append("'>");
        html.append(summary.totalProfitLoss >= 0 ? "+" : "").append(String.format("%.2f%%", summary.totalProfitLoss));
        html.append("</div>");
        html.append("</div>");
        
        html.append("<div class='stat-item'>");
        html.append("<div class='stat-label'>Top Performer</div>");
        html.append("<div class='stat-value'>").append(summary.bestPerformer).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");
        
        // AI Analysis Content
        String portfolioAnalysis = getPortfolioOverviewAnalysisForEmail(cryptoList);
        html.append("<div class='ai-analysis-main'>");
        html.append(formatPortfolioAnalysisForEmail(portfolioAnalysis));
        html.append("</div>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>📊 Portfolio Overview with visual allocation chart is attached as an image</p>");
        html.append("<p>🎯 This overview provides insights into your portfolio diversification and asset allocation</p>");
        html.append("<p>🤖 AI analysis provides strategic recommendations for portfolio optimization</p>");
        html.append("<p><small>Generated on ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</small></p>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Create test email content
     */
    private static String createTestEmailContent() {
        LoggerUtil.info(EmailService.class, "Executing createTestEmailContent()");
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append(getEmailStyles());
        html.append("</style>");
        html.append("</head><body>");
        
        html.append("<div class='header'>");
        html.append("<h1>🧪 Test Email - Crypto Portfolio Tracker</h1>");
        html.append("<p class='date'>").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm"))).append("</p>");
        html.append("</div>");
        
        html.append("<div class='summary'>");
        html.append("<h2>✅ Email Configuration Test</h2>");
        html.append("<p>If you're reading this email, your email configuration is working correctly!</p>");
        html.append("<p>Your daily portfolio reports will be sent to this email address every morning at 7:00 AM.</p>");
        html.append("</div>");
        
        html.append("<div class='footer'>");
        html.append("<p>🤖 This is a test message from your Crypto Portfolio Tracker</p>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Get CSS styles for email
     */
    private static String getEmailStyles() {
        LoggerUtil.info(EmailService.class, "Executing getEmailStyles()");
        return """
            body {
                font-family: 'Segoe UI', Arial, sans-serif;
                line-height: 1.6;
                color: #333;
                max-width: 800px;
                margin: 0 auto;
                padding: 20px;
                background-color: #f5f5f5;
            }
            .header {
                background: linear-gradient(135deg, #1976D2, #42A5F5);
                color: white;
                padding: 30px;
                border-radius: 10px;
                text-align: center;
                margin-bottom: 20px;
            }
            .header h1 {
                margin: 0;
                font-size: 28px;
                font-weight: bold;
            }
            .date {
                margin: 10px 0 0 0;
                opacity: 0.9;
                font-size: 16px;
            }
            .summary {
                background: white;
                padding: 25px;
                border-radius: 10px;
                margin-bottom: 20px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .summary h2 {
                margin-top: 0;
                color: #1976D2;
                border-bottom: 2px solid #E3F2FD;
                padding-bottom: 10px;
            }
            .summary-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                gap: 15px;
                margin-top: 20px;
            }
            .summary-item {
                text-align: center;
                padding: 15px;
                background: #F8F9FA;
                border-radius: 8px;
                border: 1px solid #E9ECEF;
            }
            .summary-label {
                font-size: 12px;
                color: #666;
                margin-bottom: 5px;
                text-transform: uppercase;
                font-weight: bold;
            }
            .summary-value {
                font-size: 18px;
                font-weight: bold;
                color: #333;
            }
            .summary-value.positive {
                color: #28A745;
            }
            .summary-value.negative {
                color: #DC3545;
            }
            .summary-value.primary {
                color: #1976D2;
            }
            .crypto-list {
                background: white;
                padding: 25px;
                border-radius: 10px;
                margin-bottom: 20px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .crypto-list h2 {
                margin-top: 0;
                color: #1976D2;
                border-bottom: 2px solid #E3F2FD;
                padding-bottom: 10px;
            }
            .crypto-item {
                border: 1px solid #E9ECEF;
                border-radius: 8px;
                padding: 15px;
                margin-bottom: 15px;
                background: #FAFAFA;
            }
            .crypto-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 10px;
            }
            .crypto-header h3 {
                margin: 0;
                color: #333;
                font-size: 16px;
            }
            .crypto-price {
                font-size: 16px;
                font-weight: bold;
                color: #1976D2;
            }
            .crypto-details {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 10px;
            }
            .detail-item {
                display: flex;
                justify-content: space-between;
                padding: 5px 0;
                border-bottom: 1px solid #EEE;
            }
            .label {
                color: #666;
                font-weight: 500;
            }
            .value {
                font-weight: bold;
            }
            .value.positive {
                color: #28A745;
            }
            .value.negative {
                color: #DC3545;
            }
            .ai-analysis-section {
                margin-top: 15px;
                padding: 12px;
                background-color: #F8F9FA;
                border-radius: 6px;
                border-left: 3px solid #1976D2;
            }
            .ai-analysis-section h4 {
                margin: 0 0 10px 0;
                color: #1976D2;
                font-size: 14px;
                font-weight: bold;
            }
            .ai-analysis-content {
                font-size: 12px;
                line-height: 1.5;
                color: #495057;
            }
            .ai-portfolio-main {
                background: white;
                padding: 30px;
                border-radius: 12px;
                margin-bottom: 20px;
                box-shadow: 0 4px 15px rgba(0,0,0,0.1);
                border-left: 5px solid #1976D2;
            }
            .ai-portfolio-main h2 {
                margin-top: 0;
                color: #1976D2;
                font-size: 24px;
                text-align: center;
                border-bottom: 3px solid #E3F2FD;
                padding-bottom: 15px;
                margin-bottom: 25px;
            }
            .portfolio-stats-box {
                background: linear-gradient(135deg, #E3F2FD, #F3E5F5);
                padding: 20px;
                border-radius: 10px;
                margin-bottom: 25px;
                border: 1px solid #BBDEFB;
            }
            .portfolio-stats-box h3 {
                margin: 0 0 15px 0;
                color: #1976D2;
                font-size: 18px;
                text-align: center;
            }
            .stats-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                gap: 15px;
            }
            .stat-item {
                background: white;
                padding: 15px;
                border-radius: 8px;
                text-align: center;
                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                border: 1px solid #E1F5FE;
            }
            .stat-label {
                font-size: 12px;
                color: #666;
                margin-bottom: 8px;
                text-transform: uppercase;
                font-weight: bold;
                letter-spacing: 0.5px;
            }
            .stat-value {
                font-size: 18px;
                font-weight: bold;
                color: #333;
            }
            .ai-analysis-main {
                background: #FAFAFA;
                padding: 25px;
                border-radius: 12px;
                border: 2px solid #E3F2FD;
                font-size: 15px;
                line-height: 1.7;
            }
            .footer {
                text-align: center;
                padding: 20px;
                color: #666;
                font-size: 14px;
                background: white;
                border-radius: 10px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .footer p {
                margin: 5px 0;
            }
            """;
    }
    
    /**
     * Calculate portfolio summary statistics
     */
    private static PortfolioSummary calculatePortfolioSummary(List<CryptoData> cryptoList) {
        LoggerUtil.info(EmailService.class, "Executing calculatePortfolioSummary(cryptoList.size=" + (cryptoList != null ? cryptoList.size() : 0) + ")");
        double totalValue = 0;
        double totalProfitLoss = 0;
        String bestPerformer = "N/A";
        double bestPerformance = Double.NEGATIVE_INFINITY;
        
        for (CryptoData crypto : cryptoList) {
            totalValue += crypto.getTotalValue();
            double profitLoss = crypto.getProfitLossPercentage() * 100;
            
            if (profitLoss > bestPerformance) {
                bestPerformance = profitLoss;
                bestPerformer = crypto.symbol;
            }
        }
        
        // Calculate weighted average P&L
        if (!cryptoList.isEmpty()) {
            double totalCost = 0;
            double weightedPL = 0;
            
            for (CryptoData crypto : cryptoList) {
                double cost = crypto.holdings * crypto.avgBuyPrice;
                totalCost += cost;
                weightedPL += cost * crypto.getProfitLossPercentage();
            }
            
            if (totalCost > 0) {
                totalProfitLoss = (weightedPL / totalCost) * 100;
            }
        }
        
        return new PortfolioSummary(totalValue, totalProfitLoss, bestPerformer);
    }
    
    /**
     * Get AI analysis for a specific cryptocurrency for email report
     */
    private static String getAiAnalysisForEmail(CryptoData crypto) {
        LoggerUtil.info(EmailService.class, "Executing getAiAnalysisForEmail(crypto.symbol=" + (crypto != null ? crypto.symbol : "null") + ")");
        try {
            LoggerUtil.debug(EmailService.class, "Getting AI analysis for " + crypto.symbol + " for email report");
            
            // Use the same AI service as the AiAnalysisDialog
            // Get cached analysis if available, otherwise generate new one
            String analysis = AiAdviceService.getDetailedAnalysis(crypto, false);
            
            if (analysis != null && !analysis.trim().isEmpty()) {
                LoggerUtil.debug(EmailService.class, "AI analysis retrieved for " + crypto.symbol);
                return analysis;
            } else {
                LoggerUtil.warning(EmailService.class, "No AI analysis available for " + crypto.symbol);
                return "AI analysis temporarily unavailable. Please check the application for the latest insights.";
            }
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Error getting AI analysis for " + crypto.symbol + ": " + e.getMessage(), e);
            return "Error retrieving AI analysis. Please check the application for manual analysis.";
        }
    }
    
    /**
     * Format AI analysis for email display (improved version)
     */
    private static String formatAiAnalysisForEmail(String analysis) {
        LoggerUtil.info(EmailService.class, "Executing formatAiAnalysisForEmail()");
        if (analysis == null || analysis.trim().isEmpty()) {
            return "<p style='color: #666; font-style: italic;'>No analysis available</p>";
        }
        
        try {
            // Check if it's an error message
            if (analysis.startsWith("AI analysis unavailable") || analysis.startsWith("Error")) {
                return "<p style='color: #FF6B35; font-style: italic; background-color: #FFF3E0; padding: 10px; border-radius: 4px;'>" +
                       "⚠️ " + analysis + "</p>";
            }
            
            // Clean up and process the analysis text
            String cleanedAnalysis = cleanupAnalysisText(analysis);
            
            StringBuilder html = new StringBuilder();
            html.append("<div style='background-color: #F8F9FA; padding: 15px; border-radius: 8px; border-left: 4px solid #1976D2; font-size: 13px; line-height: 1.4;'>");
            
            // Split analysis into sections
            String[] sections = cleanedAnalysis.split("\\*\\*\\d+\\.");
            
            for (String section : sections) {
                section = section.trim();
                if (section.isEmpty()) continue;
                
                // Extract section header and content
                String[] lines = section.split("\n", 2);
                if (lines.length > 0) {
                    String header = lines[0].trim();
                    String content = lines.length > 1 ? lines[1].trim() : "";
                    
                    // Format section header
                    if (header.contains("🔥") || header.contains("📊") || header.contains("💰") || 
                        header.contains("📈") || header.contains("🎯") || header.contains("🔮")) {
                        html.append("<div style='margin: 12px 0 8px 0;'>");
                        html.append("<strong style='color: #1976D2; font-size: 14px;'>").append(formatSectionHeader(header)).append("</strong>");
                        html.append("</div>");
                        
                        // Format section content
                        if (!content.isEmpty()) {
                            html.append(formatSectionContent(content));
                        }
                    } else if (!header.isEmpty()) {
                        // Handle content without clear section headers
                        html.append("<div style='margin: 8px 0;'>");
                        html.append(formatEmailContentLine(header));
                        if (!content.isEmpty()) {
                            html.append("<br/>").append(formatSectionContent(content));
                        }
                        html.append("</div>");
                    }
                }
            }
            
            html.append("</div>");
            return html.toString();
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Error formatting AI analysis for email: " + e.getMessage());
            return "<p style='color: #666; font-style: italic;'>Error formatting analysis</p>";
        }
    }
    
    /**
     * Clean up analysis text and remove duplicates
     */
    private static String cleanupAnalysisText(String analysis) {
        LoggerUtil.info(EmailService.class, "Executing cleanupAnalysisText()");
        // Remove duplicate summary lines
        String cleaned = analysis.replaceAll("(?m)^📊 Summary:\\s*$", "");
        
        // Remove excessive line breaks
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        
        // Remove duplicate crypto info lines
        cleaned = cleaned.replaceAll("(?m)^\\* Symbol:.*$", "");
        cleaned = cleaned.replaceAll("(?m)^\\* Current Price:.*$", "");
        cleaned = cleaned.replaceAll("(?m)^\\* Your Average Buy Price:.*$", "");
        cleaned = cleaned.replaceAll("(?m)^\\* Holdings:.*$", "");
        cleaned = cleaned.replaceAll("(?m)^\\* Current P&L:.*$", "");
        
        // Remove generated timestamp
        cleaned = cleaned.replaceAll("(?m)^Generated:.*$", "");
        
        return cleaned.trim();
    }
    
    /**
     * Format section header with proper emoji and styling
     */
    private static String formatSectionHeader(String header) {
        LoggerUtil.info(EmailService.class, "Executing formatSectionHeader(header=" + header + ")");
        // Clean up header text
        header = header.replaceAll("\\*\\*", "").trim();
        
        // Add proper spacing around emojis
        header = header.replaceAll("([🔥📊💰📈🎯🔮])\\s*", "$1 ");
        
        return header;
    }
    
    /**
     * Format section content with proper bullet points and styling
     */
    private static String formatSectionContent(String content) {
        LoggerUtil.info(EmailService.class, "Executing formatSectionContent()");
        StringBuilder html = new StringBuilder();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.startsWith("* **") && line.contains(":**")) {
                // Handle bullet points with bold labels
                String[] parts = line.split(":\\*\\*", 2);
                if (parts.length == 2) {
                    String label = parts[0].replace("* **", "").trim();
                    String value = parts[1].trim();
                    
                    html.append("<div style='margin: 6px 0; padding-left: 15px;'>");
                    html.append("• <strong style='color: #333;'>").append(label).append(":</strong> ");
                    html.append(formatEmailContentLine(value));
                    html.append("</div>");
                }
            } else if (line.startsWith("* ")) {
                // Regular bullet points
                html.append("<div style='margin: 6px 0; padding-left: 15px;'>");
                html.append("• ").append(formatEmailContentLine(line.substring(2).trim()));
                html.append("</div>");
            } else if (line.startsWith("**") && line.endsWith("**")) {
                // Bold headers within sections
                String boldText = line.replace("**", "");
                html.append("<div style='margin: 10px 0 6px 0;'>");
                html.append("<strong style='color: #1976D2;'>").append(boldText).append("</strong>");
                html.append("</div>");
            } else if (!line.startsWith("Important Considerations") && !line.startsWith("📊 Summary:")) {
                // Regular text
                html.append("<div style='margin: 4px 0;'>");
                html.append(formatEmailContentLine(line));
                html.append("</div>");
            }
        }
        
        return html.toString();
    }
    
    /**
     * Format content lines for email (improved version)
     */
    private static String formatEmailContentLine(String content) {
        LoggerUtil.info(EmailService.class, "Executing formatEmailContentLine()");
        if (content == null) return "";
        
        try {
            // Format buy/sell recommendations
            content = content.replaceAll("\\b(BUY|STRONG BUY|HOLD)\\b", 
                                       "<span style='color: #28A745; font-weight: bold;'>$1</span>");
            content = content.replaceAll("\\b(SELL|STRONG SELL|AVOID)\\b", 
                                       "<span style='color: #DC3545; font-weight: bold;'>$1</span>");
            content = content.replaceAll("\\b(NEUTRAL|WAIT|MONITOR)\\b", 
                                       "<span style='color: #FFC107; font-weight: bold;'>$1</span>");
            
            // Format percentages
            content = content.replaceAll("([+-]?\\d+\\.?\\d*)%", 
                                       "<strong style='color: #17A2B8;'>$1%</strong>");
            
            // Format currency (fix dollar sign escaping)
            content = content.replaceAll("\\$([\\d,]+\\.?\\d*)", 
                                       "<strong style='color: #28A745;'>\\$$1</strong>");
            
        } catch (Exception e) {
            LoggerUtil.warning(EmailService.class, "Error formatting email content line: " + e.getMessage());
        }
        
        return content;
    }
    
    /**
     * Get AI portfolio overview analysis for email report
     * @param cryptoList The list of cryptocurrency holdings
     * @return AI-generated portfolio overview analysis
     */
    private static String getPortfolioOverviewAnalysisForEmail(List<CryptoData> cryptoList) {
        LoggerUtil.info(EmailService.class, "Executing getPortfolioOverviewAnalysisForEmail(cryptoList.size=" + (cryptoList != null ? cryptoList.size() : 0) + ")");
        try {
            LoggerUtil.debug(EmailService.class, "Getting AI portfolio overview analysis for email report");
            
            // Use the new AiAdviceService method for portfolio overview analysis
            String analysis = AiAdviceService.getPortfolioOverviewAnalysis(cryptoList);
            
            if (analysis != null && !analysis.trim().isEmpty()) {
                LoggerUtil.debug(EmailService.class, "Portfolio overview analysis retrieved successfully");
                return analysis;
            } else {
                LoggerUtil.warning(EmailService.class, "No portfolio overview analysis available");
                return "Portfolio overview analysis temporarily unavailable. Please check the application for the latest insights.";
            }
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Error getting portfolio overview analysis: " + e.getMessage(), e);
            return "Error retrieving portfolio overview analysis. Please check the application for manual analysis.";
        }
    }
    
    /**
     * Format portfolio overview analysis for email display
     * @param analysis The portfolio analysis text
     * @return Formatted HTML for email display
     */
    private static String formatPortfolioAnalysisForEmail(String analysis) {
        LoggerUtil.info(EmailService.class, "Executing formatPortfolioAnalysisForEmail()");
        if (analysis == null || analysis.trim().isEmpty()) {
            return "<p style='color: #666; font-style: italic;'>No portfolio analysis available</p>";
        }
        
        try {
            // Check if it's an error message
            if (analysis.startsWith("Portfolio overview analysis unavailable") || analysis.startsWith("Error")) {
                return "<p style='color: #FF6B35; font-style: italic; background-color: #FFF3E0; padding: 10px; border-radius: 4px;'>" +
                       "⚠️ " + analysis + "</p>";
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<div style='background-color: #F8F9FA; padding: 20px; border-radius: 10px; border-left: 4px solid #1976D2; font-size: 14px; line-height: 1.6; margin-bottom: 20px;'>");
            
            // Split the analysis by sections with emoji headers
            String[] sections = analysis.split("(?=🎯|⚖️|📊|🔮|═════|📋)");
            
            for (String section : sections) {
                section = section.trim();
                if (section.isEmpty()) continue;
                
                // Format different types of sections
                if (section.startsWith("🎯") || section.startsWith("⚖️") || section.startsWith("📊") || section.startsWith("🔮")) {
                    // Main analysis sections
                    html.append(formatAnalysisSection(section));
                } else if (section.startsWith("📋")) {
                    // Quick stats section
                    html.append(formatQuickStatsSection(section));
                } else if (section.startsWith("═════")) {
                    // Header section
                    html.append(formatHeaderSection(section));
                } else if (!section.startsWith("⚠️") && !section.startsWith("⏰")) {
                    // Regular content
                    html.append("<div style='margin: 10px 0;'>");
                    html.append(formatPortfolioContentLine(section));
                    html.append("</div>");
                }
            }
            
            html.append("</div>");
            return html.toString();
            
        } catch (Exception e) {
            LoggerUtil.error(EmailService.class, "Error formatting portfolio analysis for email: " + e.getMessage());
            return "<p style='color: #666; font-style: italic;'>Error formatting portfolio analysis</p>";
        }
    }
    
    /**
     * Format analysis section with proper styling
     */
    private static String formatAnalysisSection(String section) {
        LoggerUtil.debug(EmailService.class, "Formatting analysis section");
        StringBuilder html = new StringBuilder();
        
        String[] lines = section.split("\n");
        if (lines.length > 0) {
            String header = lines[0].trim();
            
            html.append("<div style='margin: 20px 0 15px 0;'>");
            html.append("<h3 style='color: #1976D2; margin: 0 0 15px 0; font-size: 16px; font-weight: bold; border-bottom: 2px solid #E3F2FD; padding-bottom: 8px;'>");
            html.append(header);
            html.append("</h3>");
            
            // Process content lines
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                if (line.startsWith("•") || line.startsWith("-")) {
                    // Bullet points
                    html.append("<div style='margin: 8px 0; padding-left: 20px;'>");
                    html.append("• ").append(formatPortfolioContentLine(line.substring(1).trim()));
                    html.append("</div>");
                } else if (line.contains(":") && !line.startsWith("⚠️")) {
                    // Key-value pairs
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        html.append("<div style='margin: 8px 0;'>");
                        html.append("<strong style='color: #333;'>").append(parts[0].trim()).append(":</strong> ");
                        html.append(formatPortfolioContentLine(parts[1].trim()));
                        html.append("</div>");
                    }
                } else {
                    // Regular text
                    html.append("<div style='margin: 8px 0;'>");
                    html.append(formatPortfolioContentLine(line));
                    html.append("</div>");
                }
            }
            html.append("</div>");
        }
        
        return html.toString();
    }
    
    /**
     * Format quick stats section
     */
    private static String formatQuickStatsSection(String section) {
        LoggerUtil.debug(EmailService.class, "Formatting quick stats section");
        StringBuilder html = new StringBuilder();
        
        html.append("<div style='background-color: #E3F2FD; padding: 15px; border-radius: 8px; margin: 15px 0;'>");
        html.append("<h4 style='color: #1976D2; margin: 0 0 10px 0; font-size: 14px;'>Portfolio Quick Stats</h4>");
        
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("📋") || line.startsWith("─")) continue;
            
            if (line.startsWith("•")) {
                html.append("<div style='margin: 5px 0; font-size: 13px;'>");
                html.append(formatPortfolioContentLine(line));
                html.append("</div>");
            }
        }
        
        html.append("</div>");
        return html.toString();
    }
    
    /**
     * Format header section
     */
    private static String formatHeaderSection(String section) {
        LoggerUtil.debug(EmailService.class, "Formatting header section");
        String[] lines = section.split("\n");
        if (lines.length > 1) {
            String title = lines[1].trim();
            return "<div style='text-align: center; margin: 0 0 20px 0;'>" +
                   "<h2 style='color: #1976D2; margin: 0; font-size: 18px; font-weight: bold;'>" + title + "</h2>" +
                   "</div>";
        }
        return "";
    }
    
    /**
     * Format content lines for portfolio analysis email
     */
    private static String formatPortfolioContentLine(String content) {
        LoggerUtil.debug(EmailService.class, "Formatting portfolio content line");
        if (content == null) return "";
        
        try {
            // Format recommendations with colors
            content = content.replaceAll("\\b(INCREASE|BUY|ADD|ACCUMULATE)\\b", 
                                       "<span style='color: #28A745; font-weight: bold;'>$1</span>");
            content = content.replaceAll("\\b(DECREASE|SELL|REDUCE|TRIM)\\b", 
                                       "<span style='color: #DC3545; font-weight: bold;'>$1</span>");
            content = content.replaceAll("\\b(MAINTAIN|HOLD|STABLE|BALANCED)\\b", 
                                       "<span style='color: #17A2B8; font-weight: bold;'>$1</span>");
            
            // Format percentages and numbers
            content = content.replaceAll("([+-]?\\d+\\.?\\d*)%", 
                                       "<strong style='color: #17A2B8;'>$1%</strong>");
            
            // Format currency amounts
            content = content.replaceAll("\\$([\\d,]+\\.?\\d*)", 
                                       "<strong style='color: #28A745;'>\\$$1</strong>");
            
            // Format concentration warnings
            content = content.replaceAll("\\b(HIGH CONCENTRATION|CONCENTRATION RISK)\\b", 
                                       "<span style='color: #DC3545; font-weight: bold; background-color: #FFEBEE; padding: 2px 4px; border-radius: 3px;'>$1</span>");
            
            // Format positive indicators
            content = content.replaceAll("\\b(GOOD DIVERSIFICATION|WELL BALANCED|EXCELLENT)\\b", 
                                       "<span style='color: #28A745; font-weight: bold;'>$1</span>");
            
        } catch (Exception e) {
            LoggerUtil.warning(EmailService.class, "Error formatting portfolio content line: " + e.getMessage());
        }
        
        return content;
    }
    
    /**
     * Portfolio summary data class
     */
    private static class PortfolioSummary {
        final double totalValue;
        final double totalProfitLoss;
        final String bestPerformer;
        PortfolioSummary(double totalValue, double totalProfitLoss, String bestPerformer) {
            LoggerUtil.info(EmailService.class, "Constructing PortfolioSummary(totalValue=" + totalValue + ", totalProfitLoss=" + totalProfitLoss + ", bestPerformer=" + bestPerformer + ")");
            this.totalValue = totalValue;
            this.totalProfitLoss = totalProfitLoss;
            this.bestPerformer = bestPerformer;
        }
    }
    /**
     * Email configuration data class
     */
    private static class EmailConfig {
        final String fromEmail;
        final String password;
        final String toEmail;
        EmailConfig(String fromEmail, String password, String toEmail) {
            LoggerUtil.info(EmailService.class, "Constructing EmailConfig(fromEmail=" + fromEmail + ", toEmail=" + toEmail + ")");
            this.fromEmail = fromEmail;
            this.password = password;
            this.toEmail = toEmail;
        }
    }
}
