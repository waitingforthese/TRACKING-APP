# Build and UI update notes — POC V1.7

- Updated `TypingHistoryImeService.kt` to use a rounded-key, Gboard-inspired Android layout with number row, QWERTY rows, symbol mode, basic Marathi character mode, emoji mode, visible recording strip, and functional toolbar controls.
- Preserved app allowlist and field privacy exclusions; no bypass of `IME_FLAG_NO_PERSONALIZED_LEARNING` was added.
- Bumped `versionCode` to 5 and `versionName` to 1.4.0 so Android can distinguish this build from the previous release.
- This source has not been compiled in the current environment because Android SDK/Gradle artifacts are unavailable. Run GitHub Actions and test the generated APK before relying on it.
