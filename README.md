# Crypto Portfolio Tracker

A Java Swing-based cryptocurrency portfolio tracking application with real-time price monitoring.

## ✨ No-Build-Files Development Setup

This project is specifically configured to **avoid creating build files** during development. Perfect for keeping your workspace clean!

## 🚀 Features

- Real-time cryptocurrency price tracking
- Customizable watchlist with expected target prices
- Color-coded profit/loss indicators
- Auto-refresh functionality (30-second intervals)
- Add/remove cryptocurrencies dynamically
- Edit expected prices on-the-fly
- **Zero build artifacts** during development

## 📋 Requirements

- Java 17 or higher (configured for Java 17)
- Internet connection for price data

## 🎯 Running the Application

### Development Mode (No Build Files Created)

The project is configured to avoid creating build artifacts during development. Here are your options:

#### Option 1: Development Script (Recommended)
```bash
# Run without any build artifacts
./run-dev.sh
```

#### Option 2: Gradle Development Task
```bash
# Gradle task that cleans up after itself
./gradlew dev
```

#### Option 3: Standard Gradle Run (Auto-cleanup)
```bash
# Regular run task with automatic cleanup
./gradlew run
```

#### Option 4: Direct Java Compilation
```bash
# Manual compilation and execution
javac -cp ".:json-20231013.jar:src/main/java" src/main/java/*.java
java -cp ".:json-20231013.jar:src/main/java" CryptoApp
```

## 🧹 Workspace Management

### Clean Workspace
```bash
# Remove any accidentally created build files
./clean-workspace.sh
```

### Check for Build Files
```bash
# Verify no build directories exist
find . -name "build" -type d || echo "Clean workspace ✅"
```

## 🏗️ How No-Build-Files Works

1. **Gradle Configuration**: `build.gradle` disables JAR creation and uses temporary directories
2. **Auto-cleanup**: Build directories are automatically deleted after tasks
3. **Development Script**: Direct compilation without Gradle overhead
4. **Temporary Builds**: When needed, builds go to `/tmp` and are cleaned up

## 📁 Project Structure

```
├── build.gradle              # No-build-files Gradle configuration
├── gradlew                   # Gradle wrapper script
├── run-dev.sh               # Development runner (no artifacts)
├── clean-workspace.sh       # Workspace cleanup utility
├── gradle/wrapper/          # Gradle wrapper files
└── src/main/java/
    ├── CryptoApp.java              # Main launcher
    ├── CryptoPortfolioGUI.java     # GUI application
    ├── ModernCryptoPortfolioGUI.java # Enhanced GUI (alternative)
    └── CryptoPriceApp.java         # Console version
```

## API

This application uses the [CoinGecko API](https://www.coingecko.com/en/api) to fetch real-time cryptocurrency prices.

## Project Structure

```
├── build.gradle              # Gradle build configuration
├── gradlew                   # Gradle wrapper script (Unix)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/
    └── main/
        └── java/
            ├── CryptoPortfolioGUI.java    # Main GUI application
            └── CryptoPriceApp.java        # Simple console version
```
