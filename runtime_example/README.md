This is the example runtime/ directory Bot need when run.

You need to rename this from runtime_example/-> runtime/

and make sure the directory is located in the current dir of bot java process.

Launch both `bot-io` and `bot-engine` with:

`BOT_CONFIG_FILE=./runtime/dev.properties`

The shared bootstrap properties file points both modules to the same runtime config.

The same `dev.properties` file also carries engine runtime settings such as OpenClaw, weather and OpenAI config.

Runtime JSON placeholders like `${SLACK_TOKEN_SECRET}` are resolved from environment variables.

Properties placeholders inside `dev.properties` should also be provided as environment variables, for example:

`OPENCLAW_HOOKS_TOKEN`
`OPENCLAW_GATEWAY_TOKEN`
`OPENCLAW_NODE_CONTEXT_SECRET`
`OPENAI_API_KEY`
`WEATHER_API_KEY`
`SLACK_TOKEN_SECRET`
`DISCORD_SECRET`
`TELEGRAM_SECRET`

Do not ever commit secret tokens to version control.


