#!/usr/bin/env bash
set -euo pipefail

# Report and safely reclaim Docker storage without touching volumes.
# Intended for bot hosts and the shared Hermes host.

RETENTION="${DOCKER_MAINTENANCE_RETENTION:-168h}"
CLEANUP_AT="${DOCKER_MAINTENANCE_CLEANUP_AT:-85}"
FAIL_AT="${DOCKER_MAINTENANCE_FAIL_AT:-95}"
MODE="report"

usage() {
  cat <<'USAGE'
Usage: scripts/docker-maintenance.sh [options]

Options:
  --report       Print usage information only (default)
  --cleanup      Clean safe unused Docker data when usage reaches the threshold
  --check        Check usage without cleanup
  --dry-run      Show what cleanup would do without changing Docker data
  --cleanup-at N Cleanup threshold percentage (default: 85)
  --fail-at N    Failure threshold percentage (default: 95)
  --retention X  Age filter for unused data (default: 168h)
  --help         Show this help

Volumes are never pruned by this script.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --report) MODE="report"; shift ;;
    --cleanup) MODE="cleanup"; shift ;;
    --check) MODE="check"; shift ;;
    --dry-run) MODE="dry-run"; shift ;;
    --cleanup-at) CLEANUP_AT="${2:?missing --cleanup-at value}"; shift 2 ;;
    --fail-at) FAIL_AT="${2:?missing --fail-at value}"; shift 2 ;;
    --retention) RETENTION="${2:?missing --retention value}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

for value in "$CLEANUP_AT" "$FAIL_AT"; do
  [[ "$value" =~ ^[0-9]+$ ]] || { echo "Threshold must be an integer: $value" >&2; exit 2; }
done
(( CLEANUP_AT < FAIL_AT )) || { echo "cleanup threshold must be below fail threshold" >&2; exit 2; }

command -v docker >/dev/null 2>&1 || { echo "ERROR: docker is not installed" >&2; exit 1; }

LOCK_FILE="${DOCKER_MAINTENANCE_LOCK_FILE:-/tmp/the-bot-docker-maintenance.lock}"
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "Docker maintenance is already running: $LOCK_FILE"
  exit 0
fi

docker_root="$(docker info --format '{{.DockerRootDir}}' 2>/dev/null || true)"
docker_root="${docker_root:-/var/lib/docker}"
containerd_root="${CONTAINERD_ROOT:-/var/lib/containerd}"

usage_for() {
  df -P "$1" 2>/dev/null | awk 'NR == 2 { gsub(/%/, "", $5); print $5 }'
}

docker_usage="$(usage_for "$docker_root")"
containerd_usage="$(usage_for "$containerd_root")"
[[ "$docker_usage" =~ ^[0-9]+$ ]] || docker_usage=0
[[ "$containerd_usage" =~ ^[0-9]+$ ]] || containerd_usage=0

echo "Docker root: $docker_root (${docker_usage}% used)"
if [[ -d "$containerd_root" ]]; then
  echo "containerd root: $containerd_root (${containerd_usage}% used)"
else
  echo "containerd root: $containerd_root (not present)"
fi
docker system df || true

max_usage="$docker_usage"
if (( containerd_usage > max_usage )); then
  max_usage="$containerd_usage"
fi

if (( max_usage >= 70 && max_usage < CLEANUP_AT )); then
  echo "WARNING: Docker storage is at ${max_usage}% (warning threshold: 70%)."
fi

cleanup() {
  if [[ "$MODE" == "dry-run" ]]; then
    echo "Dry run: would prune unused images, build cache, stopped containers, and networks older than $RETENTION"
    echo "Dry run: named and anonymous volumes are preserved"
    return
  fi
  echo "Cleaning unused Docker data older than $RETENTION (volumes preserved)"
  docker image prune -af --filter "until=$RETENTION"
  docker builder prune -af --filter "until=$RETENTION"
  docker container prune -f --filter "until=$RETENTION"
  docker network prune -f
}

if [[ "$MODE" == "cleanup" || "$MODE" == "dry-run" ]]; then
  # Scheduled cleanup is intentionally safe to run below the emergency
  # threshold: it prevents old cache from accumulating until the disk is full.
  cleanup
  if [[ "$MODE" == "cleanup" ]]; then
    docker_usage="$(usage_for "$docker_root")" || true
    containerd_usage="$(usage_for "$containerd_root")" || true
    [[ "$docker_usage" =~ ^[0-9]+$ ]] || docker_usage=0
    [[ "$containerd_usage" =~ ^[0-9]+$ ]] || containerd_usage=0
    max_usage="$docker_usage"
    (( containerd_usage > max_usage )) && max_usage="$containerd_usage"
    echo "Post-maintenance Docker usage: ${docker_usage}%"
    [[ -d "$containerd_root" ]] && echo "Post-maintenance containerd usage: ${containerd_usage}%"
  fi
elif (( max_usage >= CLEANUP_AT )); then
  echo "Storage is at or above cleanup threshold (${CLEANUP_AT}%). Use --cleanup to reclaim safe unused data."
fi

if (( max_usage >= FAIL_AT )); then
  echo "ERROR: Docker storage filesystem remains at ${max_usage}% (failure threshold: ${FAIL_AT}%)." >&2
  echo "Inspect: docker system df; du -xhd1 '$docker_root' '$containerd_root' 2>/dev/null" >&2
  exit 1
fi
