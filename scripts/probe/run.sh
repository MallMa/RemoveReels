#!/bin/sh
# Runs the debug build's cover probe (CoverProbe) while adb scrolls Instagram, for the H0 spike.
#
# Usage: scripts/probe/run.sh NAME TRACK DRAW MEASURE SWIPES [OUT]
#   TRACK: none|events|frames|poll   DRAW: canvas|pass|windows   MEASURE: true|false   SWIPES: explore|profile
#   COVER=grid covers a whole grid instead of each Reels tile.
# Saves the probe's CSV, Instagram's frame statistics and CPU time to OUT (default: build/probe). Instagram must be in
# front; nothing from the screen is recorded.
adb=${ADB:-adb}
name=$1; track=$2; draw=$3; measure=$4; swipes=$5; out=${6:-build/probe}
mkdir -p "$out"
focus=$($adb shell dumpsys window | grep mCurrentFocus)
case "$focus" in *com.instagram.android*) ;; *) echo "Instagram is not in front: $focus"; exit 1;; esac
ig=$($adb shell pidof com.instagram.android | awk '{print $1}')
rr=$($adb shell pidof io.github.mallma.removereels)
ticks() { $adb shell cat "/proc/$1/stat" | awk '{print $14+$15}'; }
$adb shell dumpsys gfxinfo com.instagram.android reset >/dev/null
$adb shell dumpsys gfxinfo io.github.mallma.removereels reset >/dev/null
ig0=$(ticks "$ig"); rr0=$(ticks "$rr")
if [ "$track" != none ]; then
  $adb shell am broadcast -a io.github.mallma.removereels.PROBE --es track "$track" --es draw "$draw" \
    --ez measure "$measure" --es cover "${COVER:-tiles}" --es name "$name" >/dev/null
fi
sleep 1
if [ "$swipes" = explore ]; then
  set -- "540 1700 540 700 150" "540 700 540 1700 150" "540 1700 540 700 150" "540 700 540 1700 150" \
    "540 1700 540 1000 1200" "540 1000 540 1700 1200"
else
  set -- "540 1900 540 800 250" "540 800 540 1900 250" "540 1900 540 1200 1200" "540 1200 540 1900 1200"
fi
for swipe in "$@"; do
  # shellcheck disable=SC2086 # The swipe's coordinates and duration are separate arguments.
  $adb shell input swipe $swipe
  sleep 2
done
if [ "$track" != none ]; then $adb shell am broadcast -a io.github.mallma.removereels.PROBE --es track off >/dev/null; sleep 1; fi
ig1=$(ticks "$ig"); rr1=$(ticks "$rr")
echo "$name cpu_ticks instagram=$((ig1 - ig0)) removereels=$((rr1 - rr0))" | tee "$out/$name.cpu"
$adb shell dumpsys gfxinfo com.instagram.android | grep -E "Total frames rendered|Janky frames|percentile" | head -6 | tee "$out/$name.gfx"
if [ "$track" != none ]; then
  $adb exec-out run-as io.github.mallma.removereels cat "files/probe/$name.csv" > "$out/$name.csv"
  python "$(dirname "$0")/summary.py" "$out/$name.csv"
fi
