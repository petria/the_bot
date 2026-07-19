# The Bot Android Client

Independent Kotlin/Compose client for one configured `bot-web` instance.

## Build targets

The `dev` and `prod` flavors use separate application IDs and base URLs. Set
`THE_BOT_WEB_BASE_URL` in `app/build.gradle.kts` or override it in CI. The app
only calls bot-web; it never connects directly to bot-engine or bot-io.

Firebase configuration is intentionally external. Put the matching
`google-services.json` in `app/src/<flavor>/` when enabling FCM builds. Do not
commit Firebase credentials.

This project is not a Maven module. Build it with the Android Gradle toolchain:

```text
./gradlew :app:assembleDevDebug
./gradlew :app:assembleProdRelease
```
