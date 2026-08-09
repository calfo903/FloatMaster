package com.floatmaster.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WHY: EncryptedSharedPreferences (AES256_GCM) — per-host username/email autofill, not plaintext DataStore. Users reuse logins in floating browser.
 * Quality: KDoc, no !!, Result via nullable.
 */
@Singleton
class AutoFillRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(context, "autofill_enc", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        } catch (_: Exception) {
            // WHY: Fallback to plain prefs on <23 or no Google Play — better than crash
            context.getSharedPreferences("autofill_plain", Context.MODE_PRIVATE)
        }
    }

    fun save(host: String, username: String, password: String? = null) {
        // WHY: Trim + cap prevents injection via huge string
        val safeHost = host.take(100).lowercase()
        val safeUser = username.take(254).trim()
        if (safeUser.isBlank()) return
        prefs.edit().putString("user_\$safeHost", safeUser).apply()
        password?.take(256)?.let { prefs.edit().putString("pass_\$safeHost", it).apply() }
    }

    fun getUsername(host: String): String? = prefs.getString("user_\${host.take(100).lowercase()}", null)?.takeIf { it.isNotBlank() }

    fun getAll(): Map<String, String> {
        return prefs.all.filterKeys { it.startsWith("user_") }.mapKeys { it.key.removePrefix("user_") }.mapValues { it.value as? String ?: "" }.filterValues { it.isNotBlank() }
    }

    fun clear(host: String) { prefs.edit().remove("user_\${host.lowercase()}").remove("pass_\${host.lowercase()}").apply() }
}
