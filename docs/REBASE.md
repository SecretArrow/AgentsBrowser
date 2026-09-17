# Upstream Rebase Process

Repeatable process for moving the fork to a new Chromium revision
(spec section 73).

## 1. Pick the target

```bash
cd chromium
git fetch origin
git checkout <new-android-release-branch>   # e.g. android-15.1.0_r1
gclient sync -D --with_branch_heads --with_tags
```

## 2. Re-apply the series

```bash
cd <repo>
git -C chromium checkout -b rebase-agent
bash scripts/apply-agent-layer.sh           # git am --3way, stops on conflict
```

On conflict:

```bash
cd chromium
# resolve, then:
git am --continue
# if a patch is obsolete upstream:
git am --skip
# to abort:
git am --abort
```

## 3. Validate

```bash
bash scripts/configure-gn.sh
autoninja -C out/AgentBrowser agent_browser chrome_public_apk
```

## 4. Regenerate the series

If files moved upstream, regenerate the affected patch instead of stacking
fixes:

```bash
cd chromium
git format-patch base --output-directory ../agent_browser/integration/patches/
```

Update `SERIES` to match the new file names and order.

## 5. Record the new base

Update `CHROMIUM_BASE` in `VERSION` and note the revision in the release
metadata (docs/VERSIONING.md).
