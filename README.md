# Agent Browser — Chromium-Based Autonomous AI Browser

Agent Browser is a real Chromium fork for Android with a deeply integrated autonomous AI agent.
It is **not** a WebView wrapper, a simulator, or a chatbot bolted onto screenshots — it builds
against the Chromium source tree and drives the browser through a controlled, permission-gated
tool API layered on top of `WebContents`.

```
┌─────────────────────────────────┐
│  Chromium Browser               │
│  Blink • V8 • Network • Tabs    │
└───────────────┬─────────────────┘
        Agent Control Layer
┌───────────────┴─────────────────┐
│  AI Agent                       │
│  Understand → Plan → Act        │
│  Observe → Replan → Remember    │
└─────────────────────────────────┘
```

## Highlights

- **Real Chromium** — Blink, V8, Chromium networking, multi-process, sandbox, site isolation.
- **Autonomous goals** — persistent, scheduled, background agent tasks (`WorkManager` / `AlarmManager`).
- **Agent tools** — a controlled capability API: navigate, click, type, extract, inspect DOM /
  accessibility tree, screenshots, downloads, and more (see `agent_browser/ai/tools/`).
- **Layered page understanding** — DOM → accessibility → visible text → screenshot → vision fallback.
- **Provider abstraction** — OpenAI / Anthropic / Gemini / OpenRouter / Ollama / custom endpoints,
  with per-task model routing (planner, actions, vision, summarizer, background monitor).
- **Security-first** — permission manager, domain policy, approval gates, prompt-injection defense,
  sensitive-data minimization, loop detection, audit log, emergency stop.
- **Human oversight** — Agent Control Center UI, approvals, pause/resume/stop, full action history.

## Repository layout

```
agent_browser/          Modular Agent Browser layer (kept separate for easy rebasing)
  ai/                   Providers, routing, prompt guard, data minimization
  agent/                Runtime, planner, executor, scheduler, state, memory
  automation/           Tool implementations bridging to WebContents
  browser/              Browser-side glue and JNI
  memory/               Site / goal / run memory stores
  scheduler/            Trigger evaluation and scheduling
  security/             Permissions, safety guard, resource limits
  ui/                   Android UI: panel, control center, approvals
  integration/          Chromium integration hooks and patch series
chromium/               Chromium checkout (created by scripts/sync-chromium.sh)
scripts/                Sync / patch / build helper scripts
.github/workflows/      CI/CD (fast CI, Chromium build, releases, auto-fix)
docs/                   Architecture, integration, CI, runner setup, rebase guide
```

## Building

Agent Browser builds **on GitHub Actions** — never locally. Two tracks:

| Track | Workflow | Runner | Notes |
|---|---|---|---|
| Fast CI | `.github/workflows/ci.yml` | `ubuntu-latest` | Sanity/structure checks, seconds-scale |
| Full build | `.github/workflows/chromium-build.yml` | self-hosted (≥16 cores, 256 GB disk, 64 GB RAM) | Chromium Android build |

```bash
# One-time machine prep for the self-hosted runner (see docs/RUNNER-SETUP.md)
bash scripts/setup-builder-machine.sh
```

## Releases

`release.yml` produces Debug APK, Release APK, and Release AAB, signed with CI secrets
(`AB_KEYSTORE_BASE64`, `AB_KEYSTORE_PASSWORD`, `AB_KEY_ALIAS`, `AB_KEY_PASSWORD`). No signing
material is ever committed.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Chromium integration & patch series](docs/CHROMIUM-INTEGRATION.md)
- [CI/CD](docs/CI-CD.md) · [Runner setup](docs/RUNNER-SETUP.md)
- [Upstream rebase process](docs/REBASE.md)
- [Security model](docs/SECURITY.md)
- [Versioning](docs/VERSIONING.md)

## License

Apache-2.0 (inherited from Chromium). See [LICENSE](LICENSE).
