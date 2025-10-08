@echo off
REM CMMS11 Development Server Start Script (Windows)
REM 프론트그라운드로 실행

echo ========================================
echo CMMS11 Development Server Starting...
echo ========================================

REM JAR 파일 찾기
for %%f in (build\libs\cmms11-*.jar) do set JAR_FILE=%%f

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR file not found in build\libs\
    echo [INFO] Please build first: gradlew bootJar
    exit /b 1
)

echo [INFO] Starting: %JAR_FILE%
echo [INFO] Profile: dev
echo [INFO] Press Ctrl+C to stop
echo ========================================

REM 개발 서버 실행
java -jar "%JAR_FILE%" --spring.profiles.active=dev

echo.
echo [INFO] Server stopped.

