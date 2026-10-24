# UI update notes — POC V1.9

- Removed the wide status banner above the keyboard toolbar.
- Kept a compact, readable `● REC` / `● PAUSE` / `● OFF` / `● READY` status control in the toolbar; tapping it opens the app. The active-recording state is not hidden completely.
- Added navigation-bar window-inset handling plus a 24dp minimum bottom safe area so the last key row should stay above the Android navigation buttons.
- Bumped versionCode to 7 and versionName to 1.9.0.
- This source has not been compiled into an APK in this environment; validate with the included GitHub Actions workflow and test on a device.
