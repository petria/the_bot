# Hermes Manager Isolation - Session Notes

## What Was Done

### 1. Server Setup (ubuntu-server.local)
- Created two isolated Hermes Manager instances running on different ports
- `bot-hermes-manager-main` → port 8651 (data dir: `/home/hokan/bot-hermes/data-main`)
- `bot-hermes-manager-develop` → port 8652 (data dir: `/home/hokan/bot-hermes/data-develop`)
- Both containers running and healthy with Docker Compose

### 2. Code Changes (branch: develop, commit: eced4ff)

**Files Modified/Created:**
- `.env.example` - Added `HERMES_MANAGER_BASE_URL`, `HERMES_MANAGER_MAIN_BASE_URL`, `HERMES_MANAGER_DEVELOP_BASE_URL`
- `.github/workflows/ci.yml` - Routes to correct manager based on `HOKAN_BOT_INSTANCE_ID`
  - Main (hokan-main) → port 8651
  - Develop (hokan-develop or default) → port 8652
- `docker-compose.yml` - Added two services with `HERMES_MANAGER_BASE_URL` fallback chain
- `env/hokan-main.env` - Main instance config with `HERMES_MANAGER_BASE_URL=http://ubuntu-server.local:8651`
- `env/hokan-develop.env` - Develop instance config with `HERMES_MANAGER_BASE_URL=http://ubuntu-server.local:8652`

### 3. Current State

**Working on ubuntu-server.local:**
- Both hermes managers running on ports 8651 and 8652
- API endpoints responding at both URLs

**CI Status:**
- Pushed to `develop` branch
- CI run (27168402583) is in "completed/failure" state
- Need to investigate why the build/deploy failed

## What Still Needs To Be Done

### 1. Investigate CI Failure
- Check which job failed and why
- Review logs for errors
- Verify all environment variables are being passed correctly
- The failure may be in:
  - Build step (Docker builds)
  - Deploy step (VM deployment)
  - Hermes manager configuration on VM

### 2. Deploy to ubuntu-server.local
- Once CI passes, the new images should be deployed
- Verify both containers start with correct ports
- Test API endpoints on ports 8651 and 8652

### 3. Validate Bot Instances
- Main bot instance should connect to port 8651
- Develop bot instance should connect to port 8652
- Both instances should have independent fallback configurations

## Key Configuration

**Environment Variable Priority:**
```
HERMES_MANAGER_BASE_URL (set by CI based on branch)
├─ Main → http://ubuntu-server.local:8651
└─ Develop → http://ubuntu-server.local:8652
```

**Docker Compose Services:**
- `bot-hermes-manager-main` (port 8651)
- `bot-hermes-manager-develop` (port 8652)

## Related Files

- `/home/petria/code/github/the_bot/.agents/hermes-manager-isolation.md` - This file
- `/home/petria/code/github/the_bot/docker-compose.yml`
- `/home/petria/code/github/the_bot/.github/workflows/ci.yml`
- `/home/petria/code/github/the_bot/env/hokan-main.env`
- `/home/petria/code/github/the_bot/env/hokan-develop.env`

## SSH Access

Server: `ssh hokan@ubuntu-server.local`

Check running containers:
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Test hermes manager API:
```bash
curl -s http://localhost:8651/actuator/health
curl -s http://localhost:8652/actuator/health
```
