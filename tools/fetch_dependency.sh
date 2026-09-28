#!/usr/bin/env bash
# One dependency per invocation; never chains several downloads beyond one minute.
set -euo pipefail
VENDOR="${VENDOR:-$HOME/.cache/wd-vendor}";mkdir -p "$VENDOR"
case "${1:-}" in
 game) url='https://github.com/Anuken/Mindustry/releases/download/v160.5/Mindustry.jar';file=Mindustry.jar;sum=c2fd5a5dcb8d306525bb47ff28121237d4d25f53868562946491f2ef261dc272;;
 server) url='https://github.com/Anuken/Mindustry/releases/download/v160.5/server-release.jar';file=server-release.jar;sum=0bd327c6c3d551e7e8fdab7b695517f809baacca3b1f5cb1c1a8dd74836620e0;;
 r8) url='https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.9.35/r8-8.9.35.jar';file=r8.jar;sum='';;
 android) url='https://dl.google.com/android/repository/platform-35_r02.zip';file=platform-35.zip;sum='';;
 *) echo 'Choose ONE: game | server | r8 | android. Supply JDK17+ separately.';exit 2;;
esac
curl --fail --location --connect-timeout 8 --max-time 40 "$url" -o "$VENDOR/$file.tmp"
if [ -n "$sum" ];then echo "$sum  $VENDOR/$file.tmp" | sha256sum -c -;fi
mv "$VENDOR/$file.tmp" "$VENDOR/$file"
if [ "$1" = android ];then
 name=$(unzip -Z1 "$VENDOR/$file" | grep '/android.jar$' | head -1)
 unzip -p "$VENDOR/$file" "$name" > "$VENDOR/android.jar"
fi
echo "Ready: $VENDOR/$file"
