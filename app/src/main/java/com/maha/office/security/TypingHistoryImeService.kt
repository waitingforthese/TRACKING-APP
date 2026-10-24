package com.maha.office.security

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import java.util.UUID

/**
 * A user-selected, visible Android keyboard with a modern Android/Gboard-inspired layout.
 * Typing history remains opt-in, visible, local-only, app-scoped, and subject to input-field
 * privacy safeguards. This does not read text entered with Gboard or any other keyboard.
 */
class TypingHistoryImeService : InputMethodService() {
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private var root: LinearLayout? = null
    private var statusView: TextView? = null
    private var toolbar: LinearLayout? = null
    private var languageToolbarButton: TextView? = null

    private var shiftOn = true
    private var numbersMode = false
    private var marathiMode = false
    private var emojiMode = false

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
            setPadding(dp(4), dp(4), dp(4), dp(6))
            setBackgroundColor(KEYBOARD_BG)
        }

        statusView = TextView(this).apply {
            textSize = 10.5f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(INK)
            setPadding(dp(8), 0, dp(8), 0)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            background = rounded(STATUS_BG, STATUS_BG, 10)
            contentDescription = "Visible typing-history recording status"
        }
        root!!.addView(
            statusView,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(22)).apply {
                bottomMargin = dp(3)
            }
        )

        buildToolbar()
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
        shiftOn = true
        numbersMode = false
        emojiMode = false
        sessionId = UUID.randomUUID().toString()
        sessionStartedAt = System.currentTimeMillis()
        updateToolbarLanguageLabel()
        updateStatus()
        setKeyboardRows()
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
        if (!consent || prefs.getInt(KEY_CONSENT_VERSION, 0) < CURRENT_CONSENT_VERSION) {
            return "updated consent required"
        }
        if (!prefs.getBoolean(KEY_RECORDING, true)) return "history is paused"
        if (info == null) return "no text field focused"
        if (info.packageName !in TARGET_PACKAGES) return "current app is outside the allowed scope"

        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (inputClass != InputType.TYPE_CLASS_TEXT) return "input type is not ordinary text"
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) return "password field excluded"

        // Respect the field's privacy hint. Never bypass this flag to collect its contents.
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
            return "field is marked not for personalized learning"
        }
        return null
    }

    private fun shouldLog(info: EditorInfo): Boolean = loggingBlockReason(info) == null

    private fun updateStatus() {
        val status = statusView ?: return
        val info = currentInputEditorInfo
        val packageName = info?.packageName ?: activePackage
        val label = appLabel(packageName)
        val consent = prefs.getBoolean(KEY_CONSENT, false) &&
            prefs.getInt(KEY_CONSENT_VERSION, 0) >= CURRENT_CONSENT_VERSION
        val recording = prefs.getBoolean(KEY_RECORDING, true)
        val reason = loggingBlockReason(info)

        val message = when {
            !consent -> "History disabled • accept consent in app"
            !recording -> "History paused • $label"
            reason != null -> "Not recording • $label / $reason"
            !loggingAllowed -> "Ready • $label"
            else -> "● RECORDING ON • $label"
        }
        status.text = message
        val isRecording = message.startsWith("● RECORDING ON")
        status.setTypeface(null, if (isRecording) Typeface.BOLD else Typeface.NORMAL)
        status.setTextColor(if (isRecording) RECORDING_INK else INK)
        status.background = rounded(
            if (isRecording) RECORDING_BG else STATUS_BG,
            if (isRecording) RECORDING_BORDER else STATUS_BG,
            10
        )
        val oldMessage = prefs.getString(KEY_LAST_IME_STATUS, null)
        if (oldMessage != message) prefs.edit().putString(KEY_LAST_IME_STATUS, message).apply()
    }

    private fun buildToolbar() {
        val container = root ?: return
        toolbar?.let { container.removeView(it) }
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(2))
        }
        container.addView(
            toolbar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34)).apply {
                bottomMargin = dp(2)
            }
        )

        addToolbarButton("▤", "Open Maha Office Security history and settings", 1f) {
            openMainApp()
        }
        languageToolbarButton = addToolbarButton("EN", "Switch between English and basic Marathi layout", 1f) {
            toggleLanguage()
        }
        addToolbarButton("☺", "Open emoji keyboard", 1f) {
            emojiMode = !emojiMode
            numbersMode = false
            setKeyboardRows()
        }
        addToolbarButton("⇄", "Switch to another installed keyboard", 1f) {
            switchExternalKeyboard()
        }
        addToolbarButton("⌄", "Hide keyboard", 1f) {
            requestHideSelf(0)
        }
    }

    private fun addToolbarButton(
        label: String,
        description: String,
        weight: Float,
        action: () -> Unit
    ): TextView {
        val button = TextView(this).apply {
            text = label
            contentDescription = description
            gravity = Gravity.CENTER
            textSize = if (label.length > 2) 12f else 18f
            setTextColor(TOOL_INK)
            isClickable = true
            isFocusable = true
            background = rounded(TOOL_BG, TOOL_BG, 8)
            setOnClickListener { action() }
        }
        val lp = LinearLayout.LayoutParams(0, dp(32), weight).apply {
            setMargins(dp(2), 0, dp(2), 0)
        }
        toolbar?.addView(button, lp)
        return button
    }

    private fun updateToolbarLanguageLabel() {
        languageToolbarButton?.apply {
            text = if (marathiMode) "अ" else "EN"
            contentDescription = if (marathiMode) "Switch to English layout" else "Switch to basic Marathi layout"
        }
    }

    private fun setKeyboardRows() {
        val container = root ?: return
        // Preserve status and toolbar, removing only prior key rows.
        while (container.childCount > 2) container.removeViewAt(2)

        val rows = when {
            emojiMode -> emojiRows()
            numbersMode -> symbolRows()
            marathiMode -> marathiRows()
            else -> englishRows()
        }

        rows.forEachIndexed { rowIndex, keyboardRow ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(keyboardRow.insetDp), 0, dp(keyboardRow.insetDp), 0)
            }
            keyboardRow.keys.forEach { key ->
                val view = buildKeyView(key, keyboardRow.tone)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, key.weight).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                row.addView(view, lp)
            }
            container.addView(
                row,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(keyboardRow.heightDp)).apply {
                    bottomMargin = if (rowIndex == rows.lastIndex) 0 else dp(1)
                }
            )
        }
        updateToolbarLanguageLabel()
        updateStatus()
    }

    private fun englishRows(): List<KeyboardRow> = listOf(
        KeyboardRow("1234567890".map { Key(it.toString(), KeyAction.CHAR) }, 31, tone = KeyTone.NUMBER),
        KeyboardRow("qwertyuiop".map { Key(it.toString(), KeyAction.CHAR) }, 42),
        KeyboardRow("asdfghjkl".map { Key(it.toString(), KeyAction.CHAR) }, 42, insetDp = 9),
        KeyboardRow(
            listOf(Key("⇧", KeyAction.SHIFT, 1.35f)) +
                "zxcvbnm".map { Key(it.toString(), KeyAction.CHAR) } +
                listOf(Key("⌫", KeyAction.BACKSPACE, 1.35f)),
            43
        ),
        KeyboardRow(
            listOf(
                Key("?123", KeyAction.MODE, 1.35f),
                Key(",", KeyAction.CHAR, 0.8f),
                Key("☺", KeyAction.EMOJI_MODE, 0.9f),
                Key("English (US)", KeyAction.SPACE, 4.1f),
                Key(".", KeyAction.CHAR, 0.8f),
                Key("↵", KeyAction.ENTER, 1.25f)
            ),
            45
        )
    )

    private fun symbolRows(): List<KeyboardRow> = listOf(
        KeyboardRow("1234567890".map { Key(it.toString(), KeyAction.CHAR) }, 40),
        KeyboardRow("@#$%&-+()".map { Key(it.toString(), KeyAction.CHAR) }, 40, insetDp = 4),
        KeyboardRow(
            listOf(
                Key("=", KeyAction.CHAR), Key("/", KeyAction.CHAR), Key(":", KeyAction.CHAR),
                Key(";", KeyAction.CHAR), Key("!", KeyAction.CHAR), Key("?", KeyAction.CHAR),
                Key("⌫", KeyAction.BACKSPACE, 1.3f)
            ),
            40
        ),
        KeyboardRow(
            listOf(
                Key("ABC", KeyAction.ALPHA, 1.2f), Key("*", KeyAction.CHAR),
                Key("☺", KeyAction.EMOJI_MODE), Key("English (US)", KeyAction.SPACE, 3.8f),
                Key(".", KeyAction.CHAR), Key("↵", KeyAction.ENTER, 1.25f)
            ),
            45
        )
    )

    /** A compact standalone Devanagari character layout; it is not phonetic transliteration. */
    private fun marathiRows(): List<KeyboardRow> = listOf(
        KeyboardRow(listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ए", "ऐ", "ओ", "औ").map { Key(it, KeyAction.CHAR) }, 38),
        KeyboardRow(listOf("क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ").map { Key(it, KeyAction.CHAR) }, 38),
        KeyboardRow(listOf("ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध", "न").map { Key(it, KeyAction.CHAR) }, 38),
        KeyboardRow(listOf("प", "फ", "ब", "भ", "म", "य", "र", "ल", "व", "श").map { Key(it, KeyAction.CHAR) }, 38),
        KeyboardRow(
            listOf("ष", "स", "ह", "ळ", "क्ष", "त्र", "ज्ञ", "श्र", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं", "ः", "्").map { Key(it, KeyAction.CHAR) },
            39
        ),
        KeyboardRow(
            listOf(
                Key("?123", KeyAction.MODE, 1.2f), Key("EN", KeyAction.LANGUAGE, 1f),
                Key(",", KeyAction.CHAR, 0.75f), Key("☺", KeyAction.EMOJI_MODE, 0.85f),
                Key("मराठी", KeyAction.SPACE, 3.4f), Key("।", KeyAction.CHAR, 0.75f),
                Key("↵", KeyAction.ENTER, 1.15f)
            ),
            44
        )
    )

    private fun emojiRows(): List<KeyboardRow> = listOf(
        KeyboardRow(listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🙂", "😉", "😊").map { Key(it, KeyAction.CHAR) }, 42),
        KeyboardRow(listOf("🥰", "😍", "😘", "😎", "🤔", "🥹", "😭", "😡", "🤗", "😴").map { Key(it, KeyAction.CHAR) }, 42),
        KeyboardRow(listOf("👍", "🙏", "👏", "🎉", "✅", "🔥", "❤️", "💯", "✨", "🙌").map { Key(it, KeyAction.CHAR) }, 42),
        KeyboardRow(
            listOf(
                Key("ABC", KeyAction.ALPHA, 1.25f), Key("?123", KeyAction.MODE, 1.1f),
                Key("☻", KeyAction.EMOJI_MODE, 1f), Key("English (US)", KeyAction.SPACE, 3.8f),
                Key("⌫", KeyAction.BACKSPACE, 1.15f), Key("↵", KeyAction.ENTER, 1.15f)
            ),
            45
        )
    )

    private fun buildKeyView(key: Key, rowTone: KeyTone): TextView {
        val label = when (key.action) {
            KeyAction.SHIFT -> if (shiftOn) "⬆" else "⇧"
            KeyAction.BACKSPACE -> "⌫"
            KeyAction.MODE -> "?123"
            KeyAction.ALPHA -> "ABC"
            KeyAction.SPACE -> key.value
            KeyAction.LANGUAGE -> if (marathiMode) "EN" else "अ"
            KeyAction.EMOJI_MODE -> "☺"
            KeyAction.ENTER -> "↵"
            else -> if (
                !marathiMode && key.action == KeyAction.CHAR && key.value.length == 1 &&
                key.value[0].isLetter() && shiftOn
            ) key.value.uppercase() else key.value
        }
        val keyView = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = when {
                key.action == KeyAction.SPACE -> 10f
                key.value.length > 2 -> 10f
                rowTone == KeyTone.NUMBER -> 14f
                key.action == KeyAction.CHAR && key.value.codePointCount(0, key.value.length) > 1 -> 18f
                else -> 17f
            }
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
            setTextColor(
                if (key.action == KeyAction.ENTER) Color.WHITE
                else if (key.action == KeyAction.SPACE || rowTone == KeyTone.NUMBER) KEY_INK
                else KEY_INK
            )
            background = keyBackground(key.action, rowTone)
            elevation = if (rowTone == KeyTone.NUMBER) 0f else dp(1).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = when (key.action) {
                KeyAction.SPACE -> "Space"
                KeyAction.BACKSPACE -> "Backspace"
                KeyAction.ENTER -> "Enter"
                KeyAction.SHIFT -> "Shift"
                KeyAction.MODE -> "Switch to symbols and numbers"
                KeyAction.ALPHA -> "Switch to letters"
                KeyAction.EMOJI_MODE -> "Emoji keyboard"
                KeyAction.LANGUAGE -> "Switch language"
                else -> label
            }
            setOnClickListener { handleKey(key) }
        }
        return keyView
    }

    private fun handleKey(key: Key) {
        refreshLoggingEligibility()
        val ic: InputConnection = currentInputConnection ?: return
        when (key.action) {
            KeyAction.CHAR -> {
                val value = if (
                    !marathiMode && key.value.length == 1 && key.value[0].isLetter() && shiftOn
                ) key.value.uppercase() else key.value
                if (ic.commitText(value, 1)) {
                    appendToLog(value)
                    if (!marathiMode && key.value.length == 1 && key.value[0].isLetter() && shiftOn) {
                        shiftOn = false
                        setKeyboardRows()
                    }
                } else {
                    prefs.edit().putString(KEY_LAST_SAVE_STATUS, "InputConnection rejected character; nothing saved").apply()
                }
            }
            KeyAction.SPACE -> {
                if (ic.commitText(" ", 1)) appendToLog(" ")
                else prefs.edit().putString(KEY_LAST_SAVE_STATUS, "InputConnection rejected space; nothing saved").apply()
            }
            KeyAction.BACKSPACE -> {
                ic.deleteSurroundingTextInCodePoints(1, 0)
                if (loggingAllowed && isRecordingNow() && buffer.isNotEmpty()) {
                    val lastCodePoint = buffer.offsetByCodePoints(buffer.length, -1)
                    buffer = buffer.substring(0, lastCodePoint)
                    schedulePersist()
                }
            }
            KeyAction.SHIFT -> {
                shiftOn = !shiftOn
                setKeyboardRows()
            }
            KeyAction.MODE -> {
                numbersMode = true
                emojiMode = false
                setKeyboardRows()
            }
            KeyAction.ALPHA -> {
                numbersMode = false
                emojiMode = false
                setKeyboardRows()
            }
            KeyAction.LANGUAGE -> toggleLanguage()
            KeyAction.EMOJI_MODE -> {
                emojiMode = !emojiMode
                numbersMode = false
                setKeyboardRows()
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
                shiftOn = true
                sessionId = UUID.randomUUID().toString()
                sessionStartedAt = System.currentTimeMillis()
                setKeyboardRows()
            }
        }
        updateStatus()
    }

    private fun toggleLanguage() {
        marathiMode = !marathiMode
        numbersMode = false
        emojiMode = false
        shiftOn = true
        updateToolbarLanguageLabel()
        setKeyboardRows()
    }

    private fun switchExternalKeyboard() {
        try {
            val switched = switchToNextInputMethod(false)
            if (!switched) requestHideSelf(0)
        } catch (_: Exception) {
            requestHideSelf(0)
        }
    }

    private fun openMainApp() {
        try {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("MahaOfficeSecurity", "Could not open main app from keyboard", e)
        }
    }

    private fun appendToLog(value: String) {
        if (!loggingAllowed || !isRecordingNow() || value.isEmpty()) return
        if (buffer.length >= MAX_BUFFER_CHARS) return
        buffer += value
        schedulePersist()
        prefs.edit().putString(KEY_LAST_SAVE_STATUS, "Typing captured in memory; waiting for local save").apply()
    }

    private fun schedulePersist() {
        persistHandler.removeCallbacks(persistRunnable)
        persistHandler.postDelayed(persistRunnable, PERSIST_DELAY_MS)
    }

    private fun isRecordingNow(): Boolean {
        val mainPrefs = getSharedPreferences(MAIN_PREFS, Context.MODE_PRIVATE)
        val consent = mainPrefs.getBoolean(KEY_CONSENT, false) &&
            mainPrefs.getInt(KEY_CONSENT_VERSION, 0) >= CURRENT_CONSENT_VERSION
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
        "com.whatsapp.w4b" -> "WhatsApp Business"
        "com.instagram.android" -> "Instagram"
        "com.facebook.katana" -> "Facebook"
        "" -> "waiting for app"
        else -> pkg.substringAfterLast('.')
    }

    private fun keyBackground(action: KeyAction, rowTone: KeyTone): GradientDrawable {
        val fill = when {
            action == KeyAction.ENTER -> ENTER_BG
            action == KeyAction.SPACE -> Color.WHITE
            action == KeyAction.SHIFT || action == KeyAction.BACKSPACE ||
                action == KeyAction.MODE || action == KeyAction.ALPHA ||
                action == KeyAction.LANGUAGE || action == KeyAction.EMOJI_MODE -> MODIFIER_BG
            rowTone == KeyTone.NUMBER -> KEYBOARD_BG
            else -> Color.WHITE
        }
        val stroke = if (rowTone == KeyTone.NUMBER) KEYBOARD_BG else KEY_BORDER
        return rounded(fill, stroke, if (rowTone == KeyTone.NUMBER) 5 else 6)
    }

    private fun rounded(fillColor: Int, strokeColor: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor)
        setStroke(dp(1), strokeColor)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class Key(
        val value: String,
        val action: KeyAction,
        val weight: Float = 1f
    )

    private data class KeyboardRow(
        val keys: List<Key>,
        val heightDp: Int,
        val insetDp: Int = 0,
        val tone: KeyTone = KeyTone.NORMAL
    )

    private enum class KeyTone { NORMAL, NUMBER }

    private enum class KeyAction {
        CHAR, SPACE, BACKSPACE, SHIFT, MODE, ALPHA, LANGUAGE, EMOJI_MODE, ENTER
    }

    companion object {
        private const val PREFS = "office_keyboard_prefs"
        private const val MAIN_PREFS = "office_keyboard_prefs"
        private const val KEY_CONSENT = "consent_accepted"
        private const val KEY_CONSENT_VERSION = "consent_scope_version"
        private const val CURRENT_CONSENT_VERSION = 2
        private const val KEY_RECORDING = "recording_enabled"
        private const val KEY_LAST_IME_STATUS = "last_ime_status"
        private const val KEY_LAST_SAVE_STATUS = "last_save_status"
        private const val MAX_BUFFER_CHARS = 4000
        private const val PERSIST_DELAY_MS = 300L
        private val TARGET_PACKAGES = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "com.instagram.android", "com.facebook.katana"
        )

        private val KEYBOARD_BG = Color.rgb(216, 222, 230)
        private val STATUS_BG = Color.rgb(231, 235, 241)
        private val TOOL_BG = Color.rgb(226, 231, 238)
        private val RECORDING_BG = Color.rgb(220, 242, 232)
        private val RECORDING_BORDER = Color.rgb(185, 224, 207)
        private val RECORDING_INK = Color.rgb(0, 108, 79)
        private val KEY_BORDER = Color.rgb(214, 219, 226)
        private val MODIFIER_BG = Color.rgb(197, 205, 216)
        private val ENTER_BG = Color.rgb(12, 124, 111)
        private val INK = Color.rgb(53, 63, 74)
        private val TOOL_INK = Color.rgb(75, 85, 97)
        private val KEY_INK = Color.rgb(37, 45, 55)
    }
}
