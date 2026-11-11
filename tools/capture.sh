#!/bin/bash
# Screenshots of the demo nook on a phone (1080x2340), in English and Russian: docs/screenshots/<lang>/*.jpg,
# the full-size PNGs in app/build/shots/<lang>/. Builds app.rustle.shots (-Pdemo -Pshots: still, dark, no
# system bars), wipes only that app's data, and seeds it afresh.
#   ANDROID_SERIAL=<phone> bash tools/capture.sh
# Keep the phone on charge (he sits by a lit stove) and lying still (or he gets dizzy). Every tap checks
# that the screenshot app is in front and no call is going on; it stops otherwise.
set -e
cd "$(dirname "$0")/.."
export MSYS_NO_PATHCONV=1
: "${JAVA_HOME:=/c/Program Files/Android/Android Studio/jbr}"; export JAVA_HOME
command -v adb >/dev/null || PATH="$PATH:$LOCALAPPDATA/Android/Sdk/platform-tools"
PKG=app.rustle.shots

guard() {
  f=$(adb shell dumpsys window | grep mCurrentFocus)
  c=$(adb shell dumpsys telephony.registry | grep -m1 mCallState | tr -d ' \r')
  case "$f" in *$PKG*) ;; *) echo "ABORT focus $f"; exit 1;; esac
  [ "$c" = "mCallState=0" ] || { echo "ABORT call $c"; exit 1; }
}
tap() { guard; adb shell input tap "$1" "$2"; sleep "${3:-2}"; }
back() { guard; adb shell input keyevent 4; sleep 1.5; }
shot() { guard; adb exec-out screencap -p > "app/build/shots/$LANG_/$1.png"; }
build() { ./gradlew :app:assembleDebug -Pdemo -Pshots -Plang=$LANG_ "$@" -q; adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null; }
start() { adb shell am start -n $PKG/app.rustle.MainActivity >/dev/null; sleep "${1:-6}"; }

HOME_=148; TRIPS=408; PANTRY=668; WORKSHOP=932; TABS=2280

for LANG_ in ${LANGS:-en ru}; do
  mkdir -p "app/build/shots/$LANG_" "docs/screenshots/$LANG_"
  # The evening: the bed tile and a dark window.
  build -Pevening
  adb shell pm clear $PKG >/dev/null
  start 30
  shot 2-home-evening
  # The rest at dusk.
  build
  adb shell am force-stop $PKG; start 8
  shot 1-home
  tap 280 1540; shot 8-needs; back
  tap 135 125; shot 6-profile
  tap 540 1152; shot 7-feats; back
  tap $TRIPS $TABS; shot 3-trips
  tap $PANTRY $TABS; shot 4-shelf
  tap $WORKSHOP $TABS; shot 5-workshop
  tap $HOME_ $TABS
done
python - <<'PY'
from PIL import Image
import glob, os
for p in glob.glob("app/build/shots/*/*.png"):
    lang = p.replace("\\", "/").split("/")[-2]
    out = f"docs/screenshots/{lang}/" + os.path.basename(p).replace(".png", ".jpg")
    Image.open(p).convert("RGB").resize((720, 1560), Image.LANCZOS).save(out, quality=88)
print("ok")
PY
