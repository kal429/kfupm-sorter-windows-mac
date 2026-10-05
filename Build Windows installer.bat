@echo off
rem Builds the KFUPM Sorter installer on this PC. Nothing is uploaded anywhere.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0packaging\build-windows.ps1"
echo.
if errorlevel 1 (echo Something went wrong - see build\build-log.txt) else (echo The installer is in build\installer\KFUPM-Sorter-Windows-Setup.exe)
pause
