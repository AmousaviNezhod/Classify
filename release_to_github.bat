@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ====================================================================
echo        Classify v2.3 - Automated Git Push ^& GitHub Release
echo ====================================================================
echo.

echo [1/4] افزودن تغییرات به گیت (git add .)...
git add .
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] خطا در اجرای git add! لطفاً گیت را بررسی کنید.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/4] ثبت کامیت نسخه ۲.۳ (git commit)...
git commit -m "chore(release): bump version to 2.3 and update release notes"
if %ERRORLEVEL% NEQ 0 (
    echo [INFO] تغییر جدیدی برای کامیت یافت نشد یا قبلاً کامیت شده است.
)

echo.
echo [3/4] ایجاد تگ v2.3...
git tag -d v2.3 >nul 2>nul
git tag -a v2.3 -m "Classify v2.3"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] خطا در ایجاد تگ!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [4/4] ارسال تغییرات و تگ به گیت‌هاب (git push)...
git push origin HEAD
git push origin v2.3 --force
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] خطا در ارسال به گیت‌هاب! لطفاً اتصال اینترنت یا دسترسی احراز هویت گیت‌هاب را بررسی کنید.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ====================================================================
echo        ایجاد رسمی ریلیز در گیت‌هاب (GitHub Release)...
echo ====================================================================

where gh >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ابزار GitHub CLI (gh) یافت شد؛ در حال انتشار ریلیز...
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        gh release create v2.3 "app\build\outputs\apk\debug\app-debug.apk#Classify-v2.3-debug.apk" --title "Classify v2.3 — تفکیک جلسات نظری/عملی، میانبر جلسه بعدی و بهبود رویدادها" --notes-file RELEASE_NOTES_v2.3.md
    ) else (
        gh release create v2.3 --title "Classify v2.3 — تفکیک جلسات نظری/عملی، میانبر جلسه بعدی و بهبود رویدادها" --notes-file RELEASE_NOTES_v2.3.md
    )
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo 🎉 ریلیز v2.3 با موفقیت در گیت‌هاب منتشر شد!
        start https://github.com/AmousaviNezhod/Classify/releases/tag/v2.3
    )
) else (
    echo ابزار gh CLI شناسایی نشد؛ در حال باز کردن صفحه ساخت ریلیز گیت‌هاب در مرورگر...
    start https://github.com/AmousaviNezhod/Classify/releases/new?tag=v2.3
    echo.
    echo کافیست متن فایل RELEASE_NOTES_v2.3.md را در توضیحات پیست کنید.
)

echo.
echo ====================================================================
echo ✅ کلیه تغییرات به مخزن گیت‌هاب ارسال شدند.
echo ====================================================================
pause
