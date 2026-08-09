package com.floatmaster.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** WHY: Autofill data is encrypted and constrained to a JS-string-safe username alphabet; plaintext fallback is forbidden. */
@Singleton
class AutoFillRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
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
        val safeUser = sanitizeUsername(username) ?: return
        prefs.edit().putString(userKey(safeHost), safeUser).apply()
    }

    fun getUsername(host: String): String? = canonicalHost(host)?.let { safeHost ->
        prefs.getString(userKey(safeHost), null)?.let(::sanitizeUsername)
    }

    fun getAll(): Map<String, String> = prefs.all.asSequence()
        .filter { it.key.startsWith(USER_PREFIX) && it.value is String }
        .mapNotNull { entry -> sanitizeUsername(entry.value as String)?.let { entry.key.removePrefix(USER_PREFIX) to it } }
        .toMap()

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

    private fun sanitizeUsername(raw: String): String? {
        val value = raw.trim().take(254)
        if (value.isBlank() || value.length > 254) return null
        return value.takeIf { USERNAME_PATTERN.matches(it) }
    }

    private companion object {
        const val USER_PREFIX = "user_"
        val USERNAME_PATTERN = Regex("[A-Za-z0-9._%+@-]{1,254}")
    }
}
