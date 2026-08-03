# Docker Storage Maintenance

The bot hosts use bounded container logs and conservative Docker cleanup. The
cleanup never prunes volumes, so media, Wacli, runtime, and Hermes state remain
outside the disposable image/container cache.

## Install the host timer

Run as root on each Docker host:

```bash
sudo scripts/install-docker-maintenance.sh
```

The timer runs 15 minutes after boot and then hourly. Results are available
with:

```bash
systemctl status the-bot-docker-maintenance.timer
journalctl -u the-bot-docker-maintenance.service
```

## Manual checks

```bash
sudo scripts/docker-maintenance.sh --report
sudo scripts/docker-maintenance.sh --cleanup
```

At 70% usage the script warns. Scheduled and CI cleanup removes unused images,
BuildKit cache, stopped containers, and networks older than seven days before
the disk reaches the emergency threshold. At 95% it fails so CI does not start
another large image build on an unsafe filesystem.

## Docker daemon log rotation

Install this as `/etc/docker/daemon.json` and restart Docker during a planned
maintenance window:

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "50m",
    "max-file": "3"
  }
}
```

Existing containers must be recreated before the daemon-level setting applies
to them. Compose files in this repository also declare the same limits for new
and recreated services.

## Moving Docker storage

If a host still reaches the threshold after cleanup, move Docker and
containerd to a dedicated larger filesystem. Stop Docker and containerd,
copy the data while preserving ownership, configure Docker `data-root` and
the containerd root, then restart and verify with:

```bash
docker info --format '{{.DockerRootDir}}'
docker system df
docker compose ps
```

Keep `/mnt/hokan/storagebox-sub2` and all runtime/Wacli/Hermes state outside
Docker's disposable storage root.
