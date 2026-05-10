#!/bin/sh
set -eu

if [ "${1:-serve}" != "serve" ]; then
  exec wacli "$@"
fi

if [ -z "${WACLI_WEBHOOK_URL:-}" ]; then
  echo "WACLI_WEBHOOK_URL is required when running bot-whatsapp in serve mode" >&2
  exit 2
fi

if [ -n "${WACLI_WEBHOOK_SECRET:-}" ]; then
  wacli sync --follow --max-reconnect 0 --max-db-size "${WACLI_SYNC_MAX_DB_SIZE:-2GB}" --webhook "$WACLI_WEBHOOK_URL" --webhook-secret "$WACLI_WEBHOOK_SECRET" &
else
  wacli sync --follow --max-reconnect 0 --max-db-size "${WACLI_SYNC_MAX_DB_SIZE:-2GB}" --webhook "$WACLI_WEBHOOK_URL" &
fi
SYNC_PID=$!

terminate() {
  kill "$SYNC_PID" 2>/dev/null || true
  wait "$SYNC_PID" 2>/dev/null || true
}
trap terminate INT TERM

python /opt/bot-whatsapp/server.py &
SERVER_PID=$!

wait "$SERVER_PID"
terminate
