#!/bin/bash
# CMMS11 Production Server Start Script

APP_NAME="cmms11"
APP_DIR="/opt/cmms11"
JAR_FILE=$(ls ${APP_DIR}/${APP_NAME}-*.jar 2>/dev/null | head -n 1)
PID_FILE="${APP_DIR}/${APP_NAME}.pid"
LOG_FILE="${APP_DIR}/logs/app.out"

echo "========================================"
echo "CMMS11 Production Server Starting..."
echo "========================================"

# JAR 파일 확인
if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR file not found in ${APP_DIR}"
    exit 1
fi

# 이미 실행 중인지 확인
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "[ERROR] Server is already running (PID: $PID)"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# 로그 디렉토리 생성
mkdir -p "${APP_DIR}/logs"

# 서버 시작
echo "[INFO] Starting: $JAR_FILE"
echo "[INFO] Profile: prod"
echo "[INFO] Log: $LOG_FILE"
echo "========================================"

nohup java -Xms1g -Xmx2g -Dfile.encoding=UTF-8 \
    -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    > "$LOG_FILE" 2>&1 &

PID=$!
echo $PID > "$PID_FILE"

sleep 2

if ps -p $PID > /dev/null 2>&1; then
    echo "[SUCCESS] Server started (PID: $PID)"
else
    echo "[ERROR] Server failed to start"
    rm -f "$PID_FILE"
    exit 1
fi

