@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ====================================================================
echo        Classify - Automated APK Build ^& Wi-Fi Server
echo ====================================================================
echo.

where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    python serve_apk.py %*
) else (
    echo [1/2] در حال بیلد پروژه...
    call gradlew.bat assembleDebug
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] خطا در بیلد گردل!
        pause
        exit /b %ERRORLEVEL%
    )
    echo.
    echo [2/2] راه‌اندازی سرور پایتون...
    python -m http.server 8080 -d app\build\outputs\apk\debug
)

if %ERRORLEVEL% NEQ 0 (
    pause
)
