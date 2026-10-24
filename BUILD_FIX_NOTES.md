# V1.4 Android runtime fix

The dashboard previously crashed with `SecurityException` while reading `Settings.Secure.ENABLED_INPUT_METHODS` on targetSdk 35. Android documents that this key is not readable to apps targeting API 34+, and recommends `InputMethodManager.getEnabledInputMethodList()`.

Fixes:
- Detect enabled keyboards through `InputMethodManager.enabledInputMethodList`.
- On Android 14+ detect the selected keyboard through `InputMethodManager.currentInputMethodInfo`.
- On older Android versions use `DEFAULT_INPUT_METHOD` with a `SecurityException` guard.
- Increment `versionCode` to 3 and `versionName` to 1.2.0.

This source change has not been compiled on an Android SDK in this environment. Run GitHub Actions to confirm.
