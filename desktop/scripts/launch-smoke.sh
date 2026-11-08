#!/bin/sh
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
#
# Headless launch check for the desktop app. Starts the given launcher under
# Xvfb with a throw-away preferences directory, requires it to be running
# after 10 seconds without an exception on stderr, then stops it.
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
  "$1" >"$2/stdout.log" 2>"$2/stderr.log" &
  pid=$!
  sleep 10
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    wait "$pid" 2>/dev/null || true
    exit 0
  fi
  wait "$pid" || echo "exited early with status $?" >&2
  exit 1
' sh "$app" "$work" || status=$?

sed 's/^/stdout: /' "$work/stdout.log"
sed 's/^/stderr: /' "$work/stderr.log"
if [ "$status" -ne 0 ]; then
  echo "FAIL: the app was not running after 10 s" >&2
  exit 1
fi
if grep -qi exception "$work/stderr.log"; then
  echo "FAIL: exception on stderr" >&2
  exit 1
fi
echo "OK: running after 10 s, no exception on stderr"
