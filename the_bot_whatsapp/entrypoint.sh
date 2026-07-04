#!/bin/sh
set -eu

if [ "${1:-serve}" != "serve" ]; then
  exec wacli "$@"
fi

if [ -z "${WACLI_WEBHOOK_URL:-}" ]; then
  echo "WACLI_WEBHOOK_URL is required when running bot-whatsapp in serve mode" >&2
  exit 2
fi

set -- wacli sync --follow --max-reconnect "${WACLI_SYNC_MAX_RECONNECT:-0}" --max-db-size "${WACLI_SYNC_MAX_DB_SIZE:-2GB}" --webhook "$WACLI_WEBHOOK_URL"
if [ "${WACLI_SYNC_DOWNLOAD_MEDIA:-true}" = "true" ]; then
  set -- "$@" --download-media
fi
if [ -n "${WACLI_WEBHOOK_SECRET:-}" ]; then
  set -- "$@" --webhook-secret "$WACLI_WEBHOOK_SECRET"
fi

exec python /opt/bot-whatsapp/supervisor.py "$@"
