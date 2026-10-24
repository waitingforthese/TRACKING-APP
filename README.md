# पीक विमा — Typing History Keyboard V1.10

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
