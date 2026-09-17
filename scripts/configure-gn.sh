#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# Standalone GN configure step (idempotent; build script re-runs it safely).
set -euo pipefail

CHROMIUM_DIR="${CHROMIUM_DIR:-chromium}"
BUILD_DIR="${BUILD_DIR:-out/AgentBrowser}"
TARGET_CPU="${TARGET_CPU:-arm64}"

export PATH="$PWD/depot_tools:$PATH"
cd "$CHROMIUM_DIR"

echo "==> gn gen $BUILD_DIR"
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

echo "==> GN args summary"
gn args --list="$BUILD_DIR" 2>/dev/null | head -20 || true
