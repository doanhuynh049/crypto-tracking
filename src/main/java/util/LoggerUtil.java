package util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Centralized logging utility for the Crypto Tracking application.
 * Provides logging to both console and file with automatic log directory creation.
 * 
 * Features:
 * - Automatic creation of log directory
 * - Dual output to console and file
 * - Formatted timestamps and log levels
 * - Append mode for persistent logging
 * - Thread-safe logging operations
 */
public class LoggerUtil {
    
    private static final String LOG_DIR = "log";
    private static final String LOG_FILE = "message.log";
    private static Logger logger;
    private static boolean initialized = false;
    
    // Initialize logger on class load
    static {
        initializeLogger();
    }
    
    /**
     * Initialize the logger with console and file handlers
     */
    private static synchronized void initializeLogger() {
        if (initialized) {
            return;
        }
        
        try {
            // Create log directory if it doesn't exist
            createLogDirectory();
            
            // Get logger instance for the application
            logger = Logger.getLogger("CryptoTrackingApp");
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false); // Disable default console handler
            
            // Create and configure console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new ConsoleLogFormatter());
            
            // Create and configure file handler
            String logFilePath = LOG_DIR + File.separator + LOG_FILE;
            FileHandler fileHandler = new FileHandler(logFilePath, true); // true for append mode
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new FileLogFormatter());
            
            // Add handlers to logger
            logger.addHandler(consoleHandler);
            logger.addHandler(fileHandler);
            
            initialized = true;
            
            // Log initialization success
            logger.info("Logger system initialized successfully. Log file: " + new File(logFilePath).getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
            // Create fallback console-only logger
            createFallbackLogger();
        }
    }
    
    /**
     * Create log directory if it doesn't exist
     */
    private static void createLogDirectory() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create log directory: " + LOG_DIR);
            }
        }
    }
    
    /**
     * Create a fallback console-only logger if file logging fails
     */
    private static void createFallbackLogger() {
        logger = Logger.getLogger("CryptoTrackingApp");
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new ConsoleLogFormatter());
        logger.addHandler(consoleHandler);
        
        initialized = true;
        logger.warning("Logger initialized in fallback mode (console only)");
    }
    
    /**
     * Log an INFO level message
     */
    public static void info(String message) {
        ensureInitialized();
        logger.info(message);
    }
    
    /**
     * Log an INFO level message with class context
     */
    public static void info(Class<?> clazz, String message) {
        ensureInitialized();
        logger.info("[" + clazz.getSimpleName() + "] " + message);
    }
    
    /**
     * Log a WARNING level message
     */
    public static void warning(String message) {
        ensureInitialized();
        logger.warning(message);
    }
    
    /**
     * Log a WARNING level message with class context
     */
    public static void warning(Class<?> clazz, String message) {
        ensureInitialized();
        logger.warning("[" + clazz.getSimpleName() + "] " + message);
    }
    
    /**
     * Log a SEVERE (ERROR) level message
     */
    public static void error(String message) {
        ensureInitialized();
        logger.severe(message);
    }
    
    /**
     * Log a SEVERE (ERROR) level message with exception
     */
    public static void error(String message, Throwable throwable) {
        ensureInitialized();
        logger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * Log a SEVERE (ERROR) level message with class context
     */
    public static void error(Class<?> clazz, String message) {
        ensureInitialized();
        logger.severe("[" + clazz.getSimpleName() + "] " + message);
    }
    
    /**
     * Log a SEVERE (ERROR) level message with class context and exception
     */
    public static void error(Class<?> clazz, String message, Throwable throwable) {
        ensureInitialized();
        logger.log(Level.SEVERE, "[" + clazz.getSimpleName() + "] " + message, throwable);
    }
    
    /**
     * Log a DEBUG level message (uses FINE level)
     */
    public static void debug(String message) {
        ensureInitialized();
        logger.fine(message);
    }
    
    /**
     * Log a DEBUG level message with class context
     */
    public static void debug(Class<?> clazz, String message) {
        ensureInitialized();
        logger.fine("[" + clazz.getSimpleName() + "] " + message);
    }
    
    /**
     * Log application startup
     */
    public static void logAppStart(String appName) {
        ensureInitialized();
        logger.info("========== " + appName + " Started ==========");
    }
    
    /**
     * Log application shutdown
     */
    public static void logAppShutdown(String appName) {
        ensureInitialized();
        logger.info("========== " + appName + " Shutdown ==========");
    }
    
    /**
     * Log user action
     */
    public static void logUserAction(String action) {
        ensureInitialized();
        logger.info("[USER ACTION] " + action);
    }
    
    /**
     * Log system event
     */
    public static void logSystemEvent(String event) {
        ensureInitialized();
        logger.info("[SYSTEM] " + event);
    }
    
    /**
     * Ensure logger is initialized
     */
    private static void ensureInitialized() {
        if (!initialized) {
            initializeLogger();
        }
    }
    
    /**
     * Shutdown the logger and close all handlers
     */
    public static void shutdown() {
        if (logger != null) {
            Handler[] handlers = logger.getHandlers();
            for (Handler handler : handlers) {
                handler.close();
            }
        }
    }
    
    /**
     * Custom formatter for console output (more compact)
     */
    private static class ConsoleLogFormatter extends Formatter {
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            
            // Timestamp (time only for console)
            LocalDateTime time = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(record.getMillis()), 
                java.time.ZoneId.systemDefault()
            );
            sb.append("[").append(time.format(timeFormatter)).append("]");
            
            // Log level with color coding (simplified)
            String level = record.getLevel().getName();
            sb.append(" [").append(level).append("]");
            
            // Message
            sb.append(" ").append(record.getMessage());
            
            // Exception if present (simplified for console)
            if (record.getThrown() != null) {
                sb.append(" - ").append(record.getThrown().getMessage());
            }
            
            sb.append("\n");
            return sb.toString();
        }
    }
    
    /**
     * Custom formatter for file output (more detailed)
     */
    private static class FileLogFormatter extends Formatter {
        private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            
            // Full timestamp
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(record.getMillis()), 
                java.time.ZoneId.systemDefault()
            );
            sb.append(dateTime.format(dateTimeFormatter));
            
            // Log level
            sb.append(" [").append(record.getLevel().getName()).append("]");
            
            // Thread info
            sb.append(" [Thread-").append(record.getThreadID()).append("]");
            
            // Logger name
            if (record.getLoggerName() != null) {
                sb.append(" [").append(record.getLoggerName()).append("]");
            }
            
            // Message
            sb.append(" ").append(record.getMessage());
            
            // Exception if present (full stack trace for file)
            if (record.getThrown() != null) {
                sb.append("\n");
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                sb.append(sw.toString());
            }
            
            sb.append("\n");
            return sb.toString();
        }
    }
}
