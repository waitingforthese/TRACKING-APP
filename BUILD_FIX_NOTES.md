# Kotlin compile fix (based on the uploaded GitHub Actions build log)

The compiler reported unresolved references to `ScrollView.LayoutParams` in `MainActivity.kt` line 282.

Fix applied:
- Added `import android.widget.FrameLayout`
- Replaced `ScrollView.LayoutParams(...)` with `FrameLayout.LayoutParams(...)` when adding the main page to the ScrollView.

No other application behavior was intentionally changed. Run GitHub Actions again to confirm the compile result. This ZIP has not been compiled in this environment.
