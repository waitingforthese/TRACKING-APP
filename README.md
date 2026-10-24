# Maha Office Security — Gboard-style Keyboard POC V1.8

## Keyboard appearance update
- Updated the keyboard surface to a pale lavender/white style inspired by common Android keyboards.
- Added individually rounded white number and QWERTY keys, larger key spacing, blue-lavender modifier keys, and a familiar bottom row with symbols, emoji, language, space, period and enter.
- Simplified the top toolbar with working history/settings, emoji, symbols, language, keyboard-switch and hide actions.
- The layout is inspired by Gboard but is not a pixel-perfect clone and does not implement Google's proprietary services, swipe typing, full autocorrect or cloud predictions.
- Marathi remains a basic Devanagari character layout; it is not phonetic transliteration.

## Visible recording status
- The status strip deliberately remains clearly visible while recording is active.
- The active status is bilingual: `RECORDING ON · 正在记录` (Chinese: “recording in progress”) followed by the supported app name. It is not Chinese-only, so people using the device can clearly understand that recording is active.

## Privacy behavior
- Typing history remains opt-in, local-only, and encrypted with the existing Android Keystore-backed store.
- Only text entered through this custom keyboard is in scope; text typed with Gboard or another keyboard is not captured.
- Recording is limited to WhatsApp, WhatsApp Business, Instagram and Facebook, while updated consent is active and recording is ON.
- Password fields, non-ordinary text inputs and fields marked `IME_FLAG_NO_PERSONALIZED_LEARNING` remain excluded. The keyboard does not bypass field privacy flags.
- No server upload or admin dashboard is included.

## Test
1. Build and install the debug APK on an authorized test device.
2. Accept the clear consent notice, enable the keyboard in Android Settings and select it.
3. In a test chat in a supported app, type a harmless string such as `POC TEST 123` using this keyboard.
4. Confirm the keyboard status visibly says `RECORDING ON · 正在记录`, then return to Maha Office Security and check `View Saved Typing History` and the last-save status.
5. Delete test records when done.

## Build status
The source has not been compiled into an APK in this environment. Use GitHub Actions to build it and then test the APK on a device before relying on it.
