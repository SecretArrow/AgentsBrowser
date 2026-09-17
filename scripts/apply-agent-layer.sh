#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# Copies the agent_browser layer into the Chromium tree and applies the patch
# series (spec section 3, docs/CHROMIUM-INTEGRATION.md).
set -euo pipefail

CHROMIUM_DIR="${CHROMIUM_DIR:-chromium}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

[ -d "$CHROMIUM_DIR" ] || { echo "ERROR: $CHROMIUM_DIR not found. Run scripts/sync-chromium.sh first."; exit 1; }

echo "==> Copying agent_browser layer into the Chromium tree"
cp -r "$REPO_ROOT/agent_browser" "$CHROMIUM_DIR/agent_browser"

echo "==> Applying patch series"
cd "$CHROMIUM_DIR"
while IFS= read -r patch; do
  case "$patch" in ''|'#'*) continue ;; esac
  echo "    git am $patch"
  if ! git am --3way "agent_browser/integration/patches/$patch"; then
    echo "ERROR: patch failed: $patch (resolve, then 'git am --continue')"
    exit 1
  fi
done < "$REPO_ROOT/agent_browser/integration/patches/SERIES"

echo "==> Writing .gclient include so GN sees agent_browser"
gclient root >/dev/null

echo "Done. Next: scripts/build-chromium-android.sh"
