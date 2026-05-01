This is the example runtime/ directory the bot needs when run.

Copy or rename this from `runtime_example/` to `runtime/`.

For IDEA local development, launch both `bot-io` and `bot-engine` with:

`BOT_CONFIG_FILE=./runtime/dev.properties`

The shared bootstrap properties file points both modules to the same runtime config.
Path values in `dev.properties` are resolved relative to the properties file, so `./DEV.the_bot_config.json` means `runtime/DEV.the_bot_config.json` when `BOT_CONFIG_FILE=./runtime/dev.properties`.

The same `dev.properties` file also carries engine runtime settings such as OpenClaw, weather and OpenAI config. Optional placeholders can use `${ENV_NAME:default}`. Required placeholders use `${ENV_NAME}` and fail fast when missing.

## Runtime config precedence

Most runtime values are resolved through the shared `org.freakz.common.config.BotConfigService`.

Precedence is:

1. Runtime override, only for explicitly allowed mutable keys.
2. Bootstrap properties file selected by `BOT_CONFIG_FILE`.
3. Explicit environment or system property key passed by the caller.
4. Code default.

Currently only `channel.*` keys are runtime-mutable through the bot env-value commands and persisted in `runtime_overrides.json`. Existing `env_values.json` files are still read as a legacy fallback.

Examples:

`channel.do.public.ai`
`channel.do.url.topic`
`channel.do.sys.notify`

Secrets and infrastructure values such as `OPENAI_API_KEY`, `OPENCLAW_HOOKS_TOKEN`, `OPENCLAW_GATEWAY_TOKEN`, OpenClaw URLs and state paths are not runtime-mutable. They must come from CI/deploy environment, local IDEA environment, or the selected bootstrap properties file. Do not use `*.secret.properties` files for new config.

Minimum IDEA environment variables for local bot testing:

`BOT_CONFIG_FILE=./runtime/dev.properties`
`OPENCLAW_HOOKS_TOKEN`
`OPENAI_API_KEY`
`WEATHER_API_KEY`
`DISCORD_SECRET`
`TELEGRAM_SECRET`

Optional local overrides:

`OPENCLAW_GATEWAY_TOKEN`
`OPENCLAW_GATEWAY_WS_URL`
`OPENCLAW_GATEWAY_WS_ORIGIN`
`OPENCLAW_STATE_DIR_HOST`
`OPENCLAW_NODE_CONTEXT_SECRET`
`OPENCLAW_RUNTIME_LOG_ROOT`
`OPENCLAW_RUNTIME_LOG_ROOT_LOCAL`

`OPENCLAW_GATEWAY_WS_ORIGIN` must match one of OpenClaw's allowed control UI origins. Local dev defaults to `http://localhost:18899`.

Docker Compose injects `BOT_CONFIG_FILE=/runtime/dev.properties` by default for `bot-io` and `bot-engine`. CI deploy writes `/runtime/prod.properties` and points both modules to that file.

`prod.properties` is the container-oriented example of the same common config shape. It uses container paths such as `/runtime` and `/openclaw-state` after the file is mounted by Docker Compose.

Do not ever commit secret tokens to version control.
