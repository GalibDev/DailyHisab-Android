package com.dailyhisab.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dailyhisab.android.ui.DailyHisabApp
import com.dailyhisab.android.ui.theme.DailyHisabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyHisabTheme {
                DailyHisabApp()
            }
        }
    }
}

