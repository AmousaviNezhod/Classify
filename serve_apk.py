#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Classify - Automated APK Builder & Local Wi-Fi HTTP Server
Builds the latest debug APK via Gradle and serves it over local Wi-Fi for phone installation.
"""

import os
import sys
import socket
import http.server
import socketserver
import subprocess
import time
from datetime import datetime

try:
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')
except Exception:
    pass

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
APK_OUTPUT_DIR = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "debug")
APK_FILE = os.path.join(APK_OUTPUT_DIR, "app-debug.apk")
PORT = 8080

def get_local_ip():
    """Detects the machine's local LAN IP address, prioritizing Wi-Fi."""
    try:
        cmd = ["powershell", "-NoProfile", "-Command", "(Get-NetIPAddress -InterfaceAlias 'Wi-Fi*' -AddressFamily IPv4).IPAddress"]
        out = subprocess.check_output(cmd, text=True).strip()
        lines = [line.strip() for line in out.splitlines() if line.strip() and not line.strip().startswith("169.254")]
        if lines:
            return lines[0]
    except Exception:
        pass

    try:
        _, _, all_ips = socket.gethostbyname_ex(socket.gethostname())
        for ip in all_ips:
            if ip.startswith("10."):
                return ip
        for ip in all_ips:
            if ip.startswith("192.168.") and not any(ip.startswith(f"192.168.{x}.") for x in [56, 92, 105]):
                return ip
        for ip in all_ips:
            if not ip.startswith("127.") and not ip.startswith("169.254.") and not ip.startswith("172.18."):
                return ip
    except Exception:
        pass

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def build_apk():
    """Executes Gradle assembleDebug."""
    print("=" * 68)
    print("  🔨 [1/2] در حال بیلد نسخه دیباگ (gradlew assembleDebug)...")
    print("=" * 68)
    gradlew_cmd = os.path.join(ROOT_DIR, "gradlew.bat") if sys.platform == "win32" else os.path.join(ROOT_DIR, "gradlew")
    if not os.path.exists(gradlew_cmd):
        gradlew_cmd = "gradlew"

    cmd = [gradlew_cmd, "assembleDebug"]
    start_time = time.time()
    result = subprocess.run(cmd, cwd=ROOT_DIR, shell=(sys.platform == "win32"))
    elapsed = time.time() - start_time

    if result.returncode != 0:
        print("\n❌ خطا در بیلد گردل! کد خروج:", result.returncode)
        sys.exit(result.returncode)

    if not os.path.exists(APK_FILE):
        print("\n❌ فایل APK در مسیر یافت نشد:", APK_FILE)
        sys.exit(1)

    apk_size_mb = os.path.getsize(APK_FILE) / (1024 * 1024)
    print(f"\n✅ بیلد با موفقیت انجام شد ({elapsed:.1f} ثانیه)")
    print(f"📦 فایل APK: {APK_FILE} ({apk_size_mb:.2f} MB)")

def create_landing_page(local_ip):
    """Creates a user-friendly download web page inside the APK directory."""
    apk_debug_mb = os.path.getsize(APK_FILE) / (1024 * 1024) if os.path.exists(APK_FILE) else 0.0
    release_apk = os.path.join(APK_OUTPUT_DIR, "app-release.apk")
    apk_release_mb = os.path.getsize(release_apk) / (1024 * 1024) if os.path.exists(release_apk) else 0.0
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    release_button_html = f'''
        <a href="app-release.apk" download class="btn" style="background: #059669; margin-top: 12px; box-shadow: 0 4px 14px rgba(5, 150, 105, 0.4);">📦 دانلود نسخه ریلیز (Release) ({apk_release_mb:.2f} MB)</a>
    ''' if os.path.exists(release_apk) else ""

    html_content = f"""<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>دانلود Classify APK</title>
    <style>
        * {{ box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Tahoma, sans-serif; }}
        body {{ background: #0f141c; color: #e2e8f0; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 20px; }}
        .card {{ background: #1a2230; border: 1px solid #2d3748; border-radius: 20px; max-width: 440px; width: 100%; padding: 28px; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }}
        .logo {{ font-size: 46px; margin-bottom: 10px; }}
        h1 {{ font-size: 22px; color: #ffffff; margin-bottom: 6px; font-weight: 700; }}
        .subtitle {{ font-size: 13px; color: #94a3b8; margin-bottom: 22px; }}
        .badge {{ display: inline-block; background: rgba(59, 130, 246, 0.15); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.3); padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: bold; margin-bottom: 20px; }}
        .btn {{ display: block; background: #2563eb; color: #ffffff; text-decoration: none; padding: 16px; border-radius: 14px; font-size: 17px; font-weight: bold; transition: all 0.2s; box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4); }}
        .btn:hover {{ filter: brightness(1.1); transform: translateY(-2px); }}
        .info {{ margin-top: 22px; background: #131924; border-radius: 12px; padding: 14px; text-align: right; font-size: 13px; color: #cbd5e1; line-height: 1.8; }}
        .info-title {{ font-weight: bold; color: #93c5fd; margin-bottom: 4px; }}
        .footer {{ margin-top: 18px; font-size: 11px; color: #64748b; }}
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">📚</div>
        <h1>اپلیکیشن کلاسیفای (Classify)</h1>
        <div class="subtitle">نسخه جدید آماده نصب روی گوشی</div>
        <div class="badge">نسخه ۲.۳ (v2.3) · بیلد جدید</div>
        <a href="app-debug.apk" download class="btn">📥 دانلود نسخه دیباگ (پیشنهادی) ({apk_debug_mb:.2f} MB)</a>
        {release_button_html}
        <div class="info">
            <div class="info-title">✨ تغییرات اعمال‌شده:</div>
            <div>• میانبر هوشمند «جلسه بعدی کلاس» در تقویم</div>
            <div>• عنوان اختیاری برای رویدادها و امکان ثبت عنوان سفارشی</div>
            <div>• تفکیک هوشمند جلسات نظری و عملی دروس با مکان مستقل</div>
            <div>• اصلاح یادآورها و اعلان‌های رویدادها</div>
        </div>
        <div class="footer">تاریخ ایجاد بیلد: {now_str}</div>
    </div>
</body>
</html>
"""
    os.makedirs(APK_OUTPUT_DIR, exist_ok=True)
    index_file = os.path.join(APK_OUTPUT_DIR, "index.html")
    with open(index_file, "w", encoding="utf-8") as f:
        f.write(html_content)

def serve():
    """Starts the Python HTTP Server."""
    local_ip = get_local_ip()
    create_landing_page(local_ip)

    print("\n" + "=" * 68)
    print("  🚀 [2/2] سرور پایتون برای دانلود روی گوشی فعال شد!")
    print("=" * 68)
    print(f"  📱 روی گوشی (متصل به همین Wi-Fi) یکی از دو آدرس زیر را باز کنید:")
    print(f"     ۱. صفحه دانلود:      http://{local_ip}:{PORT}/")
    print(f"     ۲. دانلود مستقیم:    http://{local_ip}:{PORT}/app-debug.apk")
    print("-" * 68)
    print(f"  📂 پوشه در حال اشتراک: {APK_OUTPUT_DIR}")
    print(f"  💻 آدرس در کامپیوتر:    http://localhost:{PORT}/")
    print("=" * 68)
    print("  💡 برای توقف سرور، کلیدهای Ctrl + C را فشار دهید.\n")

    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=APK_OUTPUT_DIR, **kwargs)

        def log_message(self, format, *args):
            print(f"[{datetime.now().strftime('%H:%M:%S')}] {args[0]} - {args[1]} ({args[2]})")

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 سرور پایتون متوقف شد.")

def main():
    skip_build = "--no-build" in sys.argv or "--serve-only" in sys.argv
    if not skip_build:
        build_apk()
    serve()

if __name__ == "__main__":
    main()
