@echo off
setlocal

echo Starting CMMS11 Development Server...

REM Change to project root directory
cd /d "%~dp0.."

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please run this script from the project root directory.
    pause
    exit /b 1
)

REM Set environment variables for development
set SPRING_PROFILES_ACTIVE=dev
set JAVA_OPTS=-Xms256m -Xmx512m -Dfile.encoding=UTF-8

REM Create necessary directories
if not exist "storage\uploads" mkdir "storage\uploads"
if not exist "logs" mkdir "logs"

echo Building application...
call gradlew.bat clean build -x test
if %errorlevel% neq 0 (
    echo Error: Build failed
    pause
    exit /b 1
)

echo Starting application with development profile...
echo Application will be available at: http://localhost:8080
echo Press Ctrl+C to stop the server...
echo.

gradlew.bat bootRun --args='--spring.profiles.active=dev'
