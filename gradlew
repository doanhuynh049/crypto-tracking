#!/bin/bash

##############################################################################
# Gradle wrapper script for Unix/Linux
##############################################################################

# Attempt to set APP_HOME
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# Default JVM options
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Find Java command
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
        exit 1
    fi
else
    JAVACMD="java"
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
        echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
        exit 1
    fi
fi

# Gradle wrapper jar location
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Check if wrapper jar exists
if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
    echo "Error: Gradle wrapper jar not found at $GRADLE_WRAPPER_JAR"
    exit 1
fi

# Execute Gradle
exec "$JAVACMD" -Xmx64m -Xms64m \
    -Dorg.gradle.appname="gradlew" \
    -classpath "$GRADLE_WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"