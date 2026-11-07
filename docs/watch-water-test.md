# Water complication device test

This test checks the right-hand circle on the face in `oneplus_watch2_2.png`.
All temperatures are simulated. Keep air weather in the left circle.
The provider supports SHORT_TEXT and RANGED_VALUE (fixed scale −10 to 45°C).
The watch face determines styling, including whether the icon and ring appear.
No phone/server communication is implemented in this test APK. Its separate application ID
allows installation without replacing the phone client; future Data Layer integration
must align phone/watch application IDs and signing certificates.

## Install

1. Record watch model, Wear OS version, face name/version, and test commit.
2. Enable developer options on the watch by tapping its build number repeatedly.
   Enable ADB/wireless debugging and use its pairing screen. Connect the computer
   and watch to the same network. Pair and connect using the addresses shown:

   ```sh
   adb pair WATCH_IP:PAIRING_PORT
   adb connect WATCH_IP:DEBUG_PORT
   adb devices -l
   adb -s WATCH_IP:DEBUG_PORT install -r the_bot_android/wear/build/outputs/apk/debug/wear-debug.apk
   ```

   Pairing and debugging ports can differ. Enter the pairing code interactively.
3. Open **Bot Water Test** from the watch launcher. It should show `Current: 4.2°C`.
4. Long-press the chosen face, edit its right-hand circular complication, and choose
   **Water temperature (test)** (possibly listed under **Bot Water Test**).

## Acceptance checklist

- [ ] Provider appears in the right-hand slot picker and can be selected.
- [ ] `4.2°C` is readable without clipping; record icon and ring appearance.
- [ ] Tap the complication to open the test screen. Choose `12.5°C`, return to the
      face, and record the update delay. Repeat with `0.0°C` and `4.2°C`.
      Allow time between checks; refresh requests are subject to Wear OS scheduling.
- [ ] Select Unavailable. The provider returns NO_DATA with a `--` placeholder,
      never a numeric zero. Record the face's actual no-data rendering; some faces
      render their own placeholder. Use the launcher if the face disables tapping
      unavailable complications. Restore a valid reading and confirm recovery.
- [ ] Check always-on/ambient mode if enabled, then wake the screen.
- [ ] Switch to another face and back; the selected reading remains.
- [ ] Reboot the watch; the saved reading remains available after unlock.
- [ ] Air weather and the other indicators remain configured as before.

If the provider is absent, test another slot and a face that accepts third-party
complications. Capture logs to distinguish slot type restrictions from service errors.
Do not replace the user's preferred face permanently without discussing the result.

## Evidence

Save test commit, APK SHA-256, face/version, Wear OS version, observed update delays,
and pass/fail for each item. Capture logs and a screenshot using the device selector:

```sh
adb -s WATCH_IP:DEBUG_PORT logcat -d -s BotWaterTest:I AndroidRuntime:E > water-test-log.txt
adb -s WATCH_IP:DEBUG_PORT exec-out screencap -p > water-test-face.png
sha256sum the_bot_android/wear/build/outputs/apk/debug/wear-debug.apk
```

Logs include requested complication type, instance ID, reading, and refresh request time.
Unit tests verify formatting, missing/invalid data, both data types, tap actions and
local persistence. They do not establish compatibility with a physical watch face.

After testing, disable wireless debugging. To remove the test, select another
complication provider and uninstall **Bot Water Test** from the watch.
Live SYKE retrieval, push delivery, phone synchronization and their recovery tests
are a subsequent integration stage.

## Text-only comparison

On the first physical test, the right circle requested RANGED_VALUE with 4.2°C,
but displayed only the droplet and gauge. Version 0.2 adds **Water temperature (text)**,
which advertises only SHORT_TEXT, alongside the original provider. Select this new
provider in the same slot and repeat the value/update checks above. Both providers
share the saved test reading and receive refresh requests from the test screen.
If the new provider is absent in that slot, the slot may not support SHORT_TEXT;
try another slot to distinguish a face restriction from a provider failure.
If present, verify the numeric temperature visually; supplying SHORT_TEXT alone
does not prove this face renders it. Logs identify which provider/type was requested.
