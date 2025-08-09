#!/bin/bash
# Wrapper script for gradlew with automatic Java environment setup

# Set Java environment for this project
export JAVA_HOME=/opt/java-17
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java is available
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "❌ Error: Java 17 not found at $JAVA_HOME"
    echo "Please check your Java installation."
    exit 1
fi

echo "☕ Using Java: $(java -version 2>&1 | head -n1)"
echo "🏠 JAVA_HOME: $JAVA_HOME"

# Run gradlew with all passed arguments
exec ./gradlew "$@"
