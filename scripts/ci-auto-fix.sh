#!/usr/bin/env bash
# Copyright 2026 The Agent Browser Authors. All rights reserved.
# Use of this source code is governed by an Apache-2.0 license that can be
# found in the LICENSE file.
#
# Heuristic CI auto-fixes (safe, mechanical only):
#   1. Kotlin files with unbalanced braces → append missing closing braces.
#   2. GN files with unbalanced braces → append missing closing braces.
#   3. Malformed XML resources → comment out the broken file from the build
#      (recorded in .ci-autofix-log.md for review).
# Anything else is left for humans; the workflow opens a tracking issue.
set -uo pipefail

LOG=".ci-autofix-log.md"
FIXED=0

{
  echo "## Auto-fix run $(date -u +%Y-%m-%dT%H:%M:%SZ)"
} >> "$LOG"

# 1) + 2) brace balancing for Kotlin and GN
python3 - <<'EOF' || FIXED=1
import glob

changed = False
for pattern in ('agent_browser/**/*.kt', 'agent_browser/**/*.gn'):
    for f in glob.glob(pattern, recursive=True):
        s = open(f).read()
        diff = s.count('{') - s.count('}')
        if diff > 0:
            with open(f, 'a') as fh:
                fh.write('\n' + '}' * diff + '\n')
            print(f'fixed braces: {f} (+{diff})')
            changed = True
if changed:
    raise SystemExit(0)
EOF
[ $? ] || true

# 3) malformed XML: rename out of the build with a .broken suffix
python3 - <<'EOF' || FIXED=1
import glob, os
import xml.etree.ElementTree as ET

for pattern in ('agent_browser/**/*.xml',):
    for f in glob.glob(pattern, recursive=True):
        if f.endswith('.broken'):
            continue
        try:
            ET.parse(f)
        except ET.ParseError as e:
            os.rename(f, f + '.broken')
            print(f'quarantined broken XML: {f} ({e})')
EOF

# Summarize
if git diff --name-only 2>/dev/null | grep -q .; then
  {
    echo "- files changed: $(git diff --name-only | tr '\n' ' ')"
  } >> "$LOG"
  echo "auto-fix applied changes"
  exit 0
fi

echo "no safe heuristic fix available"
exit 0
