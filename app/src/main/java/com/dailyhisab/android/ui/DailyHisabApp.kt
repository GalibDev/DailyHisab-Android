package com.dailyhisab.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dailyhisab.android.feature.home.HomeScreen
import com.dailyhisab.android.ui.theme.AppBackground

@Composable
fun DailyHisabApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground,
    ) {
        HomeScreen()
    }
}

