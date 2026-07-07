@echo off
REM AI Crop Advisor - Startup Script
REM API Keys are hardcoded in application.properties

echo.
echo ========================================
echo AI Crop Lifecycle Advisor
echo ========================================
echo.
echo Starting application...
echo.
echo Gemini AI:     ENABLED ✓
echo OpenWeather:   ENABLED ✓
echo PostgreSQL:    ENABLED ✓
echo.
echo ========================================
echo Access at: http://localhost:8080
echo API Base:  http://localhost:8080/api
echo ========================================
echo.
echo Press Ctrl+C to stop
echo.

REM Build and run
mvn clean compile spring-boot:run

pause
