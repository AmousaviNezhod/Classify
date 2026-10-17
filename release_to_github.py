#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Classify - Automated Git Push and GitHub Release Script
"""

import subprocess
import sys
import os
import webbrowser

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
RELEASE_NOTES = os.path.join(ROOT_DIR, "RELEASE_NOTES_v2.3.md")
APK_PATH = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
TAG = "v2.3"
TITLE = "Classify v2.3 — تفکیک جلسات نظری/عملی، میانبر جلسه بعدی و بهبود رویدادها"

def run(cmd, check=True):
    print(f"👉 {' '.join(cmd)}")
    return subprocess.run(cmd, cwd=ROOT_DIR, check=check)

def main():
    print("=" * 68)
    print("  🚀 [1/4] افزودن تغییرات به گیت (git add .)...")
    print("=" * 68)
    run(["git", "add", "."])

    print("\n" + "=" * 68)
    print("  📝 [2/4] ثبت کامیت نسخه ۲.۳...")
    print("=" * 68)
    run(["git", "commit", "-m", "chore(release): bump version to 2.3 and update release notes"], check=False)

    print("\n" + "=" * 68)
    print(f"  🏷️  [3/4] ایجاد تگ {TAG}...")
    print("=" * 68)
    run(["git", "tag", "-d", TAG], check=False)
    run(["git", "tag", "-a", TAG, "-m", f"Classify {TAG}"])

    print("\n" + "=" * 68)
    print("  🌐 [4/4] ارسال به مخزن گیت‌هاب (git push)...")
    print("=" * 68)
    run(["git", "push", "origin", "HEAD"])
    run(["git", "push", "origin", TAG, "--force"])

    print("\n" + "=" * 68)
    print("  🎉 ایجاد ریلیز در گیت‌هاب (GitHub Release)...")
    print("=" * 68)
    gh_available = subprocess.run(["where" if sys.platform == "win32" else "which", "gh"], capture_output=True).returncode == 0
    if gh_available:
        print("  ✅ ابزار GitHub CLI یافت شد؛ در حال ایجاد ریلیز...")
        gh_cmd = ["gh", "release", "create", TAG, "--title", TITLE, "--notes-file", RELEASE_NOTES]
        if os.path.exists(APK_PATH):
            gh_cmd.append(f"{APK_PATH}#Classify-{TAG}-debug.apk")
        run(gh_cmd)
        webbrowser.open(f"https://github.com/AmousaviNezhod/Classify/releases/tag/{TAG}")
    else:
        print("  🌐 ابزار gh CLI نصب نیست؛ صفحه ساخت ریلیز در مرورگر باز می‌شود...")
        webbrowser.open(f"https://github.com/AmousaviNezhod/Classify/releases/new?tag={TAG}")

    print("\n✅ عملیات با موفقیت به پایان رسید.")

if __name__ == "__main__":
    main()
