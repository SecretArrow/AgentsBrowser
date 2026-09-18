# Self-Hosted Runner Setup

The full Chromium Android build cannot run on GitHub-hosted runners
(100 GB+ disk, 6–12 h per build). Run it on your own machine.

## Requirements

- Ubuntu 22.04+ x86_64
- 16+ cores (32 recommended)
- 64 GB RAM (32 minimum)
- 256 GB+ SSD free

## One-time setup

```bash
# on the builder machine
bash scripts/setup-builder-machine.sh
```

Then register the GitHub Actions runner:

1. GitHub → dewrin/AgentBrowser → Settings → Actions → Runners → New runner
2. Download the tarball into `/home/builder/actions-runner`
3. As the `builder` user:

```bash
./config.sh --url https://github.com/dewrin/AgentBrowser \
  --labels chromium-builder --work /home/builder/agentbrowser-work
./svc.sh install
./svc.sh start
```

4. Pre-warm the Chromium checkout (saves ~1 h per build):

```bash
sudo -iu builder
cd ~ && bash -c 'git clone <this repo> ab && cd ab && bash scripts/sync-chromium.sh'
```

## Why a user account instead of root

`svc.sh` runs the runner as a service under the `builder` user; builds write
hundreds of GB and running as root makes cleanup painful.

## Verifying

```bash
# from anywhere: set repo variable CHROMIUM_RUNNER=self-hosted (or dispatch
# chromium-build with runner=self-hosted) — the build starts when this runner
# is online. The autopilot also dispatches this lane automatically.
# on the runner and artifacts appear under the run summary.
```
