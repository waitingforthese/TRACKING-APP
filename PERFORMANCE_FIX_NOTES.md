PIK VIMA KEYBOARD V1.13 — PERFORMANCE FIX NOTES

Changes made:
1. Removed per-character SharedPreferences writes from appendToLog.
2. Switched typing buffer from immutable String concatenation to StringBuilder.
3. Moved encrypted history load/decrypt/JSON serialize/encrypt/commit to a single serial background executor.
4. Added bounded checkpoints during continuous typing (up to about 1.8 seconds between enqueued snapshots) while keeping a short idle debounce.
5. Cached the rendered status state and avoid replacing status backgrounds/content descriptions after each character.
6. Avoid redundant status UI refreshes on ordinary character, space, and backspace presses.
7. Bumped app version to 1.13.0 / versionCode 11.

Build validation: no Android SDK/Gradle executable was available in this environment, so APK compilation could not be run locally. Use GitHub Actions to build and test this ZIP.
