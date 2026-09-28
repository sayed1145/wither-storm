#!/usr/bin/env bash
set -euo pipefail
if command -v timeout >/dev/null 2>&1 && [ "${WS_BUILD_BOUNDED:-0}" != 1 ]; then
 exec env WS_BUILD_BOUNDED=1 timeout -k 2s 50s bash "$0" "$@"
fi
cd "$(dirname "$0")"
VENDOR="${VENDOR:-$HOME/.cache/wd-vendor}"
if [ -n "${JAVA_HOME:-}" ]; then J="$JAVA_HOME/bin/"; elif [ -x "$VENDOR/jdk17/bin/javac" ]; then J="$VENDOR/jdk17/bin/"; else J=""; fi
GAME="${MINDUSTRY_JAR:-$VENDOR/Mindustry.jar}"
[ -f "$GAME" ] || { echo 'Set MINDUSTRY_JAR to official v160.5 Mindustry.jar'; exit 1; }
mkdir -p .cache/classes .cache/stage .cache/dex release assets/music
if [ ! -f release/Wither-Storm-v1.7.1.jar ] && [ -f release/凋零风暴_v1.7.1_完整包.zip ]; then
 unzip -p release/凋零风暴_v1.7.1_完整包.zip 'Wither-Storm-v1.7.1/release/Wither-Storm-v1.7.1.jar' > .cache/recovered.jar
 mv .cache/recovered.jar release/Wither-Storm-v1.7.1.jar
fi
if [ ! -f assets/music/wither-storm-theme.ogg ]; then
 [ -f release/Wither-Storm-v1.7.1.jar ] || { echo 'Missing theme asset and existing release JAR'; exit 1; }
 unzip -p release/Wither-Storm-v1.7.1.jar music/wither-storm-theme.ogg > assets/music/wither-storm-theme.ogg
fi
find .cache/classes -type f -delete
"${J}javac" --release 17 -encoding UTF-8 -cp "$GAME" -d .cache/classes $(find src -name '*.java' | sort)
rm -rf .cache/stage .cache/dex;mkdir -p .cache/stage .cache/dex
cp -r .cache/classes/* .cache/stage/;cp -r assets/* .cache/stage/;cp mod.hjson icon.png .cache/stage/
mkdir -p .cache/stage/credits .cache/stage/licenses
cp CREDITS.md PROVENANCE.md .cache/stage/credits/
cp LICENSE .cache/stage/licenses/project-GPL-3.0.txt
cp NOTICE.md .cache/stage/licenses/project-NOTICE.md
"${J}jar" --create --file .cache/wither-desktop.jar -C .cache/stage .
if [ "${1:-universal}" != desktop ]; then
 "${J}java" -Xmx600m -cp "$VENDOR/r8.jar" com.android.tools.r8.D8 --release --min-api 21 --lib "$VENDOR/android.jar" --classpath "$GAME" --output .cache/dex .cache/wither-desktop.jar
 cp .cache/dex/classes*.dex .cache/stage/
fi
"${J}jar" --create --file release/Wither-Storm-v1.7.1.jar -C .cache/stage .
echo 'Built release/Wither-Storm-v1.7.1.jar'
