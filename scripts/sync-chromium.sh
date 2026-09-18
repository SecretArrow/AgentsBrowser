#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# Fetches Chromium for Android into ./chromium (spec section 2).
# Requires: git, python3, curl. ~25 GB at fetch, ~100 GB after hooks+sync.
set -euo pipefail

CHROMIUM_DIR="${CHROMIUM_DIR:-chromium}"
BRANCH="${CHROMIUM_BRANCH:-android-15.0.0_r1}"

echo "==> Installing depot_tools"
# DEPOT_TOOLS_DIR lets CI jobs share one checkout across jobs (the clone is
# ~1 GB); defaults to ./depot_tools next to the chromium dir.
DEPOT_TOOLS_DIR="${DEPOT_TOOLS_DIR:-$PWD/depot_tools}"
if [ ! -d "$DEPOT_TOOLS_DIR/.git" ]; then
  git clone --depth=1 https://chromium.googlesource.com/chromium/tools/depot_tools.git \
    "$DEPOT_TOOLS_DIR"
fi
export PATH="$DEPOT_TOOLS_DIR:$PATH"

echo "==> Fetching Chromium (this is the long step; ~25 GB)"
if [ ! -d "$CHROMIUM_DIR/.git" ]; then
  mkdir -p "$CHROMIUM_DIR"
  fetch --nohooks android
fi

cd "$CHROMIUM_DIR"
git checkout "$BRANCH" 2>/dev/null || echo "WARN: branch $BRANCH not found; staying on default"

echo "==> Syncing dependencies + hooks"
gclient sync -D --with_branch_heads --with_tags

echo "==> Installing Android SDK bits via Chromium tooling"
python3 build/install-build-deps-android.sh 2>/dev/null || \
  echo "NOTE: run build/install-build-deps.sh manually if this failed"

echo "Done. Next: scripts/apply-agent-layer.sh && scripts/build-chromium-android.sh"
