# Maha Office Security — Typing History POC V1.7

## Keyboard UI update
- Reworked the custom IME with a modern Android/Gboard-inspired layout: number row, QWERTY keys, shift, backspace, spacebar, enter, emoji page, and a compact toolbar.
- Added a basic Devanagari Marathi character layout. This is a character layout, not phonetic transliteration.
- Toolbar controls switch English/Marathi, open the app dashboard, toggle emojis, switch to another installed keyboard, and hide the keyboard.
- The visible recording-status strip remains at the top of the keyboard.

## Privacy and recording behavior
- Typing history remains opt-in and is stored locally using the existing encrypted store.
- Only text entered through this custom keyboard is in scope; text typed with Gboard or another keyboard is not captured.
- Recording is limited to WhatsApp, WhatsApp Business, Instagram and Facebook, while consent is active and recording is ON.
- Password fields, non-ordinary text inputs, and fields marked `IME_FLAG_NO_PERSONALIZED_LEARNING` remain excluded. The keyboard does not bypass field privacy flags.
- No server upload or admin dashboard is included.

## Test safely
1. Build and install the debug APK on an authorized test device.
2. Accept the clear consent notice, enable the keyboard in Android Settings, and select it.
3. In a test chat in a supported app, type a harmless string such as `POC TEST 123` using this keyboard.
4. Confirm the keyboard's status strip says `RECORDING ON`, then return to Maha Office Security and check `View Saved Typing History` and the last-save status.
5. Delete test records when done.

## Limitations
This is a custom keyboard with a familiar layout, not a pixel-perfect Gboard clone. It does not implement Google services, full predictive typing, cloud suggestions, or swipe typing. Marathi support is a basic character layout. Android apps may mark message fields as not for personalized learning; those fields are deliberately not logged. Never type passwords, OTPs, banking credentials, or private secrets while recording is enabled.

The source has not been compiled into an APK in this environment. Use GitHub Actions to build and test it on a device.
