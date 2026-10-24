# Maha Office Security — Typing History POC V1.3

A local-only Android proof of concept for a custom, user-selected keyboard. There is no admin dashboard, internet permission, server or background upload.

## Scope

- Records text entered **through this custom keyboard only** when the current editor belongs to WhatsApp (`com.whatsapp`), Instagram (`com.instagram.android`) or Facebook (`com.facebook.katana`).
- Requires the user to accept a clear consent notice in the app, enable the keyboard in Android Settings, and select it as the active keyboard.
- Shows a visible status row on the keyboard indicating when history is on and which supported app is active.
- Saves local records encrypted using AES-GCM with an Android Keystore key.
- Includes Pause/Resume, View History, Delete Record, Delete All, and Withdraw Consent.
- Excludes password variations, numeric/phone/date fields, other non-text fields, and editors with `IME_FLAG_NO_PERSONALIZED_LEARNING`.
- Has no Android `INTERNET` permission.

## Important limitations

- Android does **not** provide a permission to read text typed through another keyboard. This app must be enabled and selected as the keyboard. Text typed with Gboard or another IME is not captured.
- V1.2 provides basic English QWERTY and a simple direct-character Marathi layout. It has no phonetic transliteration, suggestions, voice input, or advanced editing; do not assume it replaces a full production keyboard.
- OTP/sensitive-field detection cannot be perfect: some third-party apps use ordinary text fields even for sensitive data. Do not type passwords, OTPs, banking credentials or other secrets while history is enabled. Only use on devices for which you have appropriate authorization and informed consent.
- Only typed text processed by the IME is included; it does not retrieve existing chats, voice messages, copied/pasted text entered outside the IME, messages typed with other keyboards, or confirm whether a draft was sent.
- This repository contains source code; a successful APK build still needs to run in GitHub Actions or Android Studio. This environment did not run an Android/Gradle compile.

## Build with GitHub Actions

1. Upload the contents of this directory to the root of `waitingforthese/TRACKING-APP` (including `.github/workflows/android.yml`).
2. Commit to `main` or `master`, or open GitHub Actions and run **Android Debug APK** manually.
3. Download the `maha-office-security-debug-apk` artifact from the workflow run.

## First-run setup

1. Install and open Maha Office Security.
2. Read the privacy notice and explicitly accept it.
3. Tap **Enable Maha Office Security Keyboard** and enable it in Android settings.
4. Tap **Select Keyboard** and choose Maha Office Security Keyboard.
5. Start typing in one of the three supported apps. The keyboard status row should say `TYPING HISTORY ON` when eligible input is being logged.
6. Use the app's Pause/Resume and Delete controls as needed.


## V1.3 UI diagnostics
The consent action now confirms that consent was saved, shows a brief transition message, and displays an error dialog if the main screen throws an exception. After the dashboard opens, use **View Saved Typing History** to view records stored on this phone.
