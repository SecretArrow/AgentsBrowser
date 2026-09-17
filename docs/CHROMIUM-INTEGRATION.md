# Chromium Integration

## Layout

```
chromium/                ← fetched by scripts/sync-chromium.sh (NOT committed)
agent_browser/           ← copied into the tree by scripts/apply-agent-layer.sh
agent_browser/integration/patches/  ← the only Chromium-file modifications
```

## Patch series

Ordered by `agent_browser/integration/patches/SERIES`:

1. `0001-add-agent_browser-layer-to-gn.patch` — register `//agent_browser` group.
2. `0002-chrome-agent-runtime-integration.patch` — runtime init at startup.
3. `0003-agent-newtab-and-control-intents.patch` — NTP + control intents.

Rules:

- Patches must stay small and mechanical; feature code belongs in
  `agent_browser/`.
- Every patch carries the standard Apache header and a `[PATCH n/3]` subject.
- After each upstream rebase, regenerate the series (see docs/REBASE.md).

## GN targets

| Target | Purpose |
|---|---|
| `//agent_browser` | aggregate group |
| `//agent_browser/ai:agent_browser_ai` | providers, guards |
| `//agent_browser/agent:agent_browser_agent` | runtime, goals, state |
| `//agent_browser/agent:agent_browser_agent_tests` | unit tests (JUnit) |
| `//agent_browser/browser:agent_browser_browser` | host + JNI glue |

## Build pipeline

```
scripts/sync-chromium.sh       # depot_tools + fetch android + gclient sync
scripts/apply-agent-layer.sh   # copy layer + git am patch series
scripts/configure-gn.sh        # gn gen out/AgentBrowser (arm64, release)
scripts/build-chromium-android.sh  # autoninja agent_browser chrome_public_apk
```

Outputs land in `chromium/out/AgentBrowser/apks/`.
