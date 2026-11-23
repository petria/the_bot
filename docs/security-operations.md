# Bot security operations

## Service credentials

Set GitHub environment secrets per instance and deploy the related services together.

| Secret | Services |
| --- | --- |
| THE_BOT_INTERNAL_API_TOKEN | Engine, I/O, web, and WhatsApp sidecar internal calls |
| THE_BOT_NOTIFICATION_SIGNING_KEY | Engine and web notification delivery |
| WHATSAPP_WEBHOOK_SECRET | WhatsApp sidecar and I/O webhook |
| HERMES_MANAGER_TOKEN | Engine and Hermes manager |

Rotate one credential at a time. Redeploy all its consumers, then check authenticated traffic before rotating the next. Notification and WhatsApp requests fail closed during a key mismatch. Use independently generated keys for develop and production.

## Hermes

The manager accepts the configured default backend origin and the comma-separated instance allowlist in the manager Compose configuration. An origin includes scheme, hostname and port. A stored backend key can only be reused for that backend's provider and saved base URL.

The manager writes the managed provider key to the profile's mode-0600 .env file. The gateway reads it through an environment reference in YAML. The manager signals the gateway supervisor through profile files with fixed operations and has no Docker daemon socket.

Backend updates preserve runtime state. A manager API update with purgeState set to true stops the routing profiles and saves their prior state under each profile's state-backup directory before starting them again. Backups contain conversation data and require the same access controls as the profile itself.

## Web and debugging

Configure THE_BOT_WEB_TRUSTED_PROXY_ADDRESSES with the connecting proxy's exact IP only when that proxy overwrites X-Real-IP. Otherwise the rate limiter uses the socket peer address. Configure external Web UI link hosts through THE_BOT_WEBUI_LINKS_ALLOWED_HOSTS. The two built-in domains and the instance's configured HTTPS public URL host are already permitted.

The web CSP starts in report-only mode. Check browser console violations for login, admin, live channels, and media before setting THE_BOT_WEB_CSP_REPORT_ONLY=false. CLI clients should use the matching release, which fetches a CSRF token before login and again after session rotation.

The develop debugger is published on the Docker host loopback. Forward the port over SSH for remote IDE access and connect the IDE to the forwarded local port.
