#!/bin/bash
# CMMS11 Development Server Stop Script

APP_NAME="cmms11"
APP_DIR="/opt/cmms11"
PID_FILE="${APP_DIR}/${APP_NAME}-dev.pid"

echo "========================================"
echo "CMMS11 Development Server Stopping..."
echo "========================================"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "[WARNING] PID file not found"
    echo "[INFO] Searching for running process..."
    
    PID=$(ps aux | grep "[j]ava.*${APP_NAME}.*\.jar.*dev" | awk '{print $2}')
    
    if [ -z "$PID" ]; then
        echo "[INFO] Server is not running"
        exit 0
    fi
else
    PID=$(cat "$PID_FILE")
fi

# Check if process is running
if ! ps -p $PID > /dev/null 2>&1; then
    echo "[INFO] Server is not running (stale PID file)"
    rm -f "$PID_FILE"
    exit 0
fi

# Stop the process
echo "[INFO] Stopping server (PID: $PID)"
kill $PID

# Wait for graceful shutdown
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "[SUCCESS] Server stopped"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Force kill if still running
echo "[WARNING] Forcing shutdown..."
kill -9 $PID
rm -f "$PID_FILE"
echo "[SUCCESS] Server forcefully stopped"
