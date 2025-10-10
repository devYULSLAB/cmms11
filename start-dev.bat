@echo off
REM CMMS11 Development Server Start Script (Windows)
REM 프론트그라운드로 실행

echo ========================================
echo CMMS11 Development Server Starting...
echo ========================================
echo [INFO] Profile: dev
echo [INFO] Press Ctrl+C to stop
echo ========================================

REM Gradle bootRun으로 개발 서버 실행 (UTF-8 인코딩 설정 포함)
call gradlew bootRun

echo.
echo [INFO] Server stopped.

