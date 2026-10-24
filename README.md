# Maha Office Security — Gboard-style Keyboard POC V1.9

## Keyboard layout update
- Removed the full-width recording banner above the keyboard toolbar to make the keyboard more compact and closer to a Gboard-style layout.
- Retained a compact visible `● REC` / `● PAUSE` / `● OFF` / `● READY` toolbar indicator so people using the device can still tell when typing history is active or paused. Tapping it opens the app.
- Reserved bottom safe space for Android navigation buttons using window insets, with a 24dp minimum spacer fallback, so the bottom keyboard row should not sit underneath the system navigation bar.
- Keeps rounded white number/QWERTY keys, lavender modifier keys, emoji, language, symbol, space and enter controls.

## Language / features
- The keyboard is inspired by common Android keyboard layouts, not a pixel-perfect clone of Gboard.
- Marathi remains a basic Devanagari character layout; it is not phonetic transliteration.
- This project does not include Google's proprietary prediction, cloud suggestions, swipe typing or full autocorrect.

## Privacy behavior
- Typing history remains opt-in, local-only, and encrypted with the existing Android Keystore-backed store.
- Only text entered through this custom keyboard is in scope; text typed with Gboard or another keyboard is not captured.
- Recording is limited to WhatsApp, WhatsApp Business, Instagram and Facebook while updated consent is active and recording is ON.
- Password fields, non-ordinary text inputs and fields marked `IME_FLAG_NO_PERSONALIZED_LEARNING` remain excluded. The keyboard does not bypass field privacy flags.
- No server upload or admin dashboard is included.

## Device test
1. Build and install the debug APK on an authorized test device.
2. Accept the consent notice, enable the keyboard in Android Settings and select it.
3. In a test chat in a supported app, type a harmless string such as `POC TEST 123` using this keyboard.
4. Confirm the compact toolbar status shows `● REC` only when logging is eligible; otherwise it shows `● OFF` / `● PAUSE` / `● READY`.
5. Return to Maha Office Security and check `View Saved Typing History`.
6. Delete test records when done.

## Build status
The source has not been compiled into an APK in this environment. Use GitHub Actions to build it and validate the layout on a device before relying on it.
