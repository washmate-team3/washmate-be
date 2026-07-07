# ============================================================
# WashMate Backend - start against Supabase using environment
# variables supplied by the developer or secret manager.
# Tu dong nap bien tu file .env (neu co) truoc khi kiem tra.
# ============================================================

# Nap .env: dong dang KEY=VALUE, bo qua comment (#) va dong trong.
# Bien da set san trong environment se KHONG bi ghi de.
$envFile = Join-Path $PSScriptRoot '.env'
if (Test-Path $envFile) {
    Write-Host ">>> Loading environment variables from .env..." -ForegroundColor Gray
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $key, $value = $line.Split('=', 2)
            $key = $key.Trim()
            if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key))) {
                [Environment]::SetEnvironmentVariable($key, $value.Trim(), 'Process')
            }
        }
    }
}

$requiredVariables = @(
    'DB_URL',
    'DB_USERNAME',
    'DB_PASSWORD',
    'JWT_SECRET',
    'CORS_ALLOWED_ORIGIN_PATTERNS'
)

$missingVariables = $requiredVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_))
}

if ($missingVariables.Count -gt 0) {
    throw "Missing required environment variables: $($missingVariables -join ', ')"
}

$existing = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host ">>> Port 8080 is in use; stopping the current listener..." -ForegroundColor Yellow
    $existing | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
    Start-Sleep -Seconds 1
}

Write-Host ">>> Starting WashMate Backend (profile: supabase)..." -ForegroundColor Cyan
Write-Host ">>> Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host ">>> Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=supabase"
