# Security Model

## Capability chain (spec §63)

```
AI model
  ↓
SafetyGuard          (hard boundaries — cannot be overridden)
  ↓
DomainPolicyStore    (per-domain user policy)
  ↓
Goal restrictions    (allowed/blocked actions)
  ↓
ToolRegistry         (permission-aware execution)
  ↓
PageBridge / JNI     (controlled Chromium surface)
```

The AI never receives OS-level access, raw WebContents handles, or the
ability to add new tools at runtime.

## Hard boundaries (`SafetyGuard`)

Always forbidden, regardless of policy or goal configuration:

- extracting cookies, passwords, API keys, keystores
- disabling the sandbox
- CAPTCHA bypass, anti-bot evasion, authentication bypass
- shell execution

## Prompt-injection defense (spec §40)

All webpage-derived text is wrapped in
`<<<UNTRUSTED_WEBPAGE_CONTENT_BEGIN/END>>>` blocks with an explicit data-not-
instructions note. Injection-pattern matches block the pending action and
record a `SecurityEvent`.

## Sensitive-data protection (spec §41)

`SensitiveDataFilter` redacts or blocks: API keys (OpenAI/Anthropic/Google/
GitHub/Slack patterns), passwords, cookies, session ids, PEM private keys,
and long base64-like blobs before anything is stored in memory, sent to a
provider, or written to the audit log.

## API key storage (spec §18)

Provider keys live in `SecureKeyStore`: AES-256-GCM via Android Keystore,
key material never in source, plaintext prefs, logs, or agent memory.

## Approvals (spec §26)

Risky actions (submit, upload, download, tab-close, coordinate clicks) and
any action whose domain policy says `approval_required` suspend the run with
status `WAITING_APPROVAL` until the user decides.

## Emergency stop (spec §29)

STOP ALL AGENTS immediately halts execution, preserves state for safe
resume, and is exposed prominently in the Agent Control Center.
