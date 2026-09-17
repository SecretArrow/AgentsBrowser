# CI/CD

## Workflows

| Workflow | Runner | Trigger | Purpose |
|---|---|---|---|
| `ci-fast` | `ubuntu-latest` | push/PR | seconds-scale structure + syntax validation |
| `chromium-build` | `[self-hosted, chromium-builder]` | push to main (paths) / manual | full APK/AAB build |
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

`chromium-build` and `release` require a self-hosted runner labeled
`chromium-builder` — see docs/RUNNER-SETUP.md.
