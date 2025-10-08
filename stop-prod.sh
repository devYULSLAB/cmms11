#!/bin/bash
# CMMS11 Production Server Stop Script

APP_NAME="cmms11"
APP_DIR="/opt/cmms11"
PID_FILE="${APP_DIR}/${APP_NAME}.pid"

echo "========================================"
echo "CMMS11 Production Server Stopping..."
echo "========================================"

# PID 파일 확인
if [ ! -f "$PID_FILE" ]; then
    echo "[WARNING] PID file not found"
    echo "[INFO] Searching for running process..."
    
    PID=$(ps aux | grep "[j]ava.*${APP_NAME}.*\.jar" | awk '{print $2}')
    
    if [ -z "$PID" ]; then
        echo "[INFO] Server is not running"
        exit 0
    fi
else
    PID=$(cat "$PID_FILE")
fi

# 프로세스 확인
if ! ps -p $PID > /dev/null 2>&1; then
    echo "[INFO] Server is not running (stale PID file)"
    rm -f "$PID_FILE"
    exit 0
fi

# 프로세스 종료
echo "[INFO] Stopping server (PID: $PID)"
kill $PID

# 종료 대기
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "[SUCCESS] Server stopped"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# 강제 종료
echo "[WARNING] Forcing shutdown..."
kill -9 $PID
rm -f "$PID_FILE"
echo "[SUCCESS] Server forcefully stopped"

