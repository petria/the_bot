# the_bot_whatsapp

WhatsApp sidecar for `bot-io`.

The image contains `wacli` and a small HTTP wrapper:

- `wacli sync --follow` receives WhatsApp Web messages and posts them to `bot-io`.
- `POST /send` executes `wacli --json send text` for outbound messages.

## First Pairing

Use a persistent store directory. For the dev WhatsApp SIM:

```bash
WHATSAPP_WACLI_DIR=wacli-dev docker compose run --rm bot-whatsapp auth --phone +358449125874
```

Scan or approve the pairing from WhatsApp. The linked-device session is stored under:

```text
${HOKAN_DATA_DIR}/wacli-dev
```

After pairing:

```bash
docker compose up -d bot-whatsapp bot-io
```

## Runtime Config

`bot-io` needs `whatsappConfig` enabled in the runtime bot config:

```json
"whatsappConfig": {
  "network": "WhatsApp",
  "sendBaseUrl": "http://bot-whatsapp:8095",
  "sendToken": "${WHATSAPP_SEND_TOKEN:}",
  "webhookSecret": "${WHATSAPP_WEBHOOK_SECRET:}",
  "channelList": [],
  "connectStartup": true
}
```

Do not delete the `wacli-dev` store unless you want to pair the WhatsApp account again.
