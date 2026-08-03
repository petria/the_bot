#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run as root, for example: sudo $0" >&2
  exit 1
fi

install -m 0755 "$REPO_ROOT/scripts/docker-maintenance.sh" /usr/local/sbin/the-bot-docker-maintenance
install -m 0644 "$REPO_ROOT/systemd/the-bot-docker-maintenance.service" /etc/systemd/system/the-bot-docker-maintenance.service
install -m 0644 "$REPO_ROOT/systemd/the-bot-docker-maintenance.timer" /etc/systemd/system/the-bot-docker-maintenance.timer
systemctl daemon-reload
systemctl enable --now the-bot-docker-maintenance.timer
systemctl start the-bot-docker-maintenance.service
systemctl --no-pager --full status the-bot-docker-maintenance.timer
