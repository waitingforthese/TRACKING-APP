# पीक विमा — Typing History Keyboard V1.13

## UI / branding update
- Uses the user-provided wheat-and-hands logo in the keyboard toolbar and app screen.
- App display branding: “पीक विमा”; Android app/IME labels also identify it as a typing-history keyboard.
- Removed the literal `REC` text. The keyboard retains a small colored status dot, with status details and consent shown in the app.
- Keeps the Gboard-inspired QWERTY layout, number row, emoji, symbol, language, space and enter controls.
- Keeps safe bottom spacing for Android navigation.

## Privacy behavior
- Typing history is opt-in, local-only, and encrypted with the existing Android Keystore-backed store.
- Only text entered through this custom keyboard is in scope; Gboard/SwiftKey input is not captured.
- Recording is limited to WhatsApp, WhatsApp Business, Instagram and Facebook while current consent is active and history is ON.
- Password fields, non-ordinary text inputs and fields marked `IME_FLAG_NO_PERSONALIZED_LEARNING` remain excluded. The keyboard does not bypass field privacy flags.
- No server upload or admin dashboard is included. The consent screen describes local typing-history capture; use only on authorized devices with clear notice and consent.

## Build / test
1. Build the Debug APK using GitHub Actions.
2. Install on an authorized test device, read and accept the consent notice, then enable/select the keyboard in Android Settings.
3. Test only with a harmless string such as `PIK VIMA TEST 123` in a supported app.
4. Check the small status dot and the detailed status in the app dashboard.
5. Open `View Saved Typing History` and delete test records when finished.

## Build status
Source changes are prepared; the APK has not been compiled in this environment. Validate using the GitHub Actions workflow before installing.


## V1.11 UI update
- Removed the crop-insurance logo tile from the keyboard toolbar.
- Added ripple and brief press-in/press-out feedback to keyboard keys and toolbar controls.
- Kept the app and service labels clear that this is a typing-history keyboard; the app should not be disguised as an unrelated insurance utility.
- The toolbar shows a text status such as “History ON”, “History PAUSED”, or “Outside scope” instead of a REC badge.


## V1.13 app scope confirmation
- The input-method allowlist contains the four requested Android package IDs: `com.whatsapp` (WhatsApp), `com.whatsapp.w4b` (WhatsApp Business), `com.instagram.android` (Instagram), and `com.facebook.katana` (Facebook).
- The visible keyboard status remains enabled and reports states such as “History ON”, “History PAUSED”, “Outside scope”, or a protected-field status.
- The consent notice explicitly lists all four supported apps. Capture applies only to text entered using this custom keyboard, only while consent/history are enabled, and only for eligible ordinary text fields. Password and privacy-marked fields remain excluded.
- Version bumped to 1.12.0 / versionCode 10. Source package prepared; APK not yet verified by a successful GitHub Actions run.


## V1.13 typing-performance changes
- Keyboard status UI is only redrawn when its state changes, not after every character.
- Per-character SharedPreferences writes have been removed from the input path.
- Typing buffer uses StringBuilder to avoid repeated string allocations.
- Encrypted JSON history writes run on a single background executor rather than the IME UI thread.
- History snapshots are checkpointed after a short pause and during continuous typing.
- Recording remains opt-in, app-scoped, visible, local-only and excludes protected/password fields.
