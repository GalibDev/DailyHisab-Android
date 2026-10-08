package com.dailyhisab.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import java.math.BigDecimal

data class AppDisplaySettings(val language: String = "Default", val currency: String = "BDT")

val LocalAppDisplay = staticCompositionLocalOf { AppDisplaySettings() }

@Composable
fun AppDisplayProvider(language: String, currency: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppDisplay provides AppDisplaySettings(language, currency), content = content)
}

@Composable
fun appText(bangla: String, english: String, defaultText: String = english): String = when (LocalAppDisplay.current.language) {
    "বাংলা" -> bangla
    "English" -> english
    else -> defaultText
}

fun appMoney(minor: Long, currency: String): String {
    val symbol = if (currency == "USD") "$ " else "৳ "
    return symbol + BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
}
