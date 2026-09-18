# CI/CD

## Workflows

| Workflow | Runner | Trigger | Purpose |
|---|---|---|---|
| `ci-fast` | `ubuntu-latest` | push/PR | seconds-scale structure + syntax validation |
| `chromium-build` | self-hosted (`chromium-builder`) **or** hosted `ubuntu-latest-16-cores` | tag `v*` / manual | full fork APK build (~4–6 h) |
| `auto-fix` | `ubuntu-latest` | any workflow failure | heuristic fixes, push, retry |
| `release` | `[self-hosted, chromium-builder]` | tags `v*` | signed APK + AAB + GitHub release |

## Fast workflow design

`ci-fast` avoids any Chromium checkout: it validates repo invariants, YAML,
GN brace balance, Kotlin brace balance, and the patch-series manifest in
under a minute. It is the feedback loop for everyday commits.

## Auto-fix loop

1. `ci-fast` or `chromium-build` fails.
2. `auto-fix` collects failed job names.
3. `scripts/ci-auto-fix.sh` applies only safe mechanical fixes:
   - append missing closing braces in Kotlin/GN files,
   - quarantine malformed XML resources as `*.broken`.
4. If anything changed: commit `ci: auto-fix heuristics … [skip ci]`, push to
   the failing branch, and re-run only the failed jobs.
5. If nothing changed: open a tracking issue for a human.

The token used for pushes is the workflow-scoped `GITHUB_TOKEN`; secrets are
never echoed or committed.

## Secrets

| Secret | Used by | Meaning |
|---|---|---|
| `AB_KEYSTORE_BASE64` | release | base64 keystore |
| `AB_KEYSTORE_PASSWORD` | release | keystore password |
| `AB_KEY_ALIAS` | release | signing key alias |
| `AB_KEY_PASSWORD` | release | key password |

## Required runner label

`chromium-build` runs on either lane (dispatch input `runner`):

- **self-hosted** — free, your own machine labeled `chromium-builder`
  (docs/RUNNER-SETUP.md). Default lane used by the autopilot.
- **hosted16** — GitHub larger runner `ubuntu-latest-16-cores`
  (16 vCPU / 64 GB RAM / 256 GB SSD). Requires billing enabled on the
  account; without it the run stays `pending` until cancelled.

`release` (full-fork signing lane) still requires the self-hosted
`chromium-builder` runner — see docs/RUNNER-SETUP.md.
