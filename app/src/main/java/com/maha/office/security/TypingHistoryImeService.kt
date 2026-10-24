package com.maha.office.security

import android.inputmethodservice.InputMethodService
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.UUID

/**
 * A visible, user-selected keyboard. It only records eligible text input in the three stated apps,
 * after in-app consent. It has no network permissions or upload code.
 */
class TypingHistoryImeService : InputMethodService() {
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private var root: LinearLayout? = null
    private var statusView: TextView? = null
    private var shiftOn = true
    private var numbersMode = false
    private var marathiMode = false
    private val persistHandler = Handler(Looper.getMainLooper())
    private val persistRunnable = Runnable { persistBuffer() }
    private var activePackage: String = ""
    private var loggingAllowed = false
    private var buffer: String = ""
    private var sessionId: String = UUID.randomUUID().toString()
    private var sessionStartedAt: Long = System.currentTimeMillis()

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(6))
            setBackgroundColor(Color.rgb(225, 231, 236))
        }
        statusView = TextView(this).apply {
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(30, 48, 52))
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
        root!!.addView(statusView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(28)))
        setKeyboardRows()
        updateStatus()
        return root!!
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        persistHandler.removeCallbacks(persistRunnable)
        if (loggingAllowed && buffer.isNotEmpty()) persistBuffer()
        super.onStartInput(attribute, restarting)
        activePackage = attribute.packageName ?: ""
        loggingAllowed = shouldLog(attribute)
        buffer = ""
        sessionId = UUID.randomUUID().toString()
        sessionStartedAt = System.currentTimeMillis()
        updateStatus()
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateStatus()
        setKeyboardRows()
    }

    override fun onFinishInput() {
        persistHandler.removeCallbacks(persistRunnable)
        if (loggingAllowed && buffer.isNotEmpty()) persistBuffer()
        super.onFinishInput()
        loggingAllowed = false
        activePackage = ""
        updateStatus()
    }

    private fun loggingBlockReason(info: EditorInfo?): String? {
        val consent = prefs.getBoolean(KEY_CONSENT, false)
        if (!consent) return "consent not active"
        if (!prefs.getBoolean(KEY_RECORDING, true)) return "history is paused"
        if (info == null) return "no text field focused"
        if (info.packageName !in TARGET_PACKAGES) return "current app is outside the allowed scope"
        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (inputClass != InputType.TYPE_CLASS_TEXT) return "input type is not ordinary text"
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) return "password field excluded"
        // Respect a field's own privacy flag; do not bypass it to collect text.
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
            return "field is marked not for personalized learning"
        }
        return null
    }

    private fun shouldLog(info: EditorInfo): Boolean = loggingBlockReason(info) == null

    private fun updateStatus() {
        val status = statusView ?: return
        val consent = prefs.getBoolean(KEY_CONSENT, false)
        val recording = prefs.getBoolean(KEY_RECORDING, true)
        val info = currentInputEditorInfo
        val label = appLabel(info?.packageName ?: activePackage)
        val blockReason = loggingBlockReason(info)
        val message = when {
            !consent -> "History disabled • give consent in app"
            !recording -> "TYPING HISTORY PAUSED"
            blockReason != null -> "Not recording • $label / $blockReason"
            !loggingAllowed -> "Not recording • $label / input not ready"
            else -> "● TYPING HISTORY ON • $label"
        }
        status.text = message
        status.setTypeface(null, if (message.startsWith("● TYPING HISTORY ON")) Typeface.BOLD else Typeface.NORMAL)
        status.setTextColor(if (message.startsWith("● TYPING HISTORY ON")) Color.rgb(0, 100, 65) else Color.rgb(65, 75, 80))
        val oldMessage = prefs.getString(KEY_LAST_IME_STATUS, null)
        if (oldMessage != message) prefs.edit().putString(KEY_LAST_IME_STATUS, message).apply()
    }

    private fun setKeyboardRows() {
        val container = root ?: return
        // Retain the first status row while rebuilding the keys.
        while (container.childCount > 1) container.removeViewAt(1)
        val rows: List<List<Key>> = if (numbersMode) {
            listOf(
                "1234567890".map { Key(it.toString(), KeyAction.CHAR) },
                listOf(Key("@", KeyAction.CHAR), Key("#", KeyAction.CHAR), Key("$", KeyAction.CHAR), Key("%", KeyAction.CHAR), Key("&", KeyAction.CHAR), Key("-", KeyAction.CHAR), Key("+", KeyAction.CHAR), Key("(", KeyAction.CHAR), Key(")", KeyAction.CHAR), Key("/", KeyAction.CHAR)),
                listOf(Key("ABC", KeyAction.MODE), Key("*", KeyAction.CHAR), Key("'", KeyAction.CHAR), Key(":", KeyAction.CHAR), Key(";", KeyAction.CHAR), Key("!", KeyAction.CHAR), Key("?", KeyAction.CHAR), Key("⌫", KeyAction.BACKSPACE)),
                listOf(Key("🌐", KeyAction.SWITCH), Key(",", KeyAction.CHAR), Key("space", KeyAction.SPACE, 4f), Key(".", KeyAction.CHAR), Key("↵", KeyAction.ENTER))
            )
        } else if (marathiMode) {
            listOf(
                listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ए", "ऐ", "ओ", "औ").map { Key(it, KeyAction.CHAR) },
                listOf("क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ").map { Key(it, KeyAction.CHAR) },
                listOf("ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध", "न").map { Key(it, KeyAction.CHAR) },
                listOf("प", "फ", "ब", "भ", "म", "य", "र", "ल", "व", "श").map { Key(it, KeyAction.CHAR) },
                listOf("ष", "स", "ह", "ळ", "क्ष", "त्र", "ज्ञ", "श्र", "ा", "ि", "ी").map { Key(it, KeyAction.CHAR) },
                listOf("ु", "ू", "ृ", "े", "ै", "ो", "ौ", "्", "ं", "ः", "ँ").map { Key(it, KeyAction.CHAR) },
                listOf(Key("?123", KeyAction.MODE), Key("ABC", KeyAction.LANGUAGE), Key(",", KeyAction.CHAR), Key("🌐", KeyAction.SWITCH), Key("space", KeyAction.SPACE, 3f), Key("।", KeyAction.CHAR), Key("↵", KeyAction.ENTER))
            )
        } else {
            listOf(
                "qwertyuiop".map { Key(it.toString(), KeyAction.CHAR) },
                "asdfghjkl".map { Key(it.toString(), KeyAction.CHAR) },
                listOf(Key("⇧", KeyAction.SHIFT, 1.4f)) + "zxcvbnm".map { Key(it.toString(), KeyAction.CHAR) } + listOf(Key("⌫", KeyAction.BACKSPACE, 1.4f)),
                listOf(Key("?123", KeyAction.MODE), Key("अ", KeyAction.LANGUAGE), Key(",", KeyAction.CHAR), Key("🌐", KeyAction.SWITCH), Key("space", KeyAction.SPACE, 3f), Key(".", KeyAction.CHAR), Key("↵", KeyAction.ENTER))
            )
        }
        rows.forEach { rowKeys ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            rowKeys.forEach { key ->
                val label = if (key.action == KeyAction.CHAR && key.value.length == 1 && key.value[0].isLetter() && shiftOn) key.value.uppercase() else key.value
                val button = Button(this).apply {
                    text = label
                    textSize = when (key.action) {
                        KeyAction.CHAR -> 17f
                        KeyAction.SPACE -> 11f
                        else -> 13f
                    }
                    isAllCaps = false
                    setTextColor(Color.rgb(30, 42, 45))
                    background = keyBackground(key.action)
                    setPadding(dp(1), dp(1), dp(1), dp(1))
                    minWidth = 0
                    minimumWidth = 0
                    setOnClickListener { handleKey(key) }
                }
                val keyHeight = if (marathiMode && !numbersMode) 39 else 44
                val lp = LinearLayout.LayoutParams(0, dp(keyHeight), key.weight)
                lp.setMargins(dp(2), dp(2), dp(2), dp(2))
                row.addView(button, lp)
            }
            val rowHeight = if (marathiMode && !numbersMode) 43 else 48
            container.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(rowHeight)))
        }
    }

    private fun handleKey(key: Key) {
        refreshLoggingEligibility()
        val ic = currentInputConnection ?: return
        when (key.action) {
            KeyAction.CHAR -> {
                val value = if (key.value.length == 1 && key.value[0].isLetter() && shiftOn) key.value.uppercase() else key.value
                if (ic.commitText(value, 1)) {
                    appendToLog(value)
                } else {
                    prefs.edit().putString(KEY_LAST_SAVE_STATUS, "InputConnection rejected character; nothing saved").apply()
                }
                if (shiftOn) { shiftOn = false; setKeyboardRows() }
            }
            KeyAction.SPACE -> {
                if (ic.commitText(" ", 1)) appendToLog(" ")
                else prefs.edit().putString(KEY_LAST_SAVE_STATUS, "InputConnection rejected space; nothing saved").apply()
            }
            KeyAction.BACKSPACE -> {
                ic.deleteSurroundingText(1, 0)
                if (loggingAllowed && isRecordingNow() && buffer.isNotEmpty()) {
                    buffer = buffer.dropLast(1)
                    schedulePersist()
                }
            }
            KeyAction.SHIFT -> { shiftOn = !shiftOn; setKeyboardRows() }
            KeyAction.MODE -> { numbersMode = !numbersMode; setKeyboardRows() }
            KeyAction.LANGUAGE -> { marathiMode = !marathiMode; numbersMode = false; shiftOn = false; setKeyboardRows() }
            KeyAction.SWITCH -> {
                try {
                    switchToNextInputMethod(false)
                } catch (_: Exception) {
                    requestHideSelf(0)
                }
            }
            KeyAction.ENTER -> {
                persistHandler.removeCallbacks(persistRunnable)
                if (loggingAllowed && isRecordingNow() && buffer.isNotEmpty()) persistBuffer()
                val action = (currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_NONE) and EditorInfo.IME_MASK_ACTION
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                }
                buffer = ""
                sessionId = UUID.randomUUID().toString()
                sessionStartedAt = System.currentTimeMillis()
            }
        }
        updateStatus()
    }

    private fun appendToLog(value: String) {
        if (!loggingAllowed || !isRecordingNow() || value.isEmpty()) return
        if (buffer.length >= MAX_BUFFER_CHARS) return
        buffer += value
        schedulePersist()
    }

    private fun schedulePersist() {
        persistHandler.removeCallbacks(persistRunnable)
        persistHandler.postDelayed(persistRunnable, PERSIST_DELAY_MS)
    }

    private fun isRecordingNow(): Boolean {
        val consent = getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CONSENT, false)
        return consent && prefs.getBoolean(KEY_RECORDING, true)
    }

    private fun refreshLoggingEligibility() {
        val info = currentInputEditorInfo
        val nextAllowed = info != null && shouldLog(info)
        if (nextAllowed != loggingAllowed) {
            persistHandler.removeCallbacks(persistRunnable)
            if (loggingAllowed && buffer.isNotEmpty()) persistBuffer()
            loggingAllowed = nextAllowed
            buffer = ""
            sessionId = UUID.randomUUID().toString()
            sessionStartedAt = System.currentTimeMillis()
        }
        updateStatus()
    }

    private fun persistBuffer() {
        if (!loggingAllowed || !isRecordingNow() || buffer.isBlank()) return
        val record = TypingRecord(
            id = sessionId,
            packageName = activePackage,
            startedAt = sessionStartedAt,
            updatedAt = System.currentTimeMillis(),
            text = buffer
        )
        val saved = TypingLogStore.upsert(this, record)
        val result = if (saved) "Save succeeded (${buffer.length} characters)" else "SAVE FAILED (local storage/encryption error)"
        prefs.edit().putString(KEY_LAST_SAVE_STATUS, result).apply()
        if (!saved) android.util.Log.e("MahaOfficeSecurity", "Typing history save failed")
    }

    private fun appLabel(pkg: String): String = when (pkg) {
        "com.whatsapp" -> "WhatsApp"
        "com.instagram.android" -> "Instagram"
        "com.facebook.katana" -> "Facebook"
        "" -> "waiting for app"
        else -> pkg.substringAfterLast('.')
    }

    private fun keyBackground(action: KeyAction) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(if (action == KeyAction.SPACE) Color.rgb(255, 255, 255) else Color.rgb(250, 251, 252))
        setStroke(dp(1), Color.rgb(210, 218, 222))
        cornerRadius = dp(6).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class Key(val value: String, val action: KeyAction, val weight: Float = 1f)
    private enum class KeyAction { CHAR, SPACE, BACKSPACE, SHIFT, MODE, LANGUAGE, SWITCH, ENTER }

    companion object {
        private const val PREFS = "office_keyboard_prefs"
        private const val MAIN_PREFS = "office_keyboard_prefs"
        private const val KEY_CONSENT = "consent_accepted"
        private const val KEY_RECORDING = "recording_enabled"
        private const val KEY_LAST_IME_STATUS = "last_ime_status"
        private const val KEY_LAST_SAVE_STATUS = "last_save_status"
        private const val MAX_BUFFER_CHARS = 4000
        private const val PERSIST_DELAY_MS = 300L
        private val TARGET_PACKAGES = setOf("com.whatsapp", "com.instagram.android", "com.facebook.katana")
    }
}
