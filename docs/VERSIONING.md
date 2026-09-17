# Versioning

Every release records (spec section 72):

```
Agent Browser 1.0.0
Chromium <revision>
Agent Patch <revision>
Build <number>
```

- `AGENT_BROWSER_VERSION` — product version, semver, from `VERSION`.
- `CHROMIUM_REVISION` — stamped by CI from the Chromium checkout.
- `PATCH_REVISION` — number of commits in the Agent Browser repo.
- `BUILD_NUMBER` — `github.run_number`.

`release.yml` writes these into `build-metadata.txt` attached to each GitHub
release.

## Bumping

1. Edit `VERSION` (e.g. `1.0.0` → `1.1.0`).
2. Commit to main.
3. Tag `v1.1.0` and push the tag — `release.yml` builds and publishes.

## Compatibility

`CHROMIUM_BASE` in `VERSION` names the upstream branch the patch series
currently applies to (see docs/REBASE.md).
