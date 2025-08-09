/**
 * Main launcher class for the Crypto Portfolio application.
 * Launches the enhanced Swing interface directly.
 */
public class CryptoApp {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Enhanced Crypto Portfolio Tracker...");
        
        // Set system properties for better UI rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("swing.plaf.metal.controlFont", "Segoe UI-14");
        System.setProperty("swing.plaf.metal.userFont", "Segoe UI-14");
        
        // Launch the enhanced Swing GUI
        CryptoPortfolioGUI.main(args);
    }
}
