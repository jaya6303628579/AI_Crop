@echo off
REM ============================================
REM AI Crop Advisor - Development Setup Script
REM ============================================

echo.
echo ============================================
echo AI Crop Advisor - Starting Development Server
echo ============================================
echo.

REM Set JWT Secret
echo [*] Setting JWT Secret...
set JWT_SECRET=evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI=
echo [✓] JWT_SECRET set successfully

echo.
echo [*] Starting Maven...
echo.
echo ============================================
echo Server will be available at:
echo http://localhost:8080/api/login.html
echo ============================================
echo.

REM Run Maven
mvn spring-boot:run

pause
