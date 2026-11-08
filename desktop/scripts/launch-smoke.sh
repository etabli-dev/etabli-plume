#!/bin/sh
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
#
# Headless launch check for the desktop app. Starts the given launcher under
# Xvfb with a throw-away preferences directory, waits for its own mapped window
# and checks stderr, then stops it. Needs Xvfb, xdotool and x11-utils.
#
# Usage (from desktop/): scripts/launch-smoke.sh [path/to/bin/etabli-plume]
# Default: the app image from `./gradlew createDistributable`.
set -eu

app="${1:-build/compose/binaries/main/app/etabli-plume/bin/etabli-plume}"
[ -x "$app" ] || { echo "launcher not found: $app" >&2; exit 2; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
export ETABLI_PLUME_CONFIG_DIR="$work/config"

status=0
xvfb-run -a -s "-screen 0 1280x800x24" sh -c '
  # The isolated virtual display has no GPU; use the supported Skiko software renderer.
  SKIKO_RENDER_API=SOFTWARE "$1" >"$2/stdout.log" 2>"$2/stderr.log" &
  pid=$!
  trap "kill $pid 2>/dev/null || true; wait $pid 2>/dev/null || true" EXIT
  attempts=0
  while [ "$attempts" -lt 30 ]; do
    if ! kill -0 "$pid" 2>/dev/null; then
      wait "$pid" || echo "exited early with status $?" >&2
      exit 1
    fi
    # Match only this process on our private X display, independent of title encoding.
    window_id=$(xdotool search --onlyvisible --pid "$pid" 2>/dev/null | head -n 1)
    if [ -n "$window_id" ]; then
      xprop -id "$window_id" WM_CLASS >"$2/wm-class.log" || exit 1
      sleep 2
      kill -0 "$pid" 2>/dev/null || exit 1
      exit 0
    fi
    attempts=$((attempts + 1))
    sleep 1
  done
  xwininfo -root -tree >&2
  echo "No visible window owned by process $pid within 30 seconds" >&2
  exit 1
' sh "$app" "$work" || status=$?

sed 's/^/stdout: /' "$work/stdout.log"
sed 's/^/stderr: /' "$work/stderr.log"
if [ "$status" -ne 0 ]; then
  echo "FAIL: the app did not map and retain its own window" >&2
  exit 1
fi
if grep -qi exception "$work/stderr.log"; then
  echo "FAIL: exception on stderr" >&2
  exit 1
fi
python3 - "$work/wm-class.log" <<'PYCLASS'
import pathlib, re, sys
raw = pathlib.Path(sys.argv[1]).read_text()
values = re.findall(r'"([^"\n]+)"', raw)
if len(values) != 2 or not re.fullmatch(r"[A-Za-z0-9_.$-]+", values[1]):
    raise SystemExit("FAIL: no usable WM_CLASS from the running app: " + raw)
output = pathlib.Path("build/generated/linux-wm-class.txt")
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(values[1] + "\n")
print("Measured WM_CLASS: " + values[1])
PYCLASS
echo "OK: own window mapped, no exception on stderr, WM_CLASS measured"
