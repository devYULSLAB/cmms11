#!/bin/bash
# CMMS11 Development Server Start Script

APP_NAME="cmms11"
APP_DIR="/opt/cmms11"
JAR_FILE=$(ls ${APP_DIR}/build/libs/${APP_NAME}-*.jar 2>/dev/null | head -n 1)
PID_FILE="${APP_DIR}/${APP_NAME}-dev.pid"
LOG_FILE="${APP_DIR}/logs/cmms11-dev.out"

echo "========================================"
echo "CMMS11 Development Server Starting..."
echo "========================================"

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR file not found in ${APP_DIR}/build/libs"
    echo "[INFO] Please build the project first: ./gradlew build"
    exit 1
fi

# Check if server is already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "[ERROR] Server is already running (PID: $PID)"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# Create log directory
mkdir -p "${APP_DIR}/logs"

# Start server
echo "[INFO] Starting: $JAR_FILE"
echo "[INFO] Profile: dev"
echo "[INFO] Log: $LOG_FILE"
echo "========================================"

nohup java -Xms512m -Xmx1g -Dfile.encoding=UTF-8 \
    -jar "$JAR_FILE" \
    --spring.profiles.active=dev \
    > "$LOG_FILE" 2>&1 &

PID=$!
echo $PID > "$PID_FILE"

sleep 2

if ps -p $PID > /dev/null 2>&1; then
    echo "[SUCCESS] Server started (PID: $PID)"
    echo "[INFO] To stop: ./stop-dev.sh"
    echo "[INFO] View logs: tail -f $LOG_FILE"
else
    echo "[ERROR] Server failed to start"
    echo "[INFO] Check logs: cat $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi
