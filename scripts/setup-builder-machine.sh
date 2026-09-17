#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# One-time setup for a self-hosted Chromium builder (docs/RUNNER-SETUP.md).
# Targets Ubuntu 22.04+ x86_64 with at least: 16 cores, 64 GB RAM, 256 GB SSD.
set -euo pipefail

echo "==> System packages (Chromium Android build deps)"
sudo apt-get update
sudo apt-get install -y \
  git python3 python3-pip curl wget unzip xz-utils bzip2 ca-certificates \
  build-essential pkg-config ninja-build openjdk-17-jdk \
  lld clang-18 libglib2.0-dev libnss3-dev libatk1.0-dev libatk-bridge2.0-dev

echo "==> File watcher limits (Chromium builds hit inotify limits)"
echo fs.inotify.max_user_watches=524288 | sudo tee /etc/sysctl.d/60-chromium-build.conf
sudo sysctl --system >/dev/null

echo "==> Creating build user + runner workspace"
sudo useradd -m -s /bin/bash builder 2>/dev/null || true
sudo -u builder mkdir -p /home/builder/actions-runner /home/builder/chromium

echo "==> Next steps (manual, need a GitHub token interactively):"
echo "    1. Download the runner tarball from Repo → Settings → Actions → Runners"
echo "    2. ./config.sh --url https://github.com/dewrin/AgentBrowser --labels chromium-builder"
echo "    3. ./svc.sh install && ./svc.sh start"
echo "    4. As builder: bash scripts/sync-chromium.sh   (pre-warm the checkout)"
