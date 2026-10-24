package com.maha.office.security

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

data class TypingRecord(
    val id: String,
    val packageName: String,
    val startedAt: Long,
    val updatedAt: Long,
    val text: String
)

/** Encrypted, on-device-only store. No network/upload functions are included. */
object TypingLogStore {
    private const val PREFS = "office_typing_history"
    private const val KEY_LOG = "encrypted_typing_log"
    private const val KEY_ALIAS = "maha_office_security_typing_log_key_v1"
    private const val MAX_RECORDS = 500
    private const val MAX_RECORD_CHARS = 4000

    fun upsert(context: Context, record: TypingRecord): Boolean {
        val current = load(context) ?: return false
        val updated = current.toMutableList()
        val index = updated.indexOfFirst { it.id == record.id }
        if (record.text.isBlank()) {
            if (index >= 0) updated.removeAt(index)
        } else {
            val safe = record.copy(text = record.text.take(MAX_RECORD_CHARS))
            if (index >= 0) updated[index] = safe else updated.add(safe)
        }
        val capped = updated.sortedByDescending { it.updatedAt }.take(MAX_RECORDS)
        return save(context, capped)
    }

    fun load(context: Context): List<TypingRecord>? {
        val payload = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOG, null) ?: return emptyList()
        return try {
            val array = JSONArray(decrypt(payload))
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                TypingRecord(
                    id = item.getString("id"),
                    packageName = item.getString("packageName"),
                    startedAt = item.getLong("startedAt"),
                    updatedAt = item.getLong("updatedAt"),
                    text = item.getString("text")
                )
            }.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteAll(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().remove(KEY_LOG).commit()

    fun delete(context: Context, id: String): Boolean {
        val current = load(context) ?: return false
        return save(context, current.filterNot { it.id == id })
    }

    private fun save(context: Context, records: List<TypingRecord>): Boolean = try {
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().apply {
                put("id", record.id)
                put("packageName", record.packageName)
                put("startedAt", record.startedAt)
                put("updatedAt", record.updatedAt)
                put("text", record.text)
            })
        }
        val encrypted = encrypt(array.toString())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOG, encrypted).commit()
    } catch (_: Exception) {
        false
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val bytes = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split(":", limit = 2)
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
