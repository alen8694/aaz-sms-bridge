# AAZ SMS Bridge

Lightweight Android SMS receiver and HTTPS bridge for AAZ Connect.

## Build

Requires JDK 17 and Android SDK 35.

```sh
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Security defaults

- Forwarding is disabled until explicitly enabled.
- The REST endpoint must use HTTPS.
- The bridge key is entered on the device and is never embedded in source code.
- Successfully delivered SMS IDs are suppressed for seven days.
- The destination Group/Room is configured only on the WordPress bridge settings page.
- Every incoming sender is synced to WordPress first without its SMS body.
- The complete SMS is uploaded only when `/sender-sync` confirms `routed: true`; failures are closed.
- After server routing approval, an optional local smart filter can block the message, remove configured keywords, remove matching sentences/segments, or remove matching lines before `/inbox`.
- A visual sender-rule manager supports multiple rules per sender with sender/action dropdowns, add/edit/delete controls, and `block`, `remove_keyword`, `remove_sentence`, `remove_line`, or `remove_range` actions.
- A separate Keyword Based Forward manager forwards once when any configured case-insensitive body keyword matches, regardless of sender routing; the original sender is preserved.
- A visual keyword-specific filter manager supports multiple body-keyword rules with the same block/remove actions as sender-specific rules.
- Incoming bodies are encrypted with an Android Keystore AES-GCM key in a persistent local queue; WorkManager retries transient network/server failures.
- The in-app delivery log stores at most 20 metadata-only status entries and never stores the SMS body, bridge secret, or OTP values.
