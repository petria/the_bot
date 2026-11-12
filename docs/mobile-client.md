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

## Live water temperature

Enable **Manage System → Water temperature to watch** on the desired bot instance.
The engine reads the following deployment environment variables (CI uses matching
GitHub environment variables and writes them to Compose .env):

- `THE_BOT_WATER_WATCH_STATION`: station name, default Merikoski.
- `THE_BOT_WATER_WATCH_USERNAME`: existing bot username; required for delivery.
- `THE_BOT_WATER_WATCH_INTERVAL_MINUTES`: poll interval, default 60, minimum 5.

Develop is configured for Merikoski / petria / 30 minutes. The scheduler checks the
toggle every 30 seconds and polls immediately after enabling or engine restart.
It reuses the existing SYKE chart/vision lookup, saves the last successful reading
in runtime overrides, and retains its original retrieval time on lookup failure.
Readings expire after three polling intervals. Retrieval time is not measurement time.
Disabling publishes an unavailable state. Firebase delivery is data-only and silent.
Only the latest water update per user is retained in the mobile notification store.

Build phone and watch APKs together with the same signing key and flavor:

```sh
gradle -p the_bot_android :app:assembleDevDebug :wear:assembleDevDebug
```

Install `app/build/outputs/apk/dev/debug/app-dev-debug.apk` on the paired phone and
`wear/build/outputs/apk/dev/debug/wear-dev-debug.apk` on the watch. Both use
`org.freakz.botclient.dev`; prod uses `org.freakz.botclient`. CI restores the persistent debug key from the repository secret
`BOT_ANDROID_DEBUG_KEYSTORE_B64` for push/dispatch builds. The same key is used by
this workstation. Pull-request builds use an ephemeral key; install push-build
artifacts on regular devices so future updates preserve app data.
The previous `org.freakz.botclient.water.test` APK can remain installed for comparison.
Select **Water temperature (live)** from the new watch app for the face's right circle.
It starts unavailable, never with a simulated temperature.

Log into the phone app as the recipient user. The phone forwards the latest state
through persistent Wear OS DataItems on startup/login, on silent FCM delivery, and
through hourly background recovery work. WorkManager and normal-priority FCM may be
delayed by Android battery management; this is not a real-time delivery guarantee.
DataItems synchronize when the watch reconnects. The watch rejects older updates,
loads an existing DataItem when its provider starts, and marks expired data unavailable.
Logout clears the watch state. Test providers continue to use separate simulated data.

Acceptance: verify a real Merikoski value, silent background phone reception,
phone/watch reconnect, watch reboot, toggle Off, expiry and recovery. Unit/build
checks alone do not establish that full delivery chain on a physical paired phone.

## Branch-specific Android CI

The Android job builds matching `devDebug` phone/watch APKs for develop and
`prodDebug` APKs for main. Main's APKs use `org.freakz.botclient` and the production
bot URL; develop uses `org.freakz.botclient.dev` and the development URL.
Configure `FIREBASE_PROD_GOOGLE_SERVICES_JSON_B64` in the production GitHub environment
with the downloaded Firebase Android configuration for `org.freakz.botclient`.
Develop uses `FIREBASE_DEV_GOOGLE_SERVICES_JSON_B64` for `org.freakz.botclient.dev`.
The production secret must contain actual production app metadata, not a renamed
development app configuration. Android build failure does not establish a failure
of the separately running backend deployment jobs.

## Shared processed water cache

Bot-engine uses Spring caching with Caffeine to retain successful processed water
readings for 60 minutes after creation. Chat commands and watch polls share entries
by resolved station page URL; a hit skips chart downloads and Hermes vision.
Concurrent requests for the same station share a lookup. A miss downloads a fresh
chart image, processes it and stores the temperature with its retrieval timestamp.
Failures are not cached. Entries are bounded to 1,024 stations, are local to each
engine process and are lost on restart. Cache hits do not extend their lifetime.
Watch updates preserve the original reading timestamp and 90-minute expiry under
the current 30-minute delivery interval; receiving a cached update does not make
its temperature newer. Watch fallback state remains persisted separately.

### Refresh water from the watch

Tap the live water complication, then **Update now**. The watch sends a Data Layer
message to its paired phone. The signed-in phone calls main's authenticated
`POST /api/mobile/water/refresh` endpoint. The engine authorizes the configured
watch recipient, downloads the chart and runs vision again, replacing the
60-minute temperature cache on success. It persists and publishes the updated
water event. The phone also sends the endpoint response directly to the watch
as an urgent Data Item, so the requesting watch does not depend on FCM delivery.
The details screen and complication refresh when the reading arrives. Errors
leave the previous measurement and its original retrieval time intact.

### AI reply suggestions for notification rules

In Profile, each notification rule has an **Enable auto reply** switch (off by
 default). It includes a private **Suggested reply:** draft for the notified
user, using the triggering message and up to 40 earlier messages from the same
channel in the preceding 30 minutes (12,000 characters of context maximum).
The draft uses the configured AI backend and does not post to the source
channel. Existing rules remain off until explicitly enabled.

Draft generation runs outside incoming message handling, with a bounded queue,
a 30-second deadline including queue time, and one in-flight notification per
rule. If AI is disabled, unavailable, times out, returns an unusable answer, or
the queue is full, the original notification is delivered without a draft.
Successful private delivery updates the existing cooldown and publishes the
same notification body to the Android inbox/push path.

### Send a reviewed IRC draft through Irssi

IRC rule notifications now carry an optional structured `replyDraft`. In the
Android inbox, **Review reply** opens its original network, channel, sender and
message together with an editable draft. **Send via Irssi** submits the approved
text to main's user-scoped queue. The paired Irssi session must be online under
the configured profile nick and joined to the source channel. Existing plain
notifications remain readable and have no sending action.

The phone's **Irssi** screen creates a single-use pairing code and lists or
revokes agents. See [Irssi installation and operation](../integrations/irssi/README.md)
for `petria@new.airiot.fi`, agent credentials, routing, expiry and recovery rules.
Sending requires an explicit Android approval; generation alone never submits
a channel message. Uncertain sends require checking the channel before retry.
