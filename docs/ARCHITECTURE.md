# Architecture

Agent Browser is a Chromium fork for Android with a modular autonomous agent
layer. All agent code lives under `agent_browser/` so upstream rebases touch
the smallest possible surface (spec section 3).

```
┌────────────────────────────────────────────┐
│ Chromium (Blink, V8, net, sandbox, tabs)   │
└───────────────┬────────────────────────────┘
        Agent Control Layer
┌───────────────┴────────────────────────────┐
│ PermissionManager → ToolRegistry → PageBridge
│        → WebContents / Tabs / Navigation   │
└───────────────┬────────────────────────────┘
┌───────────────┴────────────────────────────┐
│ AgentRuntime (observe → plan → act loop)   │
│ GoalManager · TriggerManager · Scheduler   │
│ MemoryManager · StateManager · AuditLogger │
│ ApprovalManager · SafetyGuard · CostGuard  │
└────────────────────────────────────────────┘
```

## Modules

| Module | Contents |
|---|---|
| `agent_browser/ai` | Provider abstraction (OpenAI-compatible, Anthropic, Gemini, OpenRouter, Ollama), model routing, prompt-injection guard, data minimization, cost guard, research agent |
| `agent_browser/agent` | Runtime loop, goals, triggers, state, SQLite schema + migrations, audit log, approvals, retry, loop detection |
| `agent_browser/automation` | Agent tools (spec §9), semantic element model, page inspector, PageBridge interface |
| `agent_browser/browser` | BrowserHost, JNI glue into `content/`, new-tab branding |
| `agent_browser/memory` | Layered memory: global, goal, site, run, temp |
| `agent_browser/scheduler` | WorkManager scheduling, goal worker |
| `agent_browser/security` | Domain policy, safety guard, resource limits, secure key store, notifications |
| `agent_browser/ui` | AI panel, Control Center, approval dialogs |

## Trust priority (spec §40)

```
System Policy → User Instruction → Agent Policy → Tool Results → Webpage Content
```

Webpage content is always wrapped as untrusted data (see
`PromptGuard.wrapUntrustedWebContent`) and can never override policy.

## Data flow for an autonomous run

1. Trigger fires (time/content/browser/state) → WorkManager starts `AgentGoalWorker`.
2. `AgentRuntime` restores persisted state, re-observes the browser, validates.
3. `PageInspector` builds layered observations (DOM → a11y → text → screenshot → vision).
4. Planner model proposes one action as JSON.
5. `DomainPolicyStore` + `SafetyGuard` + goal restrictions gate the action; risky
   actions suspend the run for human approval.
6. `ToolRegistry` executes through `PageBridge` into Chromium.
7. Result is observed, loop detection runs, state persists, audit log appends.

## Chromium integration points

- `chrome/android` startup hook initializes the runtime (patch 0002).
- `content/public/browser` WebContents accessed only via JNI bridge
  (`agent_browser/browser/src/main/jni`).
- New-tab page and Control Center are branded Agent Browser surfaces (patch 0003).
