# Repository Workflow

## Feature Branch Promotion

- Start all new feature work on the `develop` branch. Do not implement new features directly on `main`.
- Before editing, verify that the current branch is `develop`. Preserve unrelated worktree changes when switching branches.
- Implement and test the feature on `develop`, then commit it and push `develop` to `origin` so the develop CI pipeline runs.
- Do not merge a feature to `main` until the user explicitly confirms that the feature has been accepted as working.
- After that explicit acceptance, merge `develop` into `main` and push `main` to `origin` so the main CI pipeline runs.
- A successful develop CI run alone is not approval to promote the feature to `main`.

## Frontend Testing

- New or changed React pages, components, and UI behavior must include or update unit/component tests in the same change.
- Write the relevant UI test before implementation where practical, then keep iterating until the test passes.
- Run `npm test` and `npm run build` in `the_bot_web/frontend` before accepting the UI work.
- Frontend tests are part of `npm run build`; a test failure must fail the build and block acceptance.
- Regression fixes must include a test that reproduces the original failure.

## Android and Wear OS Features

- Before changing the phone app or watch app, check `the_bot_android/README.md`, both module Gradle files, and `.github/workflows/ci.yml`. Keep phone (`:app`) and watch (`:wear`) behavior and build steps explicit; a change shared across them can affect APK pairing and installation.
- Work on `develop`, then run the relevant module's unit tests, lint, and debug APK assembly. For a phone change, use `:app:test<Flavor>DebugUnitTest :app:lint<Flavor>Debug :app:assemble<Flavor>Debug`; for a watch change, use `:wear:test<Flavor>DebugUnitTest :wear:lint<Flavor>Debug :wear:assemble<Flavor>Debug`. `<Flavor>` is `Dev` or `Prod`. Run both sets when a feature touches both apps. CI builds and verifies both modules for the selected flavor.
- The phone and watch debug APKs must be signed with the same persistent certificate so Android can install updates in place and the phone/watch companion relationship remains valid. Both Gradle modules use `~/.android/debug.keystore`, alias `androiddebugkey`, with the standard local debug-key passwords. CI restores that keystore from the protected `BOT_ANDROID_DEBUG_KEYSTORE_B64` secret and compares each APK's signer SHA-256 fingerprint against it. Do not generate a replacement key, change either module to a different signing config, or install APKs signed by mismatched certificates. If the key is missing, recover the authorized persistent key; do not clear app data or uninstall to bypass a signature mismatch.
- `dev` and `prod` are separate application IDs (`org.freakz.botclient.dev` vs `org.freakz.botclient`, and corresponding watch IDs). Both flavors currently point the phone app at the main bot instance. Choose the intended flavor and backend deliberately. Local debug builds require JDK 17, Android SDK, Gradle 8.11.1, and the persistent debug keystore at `~/.android/debug.keystore`. Local phone builds may omit Firebase configuration; FCM behavior then cannot be verified. Never commit `google-services.json` or Firebase credentials.
- When devices are attached by USB, first run `adb devices -l` and select the intended device serial. Build the APK, then install it with `adb -s <serial> install -r <apk-path>` to update in place and preserve app data. Example phone APK paths are `the_bot_android/app/build/outputs/apk/dev/debug/app-dev-debug.apk` and `.../prod/debug/app-prod-debug.apk`; the watch test APK is `the_bot_android/wear/build/outputs/apk/dev/debug/wear-dev-debug.apk` (or the `prod` path). Confirm the package/version after installation. Install the matching watch flavor as well as the phone flavor when exercising phone-to-watch data transfer; USB debugging must be enabled on each device. Do not assume that connecting the phone also makes the watch available to `adb`.
- For a connected watch, verify its own serial appears in `adb devices -l`; install the matching watch APK to that serial. If the phone and watch require transport over the paired phone, use the documented watch test procedure in `docs/watch-water-test.md` where applicable. Distinguish successful installation from successful complication selection, data delivery, and a working end-to-end feature.
- Keep Android test, lint, assembly, signing-fingerprint checks, and deployment/installation evidence separate in reports. A successful local build does not establish that CI signed with the persistent key or that both devices run the updated APKs.
