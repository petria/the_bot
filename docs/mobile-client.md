# Android Bot Client

The Android project is independent from the Maven reactor in `the_bot_android/`.
Each build targets exactly one bot-web instance:

- `devDebug`: `https://hokandevbot.airiot.fi`
- `prodRelease`: `https://hokanthebot.airiot.fi`

The app never calls bot-engine, bot-io, or Hermes directly. Mobile authentication
uses `POST /api/mobile/auth/login` and short-lived bearer access tokens with
rotating, persisted refresh tokens. Refresh state is stored by bot-web in
`THE_BOT_WEB_MOBILE_AUTH_FILE`, separate from `users.json`.

## Bot-web configuration

FCM is disabled by default. To enable push delivery, configure these environment
variables for the bot-web container:

```text
THE_BOT_WEB_MOBILE_FCM_ENABLED=true
THE_BOT_WEB_MOBILE_FCM_CREDENTIALS_FILE=/run/secrets/firebase-service-account.json
THE_BOT_WEB_MOBILE_AUTH_FILE=runtime/data/mobile-auth.json
THE_BOT_WEB_MOBILE_NOTIFICATIONS_FILE=runtime/data/mobile-notifications.json
```

The existing `THE_BOT_INTERNAL_API_TOKEN` must be shared by bot-engine and
bot-web so engine notification events can enter bot-web. The Firebase service
account file is a deployment secret and must not be committed.

The mobile API includes user-scoped notification inbox/read APIs, device
registration, notification-rule management, permitted connection map, and a
command gateway. Channel and command authorization remains the same permission
model used by the web UI and bot-engine.

## Firebase build metadata

For a real FCM app build, provide the matching non-committed
`app/src/dev/google-services.json` or `app/src/prod/google-services.json`.
The Gradle script applies the Google Services plugin only when one is present.
