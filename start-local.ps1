# ============================================================
# WashMate Backend - Script khởi động local với Supabase
# Cách dùng: .\start-local.ps1
# ============================================================

# Dừng process đang dùng port 8080 nếu có
$existing = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host ">>> Port 8080 dang duoc su dung, dang dong..." -ForegroundColor Yellow
    $existing | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
    Start-Sleep -Seconds 1
}

# Set bien moi truong
$env:DB_URL      = 'jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0'
$env:DB_USERNAME = 'postgres.xobkqehriftlivzczqki'
$env:DB_PASSWORD = 'MiniProjectAutoWash'
$env:JWT_SECRET  = 'washmate-secret-at-least-32-characters-long'
$env:OTP_MOCK_CODE         = ''
$env:OTP_EXPOSE_MOCK_CODE  = 'false'
$env:DOCS_ENABLED          = 'true'

Write-Host ">>> Khoi dong WashMate Backend (profile: supabase)..." -ForegroundColor Cyan
Write-Host ">>> Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host ">>> Nhan Ctrl+C de dung ung dung" -ForegroundColor Gray
Write-Host ""

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=supabase"