# ============================================
# AI Crop Advisor - Development Setup Script (PowerShell)
# ============================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "AI Crop Advisor - Starting Development Server" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Set JWT Secret
Write-Host "[*] Setting JWT Secret..." -ForegroundColor Yellow
$env:JWT_SECRET = "evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI="
Write-Host "[✓] JWT_SECRET set successfully" -ForegroundColor Green

Write-Host ""
Write-Host "[*] Starting Maven..." -ForegroundColor Yellow
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "Server will be available at:" -ForegroundColor Green
Write-Host "http://localhost:8080/api/login.html" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

# Run Maven
mvn spring-boot:run

Write-Host ""
Write-Host "Press any key to close..." -ForegroundColor Yellow
[Console]::ReadKey()
