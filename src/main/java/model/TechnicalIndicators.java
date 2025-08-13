package model;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * Technical analysis indicators for cryptocurrency entry techniques
 * Contains various technical signals and entry point analysis
 */
public class TechnicalIndicators implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Price data points (last 50 periods for analysis)
    private List<PricePoint> priceHistory;
    
    // Technical indicator values
    private double rsi;
    private double macd;
    private double macdSignal;
    private double sma10;
    private double sma50;
    private double ema10;
    private double ema50;
    
    // Support and Resistance levels
    private double supportLevel;
    private double resistanceLevel;
    private double fibonacciSupport38;
    private double fibonacciSupport50;
    private double fibonacciSupport61;
    
    // Volume analysis
    private double averageVolume20;
    private double currentVolume;
    private boolean volumeConfirmation;
    
    // Trend analysis
    private TrendDirection trend;
    private double trendlineSupport;
    private double trendlineResistance;
    
    // Entry signals
    private List<EntrySignal> entrySignals;
    private EntryQuality overallEntryQuality;
    
    public TechnicalIndicators() {
        this.priceHistory = new ArrayList<>();
        this.entrySignals = new ArrayList<>();
        this.trend = TrendDirection.NEUTRAL;
        this.overallEntryQuality = EntryQuality.POOR;
    }
    
    /**
     * Individual price point with OHLCV data
     */
    public static class PricePoint implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private long timestamp;
        private double open;
        private double high;
        private double low;
        private double close;
        private double volume;
        
        public PricePoint(long timestamp, double open, double high, double low, double close, double volume) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public double getOpen() { return open; }
        public double getHigh() { return high; }
        public double getLow() { return low; }
        public double getClose() { return close; }
        public double getVolume() { return volume; }
        
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public void setOpen(double open) { this.open = open; }
        public void setHigh(double high) { this.high = high; }
        public void setLow(double low) { this.low = low; }
        public void setClose(double close) { this.close = close; }
        public void setVolume(double volume) { this.volume = volume; }
    }
    
    /**
     * Entry signal with specific technique and strength
     */
    public static class EntrySignal implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private EntryTechnique technique;
        private SignalStrength strength;
        private String description;
        private double targetPrice;
        private double stopLoss;
        private double confidence;
        
        public EntrySignal(EntryTechnique technique, SignalStrength strength, String description, 
                          double targetPrice, double stopLoss, double confidence) {
            this.technique = technique;
            this.strength = strength;
            this.description = description;
            this.targetPrice = targetPrice;
            this.stopLoss = stopLoss;
            this.confidence = confidence;
        }
        
        // Getters and setters
        public EntryTechnique getTechnique() { return technique; }
        public SignalStrength getStrength() { return strength; }
        public String getDescription() { return description; }
        public double getTargetPrice() { return targetPrice; }
        public double getStopLoss() { return stopLoss; }
        public double getConfidence() { return confidence; }
        
        public void setTechnique(EntryTechnique technique) { this.technique = technique; }
        public void setStrength(SignalStrength strength) { this.strength = strength; }
        public void setDescription(String description) { this.description = description; }
        public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }
        public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    // Enums for classification
    public enum TrendDirection {
        BULLISH, BEARISH, NEUTRAL
    }
    
    public enum EntryTechnique {
        SUPPORT_RESISTANCE,
        MOVING_AVERAGE_CROSSOVER,
        RSI_OVERSOLD,
        MACD_BULLISH_CROSSOVER,
        CANDLESTICK_PATTERN,
        VOLUME_BREAKOUT,
        FIBONACCI_RETRACEMENT,
        TRENDLINE_BOUNCE
    }
    
    public enum SignalStrength {
        VERY_STRONG,
        STRONG,
        MODERATE,
        WEAK,
        VERY_WEAK
    }
    
    public enum EntryQuality {
        EXCELLENT,
        GOOD,
        AVERAGE,
        POOR,
        VERY_POOR
    }
    
    // Getters and setters for all fields
    public List<PricePoint> getPriceHistory() { return priceHistory; }
    public void setPriceHistory(List<PricePoint> priceHistory) { this.priceHistory = priceHistory; }
    
    public double getRsi() { return rsi; }
    public void setRsi(double rsi) { this.rsi = rsi; }
    
    public double getMacd() { return macd; }
    public void setMacd(double macd) { this.macd = macd; }
    
    public double getMacdSignal() { return macdSignal; }
    public void setMacdSignal(double macdSignal) { this.macdSignal = macdSignal; }
    
    public double getSma10() { return sma10; }
    public void setSma10(double sma10) { this.sma10 = sma10; }
    
    public double getSma50() { return sma50; }
    public void setSma50(double sma50) { this.sma50 = sma50; }
    
    public double getEma10() { return ema10; }
    public void setEma10(double ema10) { this.ema10 = ema10; }
    
    public double getEma50() { return ema50; }
    public void setEma50(double ema50) { this.ema50 = ema50; }
    
    public double getSupportLevel() { return supportLevel; }
    public void setSupportLevel(double supportLevel) { this.supportLevel = supportLevel; }
    
    public double getResistanceLevel() { return resistanceLevel; }
    public void setResistanceLevel(double resistanceLevel) { this.resistanceLevel = resistanceLevel; }
    
    public double getFibonacciSupport38() { return fibonacciSupport38; }
    public void setFibonacciSupport38(double fibonacciSupport38) { this.fibonacciSupport38 = fibonacciSupport38; }
    
    public double getFibonacciSupport50() { return fibonacciSupport50; }
    public void setFibonacciSupport50(double fibonacciSupport50) { this.fibonacciSupport50 = fibonacciSupport50; }
    
    public double getFibonacciSupport61() { return fibonacciSupport61; }
    public void setFibonacciSupport61(double fibonacciSupport61) { this.fibonacciSupport61 = fibonacciSupport61; }
    
    public double getAverageVolume20() { return averageVolume20; }
    public void setAverageVolume20(double averageVolume20) { this.averageVolume20 = averageVolume20; }
    
    public double getCurrentVolume() { return currentVolume; }
    public void setCurrentVolume(double currentVolume) { this.currentVolume = currentVolume; }
    
    public boolean isVolumeConfirmation() { return volumeConfirmation; }
    public void setVolumeConfirmation(boolean volumeConfirmation) { this.volumeConfirmation = volumeConfirmation; }
    
    public TrendDirection getTrend() { return trend; }
    public void setTrend(TrendDirection trend) { this.trend = trend; }
    
    public double getTrendlineSupport() { return trendlineSupport; }
    public void setTrendlineSupport(double trendlineSupport) { this.trendlineSupport = trendlineSupport; }
    
    public double getTrendlineResistance() { return trendlineResistance; }
    public void setTrendlineResistance(double trendlineResistance) { this.trendlineResistance = trendlineResistance; }
    
    public List<EntrySignal> getEntrySignals() { return entrySignals; }
    public void setEntrySignals(List<EntrySignal> entrySignals) { this.entrySignals = entrySignals; }
    
    public EntryQuality getOverallEntryQuality() { return overallEntryQuality; }
    public void setOverallEntryQuality(EntryQuality overallEntryQuality) { this.overallEntryQuality = overallEntryQuality; }
    
    /**
     * Get numeric entry quality score (0-100)
     */
    public double getEntryQualityScore() {
        switch (overallEntryQuality) {
            case EXCELLENT: return 95.0;
            case GOOD: return 80.0;
            case AVERAGE: return 60.0;
            case POOR: return 30.0;
            case VERY_POOR: return 10.0;
            default: return 0.0;
        }
    }
    
    /**
     * Get volume ratio (current/average)
     */
    public double getVolumeRatio() {
        if (averageVolume20 > 0) {
            return currentVolume / averageVolume20;
        }
        return 1.0;
    }
    
    /**
     * Get SMA20 value (using SMA10 as fallback)
     */
    public double getSma20() {
        return sma10; // Using SMA10 as SMA20 fallback
    }
    
    /**
     * Add an entry signal to the analysis
     */
    public void addEntrySignal(EntryTechnique technique, SignalStrength strength, String description, 
                              double targetPrice, double stopLoss, double confidence) {
        entrySignals.add(new EntrySignal(technique, strength, description, targetPrice, stopLoss, confidence));
    }
    
    /**
     * Get formatted analysis summary
     */
    public String getAnalysisSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("📈 TECHNICAL ANALYSIS SUMMARY\n");
        summary.append("════════════════════════════════\n\n");
        
        // Overall quality
        summary.append("🎯 Entry Quality: ").append(getQualityEmoji()).append(" ").append(overallEntryQuality).append("\n");
        summary.append("📊 Trend Direction: ").append(getTrendEmoji()).append(" ").append(trend).append("\n\n");
        
        // Key levels
        summary.append("🔻 Support Level: $").append(String.format("%.2f", supportLevel)).append("\n");
        summary.append("🔺 Resistance Level: $").append(String.format("%.2f", resistanceLevel)).append("\n\n");
        
        // Technical indicators
        summary.append("📉 RSI: ").append(String.format("%.1f", rsi)).append(getRsiSignal()).append("\n");
        summary.append("📈 MACD: ").append(String.format("%.4f", macd)).append(getMacdSignalText()).append("\n");
        summary.append("📊 SMA10/50: $").append(String.format("%.2f/$%.2f", sma10, sma50)).append(getMASignal()).append("\n\n");
        
        // Entry signals
        if (!entrySignals.isEmpty()) {
            summary.append("🎯 ENTRY SIGNALS:\n");
            for (EntrySignal signal : entrySignals) {
                summary.append("• ").append(getStrengthEmoji(signal.strength)).append(" ")
                       .append(signal.technique.name()).append(": ").append(signal.description).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    private String getQualityEmoji() {
        switch (overallEntryQuality) {
            case EXCELLENT: return "🟢🟢🟢";
            case GOOD: return "🟢🟢⚪";
            case AVERAGE: return "🟢⚪⚪";
            case POOR: return "🟡⚪⚪";
            case VERY_POOR: return "🔴⚪⚪";
            default: return "⚪⚪⚪";
        }
    }
    
    private String getTrendEmoji() {
        switch (trend) {
            case BULLISH: return "📈";
            case BEARISH: return "📉";
            case NEUTRAL: return "➡️";
            default: return "❓";
        }
    }
    
    private String getStrengthEmoji(SignalStrength strength) {
        switch (strength) {
            case VERY_STRONG: return "🟢🟢🟢";
            case STRONG: return "🟢🟢⚪";
            case MODERATE: return "🟡🟡⚪";
            case WEAK: return "🟡⚪⚪";
            case VERY_WEAK: return "🔴⚪⚪";
            default: return "⚪⚪⚪";
        }
    }
    
    private String getRsiSignal() {
        if (rsi < 30) return " (Oversold - Buy Signal 🟢)";
        else if (rsi > 70) return " (Overbought - Sell Signal 🔴)";
        else return " (Neutral)";
    }
    
    private String getMacdSignalText() {
        if (macd > macdSignal) return " (Bullish 🟢)";
        else return " (Bearish 🔴)";
    }
    
    private String getMASignal() {
        if (sma10 > sma50) return " (Golden Cross 🟢)";
        else return " (Death Cross 🔴)";
    }
}
