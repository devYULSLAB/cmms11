#!/bin/bash

# CMMS11 Production Server Stop Script
# Usage: ./stop-prod.sh

set -e

echo "Stopping CMMS11 Production Server..."

# Set consistent working directory
REPO_ROOT="/opt/cmms11"
LOG_DIR="$REPO_ROOT/logs"
PID_FILE="$LOG_DIR/app.pid"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found. CMMS11 may not be running"
    exit 0
fi

PID=$(cat "$PID_FILE")

# Check if process exists
if ! ps -p $PID > /dev/null 2>&1; then
    echo "CMMS11 is not running (stale PID file)"
    rm -f "$PID_FILE"
    exit 0
fi

# Stop the process
echo "Stopping CMMS11 (PID: $PID)..."
kill $PID
sleep 5

# Check if still running
if ps -p $PID > /dev/null 2>&1; then
    echo "Force stopping CMMS11..."
    kill -9 $PID
    sleep 2
fi

# Final check
if ps -p $PID > /dev/null 2>&1; then
    echo "Error: Failed to stop CMMS11 (PID: $PID)"
    exit 1
else
    rm -f "$PID_FILE"
    echo "CMMS11 Production Server stopped successfully!"
fi
