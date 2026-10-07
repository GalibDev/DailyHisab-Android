package com.dailyhisab.android.feature.profile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class LocalProfile(
    val displayName: String = "Guest User",
    val email: String = "",
    val userId: String,
    val themeMode: String = "System",
    val themeStyle: String = "Aurora",
    val language: String = "Default",
    val currency: String = "BDT",
)

class ProfilePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("daily_hisab_profile", Context.MODE_PRIVATE)
    private val mutableProfile = MutableStateFlow(read())
    val profile: StateFlow<LocalProfile> = mutableProfile

    fun update(transform: (LocalProfile) -> LocalProfile) {
        val updated = transform(mutableProfile.value)
        preferences.edit()
            .putString("displayName", updated.displayName)
            .putString("email", updated.email)
            .putString("userId", updated.userId)
            .putString("themeMode", updated.themeMode)
            .putString("themeStyle", updated.themeStyle)
            .putString("language", updated.language)
            .putString("currency", updated.currency)
            .apply()
        mutableProfile.value = updated
    }

    private fun read(): LocalProfile {
        val userId = preferences.getString("userId", null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString("userId", it).apply()
        }
        return LocalProfile(
            displayName = preferences.getString("displayName", "Guest User") ?: "Guest User",
            email = preferences.getString("email", "") ?: "",
            userId = userId,
            themeMode = preferences.getString("themeMode", "System") ?: "System",
            themeStyle = preferences.getString("themeStyle", "Aurora") ?: "Aurora",
            language = preferences.getString("language", "Default") ?: "Default",
            currency = preferences.getString("currency", "BDT") ?: "BDT",
        )
    }
}
