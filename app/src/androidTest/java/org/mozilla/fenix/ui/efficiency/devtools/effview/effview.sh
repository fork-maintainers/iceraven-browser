#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# effview — one command to see the selectors on the current screen.
#
# Two modes:
#   Default (trigger):  requires InteractiveInspectTest#inspect running on the device. Drops the trigger
#                       file, waits for the fresh dump, pulls it, renders the overlay, opens it.
#   --no-trigger:       just pulls whatever dump is already on the device (e.g. after dumpOnce) and renders.
#
# Typical use:
#   1) run the interactive test (keeps the app alive ~30 min, no pausing):
#        ./mach gradle fenix:connectedDebugAndroidTest \
#          -Pandroid.testInstrumentationRunnerArguments.class=org.mozilla.fenix.ui.efficiency.devtools.InteractiveInspectTest#inspect
#   2) click to any screen in the app
#   3) ./effview.sh          # → triggers a dump, pulls it, opens screen.html with every element boxed + mapped
#      (repeat step 2–3 for each screen; no rebuild, no breakpoint)
#
# Env overrides: PKG (app id), OUT (output dir), ADB.
set -euo pipefail
PKG="${PKG:-org.mozilla.fenix.debug}"
OUT="${OUT:-./eff-view}"
ADB="${ADB:-adb}"
HERE="$(cd "$(dirname "$0")" && pwd)"
TRIGGER=1
[ "${1:-}" = "--no-trigger" ] && TRIGGER=0
mkdir -p "$OUT"

ra() { "$ADB" exec-out run-as "$PKG" "$@"; }   # run a command as the (debuggable) app, binary-safe
JSON="$OUT/eff-screendump.json"
PNG="$OUT/eff-screendump.png"
# pull the device JSON into $JSON; "valid" = non-empty AND looks like JSON (adb exit codes are unreliable,
# so we judge by CONTENT, not by adb's return value).
pull_json() { ra cat files/eff-screendump.json > "$JSON" 2>/dev/null || true; }
valid_json() { [ -s "$JSON" ] && head -c1 "$JSON" 2>/dev/null | grep -q '{'; }
jbytes() { [ -f "$JSON" ] && wc -c < "$JSON" | tr -d ' ' || echo 0; }

if [ "$TRIGGER" = 1 ]; then
  echo "• triggering a fresh dump (InteractiveInspectTest#inspect must be running)…"
  ra rm -f files/eff-screendump.json 2>/dev/null || true
  "$ADB" exec-out run-as "$PKG" touch files/eff-scan 2>/dev/null || true
  ok=0
  for _ in $(seq 1 40); do            # up to ~20s; dumpAll writes the JSON last
    pull_json
    if valid_json; then ok=1; break; fi
    sleep 0.5
  done
  if [ "$ok" != 1 ]; then
    echo "✗ no valid dump appeared (got $(jbytes) bytes). Likely one of:"
    echo "    • InteractiveInspectTest#inspect is NOT running on the device right now — start it, then re-run."
    echo "    • PKG doesn't match the app under test (current: $PKG). Check the 'run-as' line in:"
    echo "        adb logcat -d -s EffScreenDump:I"
    echo "    • For a one-shot after #dumpOnce, use:  $0 --no-trigger"
    exit 1
  fi
else
  echo "• pulling the existing dump (no trigger)…"
  pull_json
  if ! valid_json; then
    echo "✗ no valid eff-screendump.json on device (got $(jbytes) bytes)."
    echo "    Run InteractiveInspectTest#dumpOnce first, or start #inspect and drop the --no-trigger flag."
    echo "    Also confirm PKG=$PKG matches the 'run-as' line in: adb logcat -d -s EffScreenDump:I"
    exit 1
  fi
fi

echo "• got $(jbytes)-byte dump; pulling screenshot…"
ra cat files/eff-screendump.png > "$PNG" 2>/dev/null || true

echo "• rendering…"
if [ -s "$PNG" ]; then
  python3 "$HERE/effviewdump-v2.py" "$JSON" "$PNG" "$OUT/screen.html"
else
  echo "  (no screenshot pulled — cannot render without screenshot)"
  exit 1
fi

if command -v open >/dev/null 2>&1; then open "$OUT/screen.html"
elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$OUT/screen.html"
else echo "open this in a browser: $OUT/screen.html"; fi
