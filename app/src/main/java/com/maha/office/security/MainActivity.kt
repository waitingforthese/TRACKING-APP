package com.maha.office.security

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private lateinit var page: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 94, 86)
        window.navigationBarColor = Color.WHITE
        if (hasCurrentConsent()) showDashboard() else showConsent()
    }

    override fun onResume() {
        super.onResume()
        if (::page.isInitialized && hasCurrentConsent()) refreshStatusIfPresent()
    }

    private fun showConsent() {
        page = newPage()
        page.addView(brandLogo(72))
        page.addView(title("पीक विमा"))
        page.addView(subtitle("Typing History Keyboard • On-device only • V1.10"))
        page.addView(space(18))
        page.addView(sectionTitle("Consent and keyboard setup"))
        page.addView(space(8))
        page.addView(body(
            "This app includes a custom keyboard with a familiar Android QWERTY layout inspired by modern keyboards. It is not Gboard and does not include all Gboard features such as Google search, cloud suggestions or full swipe typing. If you enable and select it, the keyboard records text entered through THIS keyboard only when the current app is WhatsApp, WhatsApp Business, Instagram or Facebook. The text is saved in this app on this phone, encrypted with an Android Keystore key. There is no server, cloud upload or admin dashboard in this version."
        ))
        page.addView(space(10))
        page.addView(body(
            "Sensitive-input safeguards: password fields are excluded; numeric-only input fields and fields marked as not for personalized learning are not logged. Android apps do not consistently identify every OTP or sensitive field, so this is not a guarantee that every sensitive field can be detected. Do not type passwords, OTPs, banking credentials or personal secrets while recording is on."
        ))
        page.addView(space(10))
        page.addView(body(
            "Important: Android does NOT grant keyboard-recording permission automatically during installation. After consenting, the user must manually enable this keyboard in Android Settings and select it from the keyboard picker. Text typed with Gboard or another keyboard is not captured. A small visible status dot indicates whether eligible typing can be saved; the dashboard provides the full status. Use only on authorized devices after giving users a suitable written notice."
        ))
        page.addView(space(14))
        val consent = CheckBox(this).apply {
            text = "I have read this notice and explicitly agree that text typed through this keyboard in WhatsApp, WhatsApp Business, Instagram or Facebook may be saved locally. I understand how to pause recording and delete the saved history."
            textSize = 15f
            setTextColor(INK)
            setPadding(dp(2), dp(8), dp(2), dp(8))
        }
        page.addView(consent)
        page.addView(space(12))
        val accept = button("Accept and Continue") {}
        accept.isEnabled = false
        consent.setOnCheckedChangeListener { _, checked -> accept.isEnabled = checked }
        accept.setOnClickListener {
            if (!consent.isChecked) {
                Toast.makeText(this, "Please tick the consent checkbox first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val saved = prefs.edit()
                .putBoolean(KEY_CONSENT, true)
                .putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
                .putBoolean(KEY_RECORDING, true)
                .putLong(KEY_CONSENT_AT, System.currentTimeMillis())
                .commit()
            if (!saved) {
                Toast.makeText(this, "Could not save consent. Please try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            accept.isEnabled = false
            accept.text = "Opening main screen..."
            Toast.makeText(this, "Consent saved. Opening main screen...", Toast.LENGTH_SHORT).show()
            try {
                showDashboard()
            } catch (e: Exception) {
                android.util.Log.e("MahaOfficeSecurity", "Failed to open dashboard after consent", e)
                accept.isEnabled = true
                accept.text = "Retry: Open main screen"
                AlertDialog.Builder(this)
                    .setTitle("Could not open main screen")
                    .setMessage("${e.javaClass.simpleName}: ${e.message ?: "No error details"}")
                    .setPositiveButton("Close", null)
                    .show()
            }
        }
        page.addView(accept)
        page.addView(space(8))
        page.addView(caption("No text records are created by accepting this screen alone. Records can only be created when this custom keyboard is enabled, selected, and used in WhatsApp, WhatsApp Business, Instagram or Facebook."))
    }

    private fun showDashboard() {
        page = newPage()
        page.addView(brandLogo(60))
        page.addView(title("पीक विमा"))
        page.addView(subtitle("Typing History Keyboard • On-device only"))
        page.addView(space(16))
        val status = TextView(this).apply {
            tag = STATUS_TAG
            textSize = 14f
            setTextColor(INK)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(Color.rgb(237, 247, 245), Color.rgb(207, 231, 226), 14)
        }
        page.addView(status, matchWrap())
        page.addView(space(12))
        page.addView(button("Enable पीक विमा Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        page.addView(space(8))
        page.addView(button("Select Keyboard") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        })
        page.addView(space(8))
        page.addView(button(if (isRecording()) "Pause Typing History" else "Resume Typing History") {
            val next = !isRecording()
            prefs.edit().putBoolean(KEY_RECORDING, next).commit()
            Toast.makeText(this, if (next) "Typing history resumed" else "Typing history paused", Toast.LENGTH_SHORT).show()
            showDashboard()
        })
        page.addView(space(8))
        page.addView(button("View Saved Typing History") { showHistory() })
        page.addView(space(8))
        page.addView(button("Delete All Typing History") { confirmDeleteAll() })
        page.addView(space(8))
        page.addView(button("Privacy Notice") { showPrivacyNotice() })
        page.addView(space(8))
        page.addView(button("Withdraw Consent and Delete Data") { confirmWithdraw() })
        page.addView(space(18))
        page.addView(sectionTitle("How it works"))
        page.addView(space(5))
        page.addView(body(
            "1. Enable the custom keyboard in Android Settings.\n2. Select it as the active keyboard.\n3. Type in WhatsApp, WhatsApp Business, Instagram or Facebook.\n4. While Recording is ON, eligible text appears in this app's local history.\n5. Pause recording or delete history whenever required."
        ))
        page.addView(space(10))
        page.addView(caption("Only text entered through this keyboard is in scope. It does not read old chat history, messages typed with another keyboard, voice messages, or content already present in the apps."))
        refreshStatusIfPresent()
    }

    private fun refreshStatusIfPresent() {
        val status = page.findViewWithTag<TextView>(STATUS_TAG) ?: return
        val enabled = isKeyboardEnabled()
        val selected = isKeyboardSelected()
        val recording = isRecording()
        status.text = buildString {
            append("Consent: active\n")
            append("Keyboard enabled in Android: ").append(if (enabled) "YES" else "NO").append("\n")
            append("Keyboard currently selected: ").append(if (selected) "YES" else "NO").append("\n")
            append("Typing history: ").append(if (recording) "ON" else "PAUSED").append("\n")
            append("Scope: WhatsApp, WhatsApp Business, Instagram and Facebook only\n")
            append("Storage: encrypted on this phone; no upload\n")
            append("Last keyboard status: ").append(prefs.getString(KEY_LAST_IME_STATUS, "No keyboard input status received yet")).append("\n")
            append("Last save result: ").append(prefs.getString(KEY_LAST_SAVE_STATUS, "No save attempt yet"))
        }
        status.background = rounded(
            if (recording && enabled && selected) Color.rgb(237, 247, 245) else Color.rgb(255, 248, 231),
            if (recording && enabled && selected) Color.rgb(207, 231, 226) else Color.rgb(235, 216, 164),
            14
        )
    }

    private fun hasCurrentConsent(): Boolean = prefs.getBoolean(KEY_CONSENT, false) && prefs.getInt(KEY_CONSENT_VERSION, 0) >= CURRENT_CONSENT_VERSION

    private fun isRecording(): Boolean = hasCurrentConsent() && prefs.getBoolean(KEY_RECORDING, false)

    /**
     * Android 14+ blocks direct reads of Settings.Secure.ENABLED_INPUT_METHODS for apps
     * targeting API 34+. Use the supported InputMethodManager API instead.
     */
    private fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val expectedService = TypingHistoryImeService::class.java.name
        return imm.enabledInputMethodList.any { info ->
            info.packageName == packageName && info.serviceName == expectedService
        }
    }

    /**
     * Prefer InputMethodManager on Android 14+. On older releases, fall back to the
     * documented selected-input-method setting, guarded so a platform restriction cannot
     * crash the home screen.
     */
    private fun isKeyboardSelected(): Boolean {
        val expected = ComponentName(this, TypingHistoryImeService::class.java)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val current = imm.currentInputMethodInfo ?: return false
            return current.packageName == expected.packageName && current.serviceName == expected.className
        }
        return try {
            val selected = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            ).orEmpty()
            ComponentName.unflattenFromString(selected) == expected
        } catch (_: SecurityException) {
            false
        }
    }

    private fun showHistory() {
        val records = TypingLogStore.load(this)
        if (records == null) {
            AlertDialog.Builder(this)
                .setTitle("Typing history unavailable")
                .setMessage("The encrypted history could not be opened. No records were overwritten. Do not clear app data if you need to preserve the existing encrypted file.")
                .setPositiveButton("Close", null)
                .show()
            return
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        if (records.isEmpty()) {
            list.addView(body("No typing history saved yet. Enable and select this keyboard, then type in a supported app with history ON."))
        } else {
            records.forEach { record ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    background = rounded(Color.WHITE, Color.rgb(220, 229, 229), 10)
                }
                card.addView(sectionTitle(appLabel(record.packageName)))
                card.addView(caption("Updated: ${formatTimestamp(record.updatedAt)}"))
                card.addView(space(5))
                card.addView(body(record.text))
                card.addView(space(5))
                card.addView(button("Delete this record") {
                    if (TypingLogStore.delete(this, record.id)) {
                        list.removeView(card)
                        Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Could not delete record", Toast.LENGTH_SHORT).show()
                    }
                })
                list.addView(card, matchWrap())
                list.addView(space(8))
            }
        }
        val scroll = ScrollView(this).apply { addView(list) }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Typing History (${records.size})")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .setNeutralButton("Delete All", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete all typing history?")
                    .setMessage("This permanently deletes all locally saved typing records from this phone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete All") { _, _ ->
                        if (TypingLogStore.deleteAll(this)) {
                            dialog.dismiss()
                            showHistory()
                        } else Toast.makeText(this, "Could not delete records", Toast.LENGTH_LONG).show()
                    }.show()
            }
        }
        dialog.show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Delete all typing history?")
            .setMessage("All locally saved typing records will be permanently deleted. Recording will remain ${if (isRecording()) "ON" else "PAUSED"}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete All") { _, _ ->
                if (TypingLogStore.deleteAll(this)) Toast.makeText(this, "All typing history deleted", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "Could not delete history", Toast.LENGTH_LONG).show()
            }.show()
    }

    private fun confirmWithdraw() {
        AlertDialog.Builder(this)
            .setTitle("Withdraw consent?")
            .setMessage("Recording will stop and locally saved typing history will be deleted. You should also switch to another keyboard in Android settings.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Withdraw and Delete") { _, _ ->
                prefs.edit()
                    .putBoolean(KEY_RECORDING, false)
                    .remove(KEY_CONSENT)
                    .remove(KEY_CONSENT_VERSION)
                    .remove(KEY_CONSENT_AT)
                    .commit()
                TypingLogStore.deleteAll(this)
                showConsent()
            }.show()
    }

    private fun showPrivacyNotice() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Notice")
            .setMessage(
                "The custom keyboard saves the text typed through it only when the active package is WhatsApp, WhatsApp Business, Instagram or Facebook and recording is enabled. Password variations, numeric input fields, and fields marked IME_FLAG_NO_PERSONALIZED_LEARNING are excluded. No app can guarantee that all sensitive fields are correctly identified. Stored records are encrypted using an Android Keystore key and remain local to this installation. There is no internet permission, server sync or admin access in this POC. Users can pause recording, delete records or withdraw consent."
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun appLabel(packageName: String): String = when (packageName) {
        "com.whatsapp" -> "WhatsApp"
        "com.whatsapp.w4b" -> "WhatsApp Business"
        "com.instagram.android" -> "Instagram"
        "com.facebook.katana" -> "Facebook"
        else -> packageName
    }

    private fun formatTimestamp(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(millis))

    private fun brandLogo(sizeDp: Int): ImageView = ImageView(this).apply {
        setImageResource(R.drawable.pik_vima_logo)
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = "पीक विमा logo"
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply {
            gravity = Gravity.CENTER
            bottomMargin = dp(4)
        }
    }

    private fun newPage(): LinearLayout {
        val scroll = ScrollView(this)
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.rgb(247, 249, 249))
        }
        scroll.addView(page, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
        return page
    }

    private fun title(value: String) = TextView(this).apply {
        text = value; textSize = 26f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD)
    }

    private fun subtitle(value: String) = TextView(this).apply {
        text = value; textSize = 14f; setTextColor(Color.rgb(94, 111, 115))
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value; textSize = 17f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD)
    }

    private fun body(value: String) = TextView(this).apply {
        text = value; textSize = 14f; setTextColor(Color.rgb(49, 64, 68)); setLineSpacing(dp(3).toFloat(), 1.0f)
    }

    private fun caption(value: String) = TextView(this).apply {
        text = value; textSize = 12f; setTextColor(Color.rgb(105, 120, 123)); setLineSpacing(dp(2).toFloat(), 1.0f)
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label; textSize = 14f; isAllCaps = false; setTextColor(Color.WHITE)
        background = rounded(Color.rgb(7, 110, 100), Color.rgb(7, 110, 100), 12)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        setOnClickListener { action() }
    }

    private fun space(heightDp: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun rounded(fillColor: Int, strokeColor: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(fillColor); setStroke(dp(1), strokeColor); cornerRadius = dp(radiusDp).toFloat()
    }

    companion object {
        private const val PREFS = "office_keyboard_prefs"
        private const val KEY_CONSENT = "consent_accepted"
        private const val KEY_CONSENT_AT = "consent_accepted_at"
        private const val KEY_CONSENT_VERSION = "consent_scope_version"
        private const val CURRENT_CONSENT_VERSION = 2
        private const val KEY_RECORDING = "recording_enabled"
        private const val KEY_LAST_IME_STATUS = "last_ime_status"
        private const val KEY_LAST_SAVE_STATUS = "last_save_status"
        private const val STATUS_TAG = "keyboard_status"
        private val INK = Color.rgb(16, 42, 45)
    }
}
