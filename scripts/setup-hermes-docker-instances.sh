#!/usr/bin/env bash
set -euo pipefail

# Setup one Docker Compose based Hermes deployment with one or more API profiles.
# Default target layout on the selected host:
#   ~/bot-hermes/
#     docker-compose.yml
#     .env
#     hermes-agent/
#     data/
#     the_bot-hermes-instances.env

usage() {
  cat <<'USAGE'
Usage:
  scripts/setup-hermes-docker-instances.sh [options]

Examples:
  scripts/setup-hermes-docker-instances.sh \
    --target hokan@ubuntu-server.local \
    --directory ~/bot-hermes \
    --profiles chat:8643:chat,coder:8644:agent,ai-command:8645:agent:hermes-ai-command \
    --public-host ubuntu-server.local \
    --build \
    --start \
    --verify

Options:
  --target HOST          local, or an SSH target like user@host. Default: local
  --directory PATH      Compose directory on target. Default: ~/bot-hermes
  --profiles LIST       Comma-separated profile specs: name:port[:mode[:model_label]]
                        mode defaults to chat for profile "chat", otherwise agent.
                        model_label defaults to hermes-<name>.
  --repo-url URL        Hermes Agent git repo. Default: https://github.com/NousResearch/hermes-agent.git
  --repo-ref REF        Git branch/tag/ref to checkout/update. Default: main
  --public-host HOST    Hostname/IP printed into bot env base URLs. Default: localhost
  --host HOST           API_SERVER_HOST inside profiles. Default: 0.0.0.0
  --build               Build/update the local Hermes Docker image.
  --no-build            Do not build image. Default.
  --start               Start compose service and profile gateways.
  --no-start            Configure only; do not start services. Default.
  --verify              Verify /health and /v1/toolsets for each profile.
  --force-api-keys      Regenerate profile API keys even if present.
  -h, --help            Show this help
USAGE
}

TARGET="local"
DEPLOY_DIR='~/bot-hermes'
PROFILES="chat:8643:chat,coder:8644:agent,ai-command:8645:agent:hermes-ai-command"
REPO_URL="https://github.com/NousResearch/hermes-agent.git"
REPO_REF="main"
PUBLIC_HOST="localhost"
API_HOST="0.0.0.0"
BUILD=0
START=0
VERIFY=0
FORCE_API_KEYS=0
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) TARGET="${2:?missing --target value}"; shift 2 ;;
    --directory) DEPLOY_DIR="${2:?missing --directory value}"; shift 2 ;;
    --profiles|--instances) PROFILES="${2:?missing --profiles value}"; shift 2 ;;
    --repo-url) REPO_URL="${2:?missing --repo-url value}"; shift 2 ;;
    --repo-ref) REPO_REF="${2:?missing --repo-ref value}"; shift 2 ;;
    --public-host) PUBLIC_HOST="${2:?missing --public-host value}"; shift 2 ;;
    --host) API_HOST="${2:?missing --host value}"; shift 2 ;;
    --build) BUILD=1; shift ;;
    --no-build) BUILD=0; shift ;;
    --start) START=1; shift ;;
    --no-start) START=0; shift ;;
    --verify) VERIFY=1; shift ;;
    --force-api-keys) FORCE_API_KEYS=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if [[ "$TARGET" != "local" && "$TARGET" != "localhost" ]]; then
  remote_script="/tmp/the_bot_setup_hermes_docker_$$.sh"
  remote_script_dir="$(dirname "$remote_script")"
  remote_overlay_dir="$remote_script_dir/hermes"
  echo "Copying setup script to $TARGET:$remote_script"
  scp -q "$0" "$TARGET:$remote_script"
  if [[ -d "$SCRIPT_DIR/hermes" ]]; then
    ssh "$TARGET" "mkdir -p '$remote_overlay_dir'"
    scp -q "$SCRIPT_DIR"/hermes/* "$TARGET:$remote_overlay_dir/"
  fi
  echo "Running Docker Hermes setup on $TARGET"
  ssh "$TARGET" bash "$remote_script" \
    --target local \
    --directory "$DEPLOY_DIR" \
    --profiles "$PROFILES" \
    --repo-url "$REPO_URL" \
    --repo-ref "$REPO_REF" \
    --public-host "$PUBLIC_HOST" \
    --host "$API_HOST" \
    $([[ "$BUILD" == 1 ]] && printf '%s' --build || printf '%s' --no-build) \
    $([[ "$START" == 1 ]] && printf '%s' --start || printf '%s' --no-start) \
    $([[ "$VERIFY" == 1 ]] && printf '%s' --verify || true) \
    $([[ "$FORCE_API_KEYS" == 1 ]] && printf '%s' --force-api-keys || true)
  ssh "$TARGET" "rm -f '$remote_script'; rm -rf '$remote_overlay_dir'" >/dev/null 2>&1 || true
  exit 0
fi

expand_path() {
  local p="$1"
  if [[ "$p" == \~/* ]]; then
    printf '%s/%s' "$HOME" "${p#\~/}"
  elif [[ "$p" == "~" ]]; then
    printf '%s' "$HOME"
  else
    printf '%s' "$p"
  fi
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $1" >&2
    exit 1
  fi
}

generate_key() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
  else
    python3 - <<'PY'
import secrets
print(secrets.token_hex(32))
PY
  fi
}

env_get() {
  local file="$1" key="$2"
  [[ -f "$file" ]] || return 0
  awk -F= -v k="$key" '$1 == k { sub(/^[^=]*=/, ""); print; exit }' "$file"
}

env_set() {
  local file="$1" key="$2" value="$3"
  mkdir -p "$(dirname "$file")"
  touch "$file"
  chmod 600 "$file"
  local tmp
  tmp="$(mktemp)"
  grep -v -E "^${key}=" "$file" > "$tmp" || true
  printf '%s=%s\n' "$key" "$value" >> "$tmp"
  mv "$tmp" "$file"
  chmod 600 "$file"
}

upper_profile() {
  local s="$1"
  s="${s//-/_}"
  s="${s//./_}"
  printf '%s' "$s" | tr '[:lower:]' '[:upper:]'
}

compose() {
  docker compose --env-file "$DEPLOY_DIR/.env" -f "$DEPLOY_DIR/docker-compose.yml" "$@"
}

container_exec() {
  compose exec -T bot-hermes "$@"
}

wait_for_container_ready() {
  echo "Waiting for bot-hermes container init to complete"
  local attempt
  for attempt in $(seq 1 120); do
    if container_exec sh -lc 'test "$(stat -c %U:%G /run/service 2>/dev/null)" = "hermes:hermes" && test -x /opt/hermes/bin/hermes' >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: bot-hermes did not finish container init in time" >&2
  compose logs --tail=120 bot-hermes >&2 || true
  exit 1
}

wait_for_profile_health() {
  local profile="$1" port="$2"
  local attempt
  for attempt in $(seq 1 60); do
    if curl -fsS "http://127.0.0.1:${port}/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "WARN: /health did not become ready for $profile on port $port" >&2
  return 1
}

patch_profile_config() {
  local profile="$1" mode="$2"
  container_exec python3 - "$profile" "$mode" <<'PY'
import os
import subprocess
import sys
from pathlib import Path

try:
    import yaml
except Exception as exc:
    raise SystemExit(f"PyYAML is required to patch Hermes config: {exc}")

profile = sys.argv[1]
mode = sys.argv[2].strip().lower()
config_path = subprocess.check_output(
    ["hermes", "-p", profile, "config", "path"],
    text=True,
).strip()

path = Path(config_path)
data = {}
if path.exists():
    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
if not isinstance(data, dict):
    data = {}

platform_toolsets = data.setdefault("platform_toolsets", {})
agent = data.setdefault("agent", {})
terminal = data.setdefault("terminal", {})

# Do not depend on any host repo bind mount inside the Hermes container.
terminal["backend"] = "local"
terminal["cwd"] = "."
terminal["docker_mount_cwd_to_workspace"] = False

if mode == "chat":
    soul_path = path.parent / "SOUL.md"
    soul_path.write_text(
        "You are Hokan chat assistant for IRC, Discord, Telegram, and WhatsApp users.\n\n"
        "You are a plain conversational assistant in this profile. You do not have shell tools, "
        "file tools, browser tools, skill tools, memory tools, or command execution tools available. "
        "Do not claim that you can run commands, inspect files, load skills, browse, edit files, "
        "or access the host.\n\n"
        "If a user asks what tools you have, answer that this profile has no external tools exposed "
        "and can only respond with text. Keep answers concise and suitable for chat.\n",
        encoding="utf-8",
    )
    platform_toolsets["api_server"] = ["no_mcp"]
    disabled = set(agent.get("disabled_toolsets") or [])
    disabled.update({
        "browser", "clarify", "code_execution", "computer_use", "cronjob",
        "delegation", "file", "image_gen", "memory", "messaging",
        "session_search", "skills", "terminal", "todo", "tts", "vision",
        "web", "search", "x_search", "homeassistant", "spotify", "discord",
        "discord_admin", "feishu_doc", "feishu_drive", "yuanbao", "kanban",
        "debugging", "rl", "moa",
    })
    agent["disabled_toolsets"] = sorted(disabled)
    agent.setdefault("max_turns", 10)
else:
    platform_toolsets.setdefault("api_server", ["hermes-api-server"])
    model = data.setdefault("model", {})
    model["default"] = "gpt-5.5"
    model["provider"] = "openai-codex"
    model["base_url"] = "https://chatgpt.com/backend-api/codex"
    model["context_length"] = 262144

path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(yaml.safe_dump(data, sort_keys=False, allow_unicode=True), encoding="utf-8")
print(f"patched {path} mode={mode}")
PY
}

DEPLOY_DIR="$(expand_path "$DEPLOY_DIR")"
SOURCE_DIR="$DEPLOY_DIR/hermes-agent"
DATA_DIR="$DEPLOY_DIR/data"
BOT_ENV_FILE="$DEPLOY_DIR/the_bot-hermes-instances.env"
AUTOSTART_OVERLAY="$SCRIPT_DIR/hermes/03-autostart-bot-profiles"

require_cmd docker
require_cmd git

mkdir -p "$DEPLOY_DIR" "$DATA_DIR"

if [[ ! -d "$SOURCE_DIR/.git" ]]; then
  echo "Cloning Hermes Agent into $SOURCE_DIR"
  git clone "$REPO_URL" "$SOURCE_DIR"
else
  echo "Hermes Agent source already exists: $SOURCE_DIR"
fi

if [[ "$BUILD" == 1 ]]; then
  echo "Updating Hermes Agent source to $REPO_REF"
  git -C "$SOURCE_DIR" fetch --all --tags --prune
  git -C "$SOURCE_DIR" checkout "$REPO_REF"
  git -C "$SOURCE_DIR" pull --ff-only || true
fi

if [[ -f "$AUTOSTART_OVERLAY" ]]; then
  echo "Installing bot Hermes autostart overlay"
  mkdir -p "$SOURCE_DIR/docker/cont-init.d"
  cp "$AUTOSTART_OVERLAY" "$SOURCE_DIR/docker/cont-init.d/03-autostart-bot-profiles"
  chmod 0755 "$SOURCE_DIR/docker/cont-init.d/03-autostart-bot-profiles"
  if ! grep -q '03-autostart-bot-profiles' "$SOURCE_DIR/Dockerfile"; then
    python3 - "$SOURCE_DIR/Dockerfile" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text()
needle = 'COPY --chmod=0755 docker/cont-init.d/02-reconcile-profiles /etc/cont-init.d/02-reconcile-profiles\n'
insert = needle + 'COPY --chmod=0755 docker/cont-init.d/03-autostart-bot-profiles /etc/cont-init.d/03-autostart-bot-profiles\n'
if needle in text and '03-autostart-bot-profiles' not in text:
    text = text.replace(needle, insert, 1)
path.write_text(text)
PY
  fi
fi

AUTOSTART_PROFILES="$(IFS=','; for spec in $PROFILES; do IFS=':' read -r profile _rest <<< "$spec"; printf '%s,' "$profile"; done)"
AUTOSTART_PROFILES="${AUTOSTART_PROFILES%,}"

cat > "$DEPLOY_DIR/.env" <<EOF
HERMES_UID=$(id -u)
HERMES_GID=$(id -g)
HERMES_IMAGE=bot-hermes-agent:local
EOF
chmod 600 "$DEPLOY_DIR/.env"

cat > "$DEPLOY_DIR/docker-compose.yml" <<EOF
services:
  bot-hermes:
    build:
      context: ./hermes-agent
    image: \${HERMES_IMAGE:-bot-hermes-agent:local}
    container_name: bot-hermes
    restart: unless-stopped
    volumes:
      - ./data:/opt/data
      - ./hermes-agent/docker/cont-init.d/03-autostart-bot-profiles:/etc/cont-init.d/03-autostart-bot-profiles:ro
    ports:
$(IFS=','; for spec in $PROFILES; do IFS=':' read -r _profile port _rest <<< "$spec"; printf '      - "0.0.0.0:%s:%s"\n' "$port" "$port"; done)
    environment:
      - HERMES_UID=\${HERMES_UID:-10000}
      - HERMES_GID=\${HERMES_GID:-10000}
      - BOT_HERMES_AUTOSTART_PROFILES=\${BOT_HERMES_AUTOSTART_PROFILES:-$AUTOSTART_PROFILES}
      - PYTHONUNBUFFERED=1
    command: ["sleep", "infinity"]
EOF

echo "Wrote $DEPLOY_DIR/docker-compose.yml"

if [[ "$BUILD" == 1 ]]; then
  echo "Building Hermes Docker image"
  compose build bot-hermes
fi

if [[ "$START" == 1 ]]; then
  echo "Starting bot-hermes container"
  compose up -d bot-hermes
  wait_for_container_ready
fi

if [[ "$START" != 1 ]]; then
  echo "Skipping profile creation because --start was not given"
  exit 0
fi

: > "$BOT_ENV_FILE"
chmod 600 "$BOT_ENV_FILE"

IFS=',' read -ra SPECS <<< "$PROFILES"
first=1
for spec in "${SPECS[@]}"; do
  IFS=':' read -r profile port mode model_label <<< "$spec"
  if [[ -z "${profile:-}" || -z "${port:-}" ]]; then
    echo "ERROR: invalid profile spec '$spec' (expected name:port[:mode[:model_label]])" >&2
    exit 2
  fi
  if [[ -z "${mode:-}" ]]; then
    if [[ "$profile" == "chat" ]]; then mode="chat"; else mode="agent"; fi
  fi
  if [[ -z "${model_label:-}" ]]; then
    model_label="hermes-${profile}"
  fi

  if ! container_exec hermes profile show "$profile" >/dev/null 2>&1; then
    echo "Creating profile $profile"
    container_exec hermes profile create "$profile" --clone-from default \
      || container_exec hermes profile create "$profile" --clone
  else
    echo "Profile exists: $profile"
  fi

  env_path="$(container_exec hermes -p "$profile" config env-path | tr -d '\r')"
  host_env_path="$DATA_DIR/${env_path#/opt/data/}"
  api_key="$(env_get "$host_env_path" API_SERVER_KEY || true)"
  if [[ -z "$api_key" || "$FORCE_API_KEYS" == 1 ]]; then
    api_key="$(generate_key)"
  fi

  env_set "$host_env_path" API_SERVER_ENABLED true
  env_set "$host_env_path" API_SERVER_HOST "$API_HOST"
  env_set "$host_env_path" API_SERVER_PORT "$port"
  env_set "$host_env_path" API_SERVER_KEY "$api_key"
  env_set "$host_env_path" API_SERVER_MODEL_NAME "$model_label"

  patch_profile_config "$profile" "$mode"

  echo "Starting profile gateway $profile"
  container_exec hermes -p "$profile" gateway start

  up="$(upper_profile "$profile")"
  base_url="http://${PUBLIC_HOST}:${port}"
  env_set "$BOT_ENV_FILE" "HERMES_${up}_BASE_URL" "$base_url"
  env_set "$BOT_ENV_FILE" "HERMES_${up}_API_KEY" "$api_key"
  env_set "$BOT_ENV_FILE" "HERMES_${up}_MODEL" "$model_label"
  env_set "$BOT_ENV_FILE" "HERMES_${up}_TIMEOUT_SECONDS" "120"
  env_set "$BOT_ENV_FILE" "HERMES_${up}_MODE" "$mode"

  if [[ "$first" == 1 ]]; then
    env_set "$BOT_ENV_FILE" HERMES_BASE_URL "$base_url"
    env_set "$BOT_ENV_FILE" HERMES_API_KEY "$api_key"
    env_set "$BOT_ENV_FILE" HERMES_MODEL "$model_label"
    env_set "$BOT_ENV_FILE" HERMES_TIMEOUT_SECONDS "120"
    first=0
  fi

  echo "Configured profile '$profile' at $base_url mode=$mode key=[REDACTED]"

  if [[ "$VERIFY" == 1 ]]; then
    echo "Verifying $profile health"
    wait_for_profile_health "$profile" "$port" || true
    curl -fsS "http://127.0.0.1:${port}/health" || echo "WARN: /health failed for $profile"
    echo
    echo "Verifying $profile toolsets"
    curl -fsS -H "Authorization: Bearer ${api_key}" "http://127.0.0.1:${port}/v1/toolsets" || echo "WARN: /v1/toolsets failed for $profile"
    echo
  fi
done

echo
echo "Hermes Docker deployment directory: $DEPLOY_DIR"
echo "Bot-facing env file written: $BOT_ENV_FILE (chmod 600; API keys not printed)"
