# Maha Office Security — Typing History POC V1.5

## Diagnostics update
- The keyboard's on-screen status now states why a field is not eligible for recording (for example, app not in scope, unsupported input type, or the field's `IME_FLAG_NO_PERSONALIZED_LEARNING` privacy flag).
- The main screen displays the most recent keyboard status and last local-save result.
- Local save failures are surfaced instead of being silently ignored.
- The existing privacy exclusion remains in place; this version does not bypass fields marked as not for personalized learning.

## Test safely
1. Install on a test phone and accept the visible notice.
2. Enable and select the keyboard in Android Settings.
3. In a test chat, type only a harmless test string such as `POC TEST 123`.
4. Observe the status text at the top of the keyboard, then return to this app and read `Last keyboard status` and `Last save result`.
5. Delete test records when done.

This ZIP contains source code and a GitHub Actions workflow. It has not been compiled into an APK in this environment. Use an app's own privacy controls and do not enter passwords, OTPs, banking credentials, or private information while recording is enabled.
