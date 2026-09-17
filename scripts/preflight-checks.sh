#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# Builder preflight: verifies the machine can actually build Chromium Android.
# Skippable with PREFLIGHT_SKIP=1 for dry runs on small machines.
set -uo pipefail

FAIL=0

cores="$(nproc 2>/dev/null || echo 0)"
ram_gb="$(free -g 2>/dev/null | awk '/^Mem:/{print $2}')"
disk_gb="$(df --output=avail -BG . 2>/dev/null | tail -1 | tr -dc '0-9')"

echo "cores=$cores ram_gb=${ram_gb:-?} disk_avail_gb=${disk_gb:-?}"

if [ "${PREFLIGHT_SKIP:-0}" = "1" ]; then
  echo "PREFLIGHT_SKIP=1 — skipping hard requirements"
  exit 0
fi

[ "${cores:-0}" -ge 8 ] || { echo "ERROR: need >=8 cores"; FAIL=1; }
[ "${disk_gb:-0}" -ge 200 ] || { echo "ERROR: need >=200 GB free disk"; FAIL=1; }
[ "${ram_gb:-0}" -ge 32 ] || { echo "ERROR: need >=32 GB RAM"; FAIL=1; }

for tool in git python3 curl; do
  command -v "$tool" >/dev/null || { echo "ERROR: missing tool: $tool"; FAIL=1; }
done

exit "$FAIL"
