#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# GN configure + incremental Android build producing APK/AAB
# (docs/CHROMIUM-INTEGRATION.md). Run on a builder machine or self-hosted
# runner; see scripts/setup-builder-machine.sh.
set -euo pipefail

CHROMIUM_DIR="${CHROMIUM_DIR:-chromium}"
BUILD_DIR="${BUILD_DIR:-out/AgentBrowser}"
TARGET_CPU="${TARGET_CPU:-arm64}"

export PATH="${DEPOT_TOOLS_DIR:-$PWD/depot_tools}:$PATH"
cd "$CHROMIUM_DIR"

echo "==> GN configure ($BUILD_DIR, target_cpu=$TARGET_CPU)"
gn gen "$BUILD_DIR" --args="\
target_os = \"android\" \
target_cpu = \"$TARGET_CPU\" \
is_debug = false \
is_component_build = false \
v8_symbol_level = 0 \
symbol_level = 0 \
blink_symbol_level = 0 \
enable_nacl = false \
dcheck_always_on = false \
"

echo "==> Building public browser target"
autoninja -C "$BUILD_DIR" agent_browser chrome_public_apk

echo "==> Outputs"
ls -la "$BUILD_DIR/apks/" || true

echo "Done. Artifacts: $CHROMIUM_DIR/$BUILD_DIR/apks/"
