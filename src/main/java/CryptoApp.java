/**
 * Main launcher class for the Crypto Portfolio application.
 * Launches the main application with navigation panel and portfolio view.
 */
public class CryptoApp {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Crypto Portfolio Manager...");
        
        // Set system properties for better UI rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("swing.plaf.metal.controlFont", "Segoe UI-14");
        System.setProperty("swing.plaf.metal.userFont", "Segoe UI-14");
        
        // Launch the main application with navigation
        CryptoMainApp.main(args);
    }
}
