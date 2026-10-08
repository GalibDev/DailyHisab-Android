package com.dailyhisab.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dailyhisab.android.ui.DailyHisabApp
import com.dailyhisab.android.ui.theme.DailyHisabTheme
import com.dailyhisab.android.ui.theme.AppThemeStyle
import com.dailyhisab.android.feature.profile.ProfilePreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.dailyhisab.android.notifications.ReminderScheduler
import com.dailyhisab.android.ui.AppDisplayProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.scheduleAll(applicationContext)
        setContent {
            val preferences = remember { ProfilePreferences(applicationContext) }
            val profile by preferences.profile.collectAsState()
            val dark = when (profile.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }
            DailyHisabTheme(
                darkTheme = dark,
                style = if (profile.themeStyle == "Default") AppThemeStyle.Default else AppThemeStyle.Aurora,
            ) {
                AppDisplayProvider(profile.language, profile.currency) {
                    DailyHisabApp(profile, preferences)
                }
            }
        }
    }
}
