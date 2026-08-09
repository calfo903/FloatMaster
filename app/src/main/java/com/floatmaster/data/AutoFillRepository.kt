package com.floatmaster.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WHY: Autofill data is security-sensitive. Fail closed if encrypted storage cannot be initialized; never downgrade to plaintext.
 */
@Singleton
class AutoFillRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "autofill_enc",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(host: String, username: String) {
        val safeHost = canonicalHost(host) ?: return
        val safeUser = username.trim().take(254)
        if (safeUser.isBlank()) return
        prefs.edit().putString(userKey(safeHost), safeUser).apply()
    }

    fun getUsername(host: String): String? = canonicalHost(host)
        ?.let { prefs.getString(userKey(it), null)?.takeIf(String::isNotBlank) }

    fun getAll(): Map<String, String> = prefs.all
        .asSequence()
        .filter { it.key.startsWith(USER_PREFIX) && it.value is String }
        .associate { it.key.removePrefix(USER_PREFIX) to (it.value as String).take(254) }
        .filterValues(String::isNotBlank)

    fun clear(host: String) {
        canonicalHost(host)?.let { safeHost -> prefs.edit().remove(userKey(safeHost)).apply() }
    }

    private fun userKey(host: String): String = "$USER_PREFIX$host"

    private fun canonicalHost(raw: String): String? {
        val host = raw.trim().lowercase()
        if (host.isBlank() || host.length > 253) return null
        if (host.any { it.isWhitespace() || it == '/' || it == '\\' }) return null
        return host
    }

    private companion object {
        const val USER_PREFIX = "user_"
    }
}
