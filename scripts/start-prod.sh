#!/bin/bash

# CMMS11 Production Server Start Script
# Usage: ./start-prod.sh
# - Uses /opt/cmms11/ as the working directory with consistent structure
# - Always runs in background with nohup
# - JAR files, scripts, logs, storage, and yml files are all under /opt/cmms11/

set -e

echo "Starting CMMS11 Production Server..."

# Set consistent working directory
REPO_ROOT="/opt/cmms11"
LOG_DIR="$REPO_ROOT/logs"
PID_FILE="$LOG_DIR/app.pid"
OUT_FILE="$LOG_DIR/app.out"

# Check if running as root or with sudo
if [[ $EUID -eq 0 ]]; then
   echo "This script should not be run as root for security reasons"
   exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# Check if JAR file exists
JAR_CANDIDATE=$(ls -1t /opt/cmms11/*.jar 2>/dev/null | head -n1 || true)
if [[ -z "$JAR_CANDIDATE" ]]; then
    echo "Error: No JAR file found in /opt/cmms11/. Please build and deploy the JAR first."
    exit 1
fi

# Check if already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "Error: CMMS11 is already running (PID: $PID)"
        echo "Stop it first with: ./stop-prod.sh"
        exit 1
    else
        echo "Removing stale PID file..."
        rm -f "$PID_FILE"
    fi
fi

# Set environment variables for production
export SPRING_PROFILES_ACTIVE=prod
export JAVA_OPTS="-Xms1g -Xmx2g -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

# AWS configuration (환경변수로 오버라이드 가능)
export AWS_S3_BUCKET=${AWS_S3_BUCKET:-"prodYULSLAB-bucket"}

# Create necessary directories with consistent structure
mkdir -p "$REPO_ROOT/storage/uploads"
mkdir -p "$LOG_DIR"
mkdir -p "$REPO_ROOT/scripts"

echo "[INFO] Working dir:    $REPO_ROOT"
echo "[INFO] Using JAR:      $JAR_CANDIDATE"
echo "[INFO] Profile:        prod"
echo "[INFO] Java opts:      $JAVA_OPTS"
echo "[INFO] Log dir:        $LOG_DIR"
echo "[INFO] Mode:           background (nohup)"
echo ""

# Change to working directory
cd "$REPO_ROOT"

# Start the service with nohup
echo "Starting CMMS11 service..."
nohup java $JAVA_OPTS -jar "$JAR_CANDIDATE" --spring.profiles.active=prod >> "$OUT_FILE" 2>&1 &
echo $! > "$PID_FILE"

sleep 2

# Verify it started
if ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
    echo "CMMS11 Production Server started successfully!"
    echo "PID: $(cat "$PID_FILE")"
    echo "Logs: $OUT_FILE"
    echo ""
    echo "Commands:"
    echo "  View logs:  tail -f $OUT_FILE"
    echo "  Check PID:  cat $PID_FILE"
    echo "  Stop:       ./stop-prod.sh"
else
    echo "Error: Failed to start CMMS11"
    echo "Check logs: tail -n 50 $OUT_FILE"
    exit 1
fi
