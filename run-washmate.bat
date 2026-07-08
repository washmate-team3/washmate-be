@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File start-local.ps1
pause
