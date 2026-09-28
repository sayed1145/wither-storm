#!/usr/bin/env bash
# Each mode is a SEPARATE bounded execution. Never starts two graphical clients together.
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD";VENDOR="${VENDOR:-$HOME/.cache/wd-vendor}"
if [ -n "${JAVA_HOME:-}" ]; then J="$JAVA_HOME/bin/";elif [ -x "$VENDOR/jdk17/bin/java" ];then J="$VENDOR/jdk17/bin/";else J="";fi
GAME="${MINDUSTRY_JAR:-$VENDOR/Mindustry.jar}";SERVER="${SERVER_JAR:-$VENDOR/server-release.jar}"
MODE="${1:-headless}"
[ -f release/Wither-Storm-v1.7.1.jar ] || { echo 'Extract the full package or build the JAR first.';exit 1; }
mkdir -p .cache/testbin preview
"${J}javac" --release 17 -encoding UTF-8 -cp "release/Wither-Storm-v1.7.1.jar:$GAME:$SERVER" -d .cache/testbin tools/wstest/*.java
if [ "$MODE" = headless ];then
 mkdir -p .cache/server/config/mods
 rm -f .cache/server/config/mods/Wither-Storm-v*.jar
 cp release/Wither-Storm-v1.7.1.jar .cache/server/config/mods/
 (cd .cache/server;timeout -k 2s 40s "${J}java" -Xmx550m -XX:ActiveProcessorCount=2 -Dws.tools="$ROOT/.cache/testbin" -Dws.result="$ROOT/preview/headless-result.json" -cp "$ROOT/.cache/testbin:$SERVER" wstest.ServerBootstrap > server.log 2>&1)
 tail -5 .cache/server/server.log
elif [ "$MODE" = layout ] || [ "$MODE" = layout2 ] || [ "$MODE" = pixel ] || [ "$MODE" = pixel2 ] || [ "$MODE" = aim ] || [ "$MODE" = aim2 ] || [ "$MODE" = control ] || [ "$MODE" = control2 ] || [ "$MODE" = client ] || [ "$MODE" = gl2 ] || [ "$MODE" = review ] || [ "$MODE" = animation ] || [ "$MODE" = animation2 ] || [ "$MODE" = fracture ] || [ "$MODE" = orbit ] || [ "$MODE" = orbit2 ];then
 DIR="$MODE";[ "$MODE" != client ] || DIR=release
 mkdir -p .cache/client/mods ".cache/frames/$DIR"
 rm -f .cache/client/mods/Wither-Storm-v*.jar
 cp release/Wither-Storm-v1.7.1.jar .cache/client/mods/
 printf 'pcm.!default { type null }\n' > .cache/asound.conf
 flags=();screen='1120x700x24'
 [ "$MODE" != gl2 ] || flags=(-Dws.gl2=true -Dws.quick=true)
 if [ "$MODE" = layout ];then flags=(-Dws.layout=true);fi
 if [ "$MODE" = layout2 ];then flags=(-Dws.layout=true -Dws.gl2=true);fi
 if [ "$MODE" = pixel ];then flags=(-Dws.pixel=true);fi
 if [ "$MODE" = pixel2 ];then flags=(-Dws.pixel=true -Dws.gl2=true);fi
 if [ "$MODE" = aim ];then flags=(-Dws.aim=true);fi
 if [ "$MODE" = aim2 ];then flags=(-Dws.aim=true -Dws.gl2=true -Dws.quick=true);fi
 if [ "$MODE" = control ];then flags=(-Dws.control=true);fi
 if [ "$MODE" = control2 ];then flags=(-Dws.control=true -Dws.gl2=true -Dws.quick=true);fi
 if [ "$MODE" = review ];then flags=(-Dws.review=true);fi
 if [ "$MODE" = orbit ];then flags=(-Dws.review=true -Dws.orbit=true);fi
 if [ "$MODE" = orbit2 ];then flags=(-Dws.review=true -Dws.orbit=true -Dws.gl2=true -Dws.quick=true);fi
 if [ "$MODE" = fracture ];then flags=(-Dws.fracture=true);fi
 if [ "$MODE" = animation ];then flags=(-Dws.review=true -Dws.animation=true);fi
 if [ "$MODE" = animation2 ];then flags=(-Dws.review=true -Dws.animation=true -Dws.gl2=true -Dws.quick=true);fi
 timeout -k 2s 120s xvfb-run -a -s "-screen 0 $screen" env LIBGL_ALWAYS_SOFTWARE=1 LP_NUM_THREADS=2 ALSA_CONFIG_PATH="$ROOT/.cache/asound.conf" SDL_AUDIODRIVER=dummy ALSOFT_DRIVERS=null \
  "${J}java" -Xmx700m -XX:MaxDirectMemorySize=160m -XX:ActiveProcessorCount=2 \
  -Dmindustry.data.dir="$ROOT/.cache/client" -Dws.tools="$ROOT/.cache/testbin" -Dws.capture="$ROOT/.cache/frames/$DIR" -Dws.audio=false -Dws.trackcamera=true "${flags[@]}" \
  -cp "$ROOT/.cache/testbin:$GAME" wstest.ClientBootstrap > ".cache/client-$MODE.log" 2>&1
 tail -5 ".cache/client-$MODE.log"
 if [ "$MODE" = client ];then cp .cache/frames/release/client-result.json preview/client-gl3.json;fi
 if [ "$MODE" = gl2 ];then cp .cache/frames/gl2/client-result.json preview/client-gl2.json;fi
 
else
 echo 'Use one mode: headless | client | gl2 | review';exit 2
fi
