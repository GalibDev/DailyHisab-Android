package com.dailyhisab.android.feature.reports

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dailyhisab.android.domain.model.FinanceTransaction
import com.dailyhisab.android.domain.model.TransactionType
import java.io.File
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.dailyhisab.android.ui.LocalAppDisplay
import com.dailyhisab.android.ui.appMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MonthlyCardScreen(
    contentPadding: PaddingValues,
    transactions: List<FinanceTransaction>,
    back: () -> Unit,
) {
    val context = LocalContext.current
    val currency = LocalAppDisplay.current.currency
    var month by remember { mutableStateOf(YearMonth.now()) }
    var hideAmounts by remember { mutableStateOf(true) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val monthRows = remember(transactions, month) { transactions.filter { YearMonth.from(it.date) == month } }
    val income = remember(monthRows) { monthRows.filter { it.type == TransactionType.Income }.sumOf { it.amountMinor } }
    val expense = remember(monthRows) { monthRows.filter { it.type == TransactionType.Expense }.sumOf { it.amountMinor } }
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                savedMessage = runCatching {
                    withContext(Dispatchers.IO) { MonthlyCardExporter.write(context, uri, month, income, expense, hideAmounts, currency) }
                    "PNG downloaded"
                }.getOrElse { "Download failed: ${it.localizedMessage ?: "unknown error"}" }
                busy = false
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("মাসিক হিসাব শেয়ার করুন", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        } }
        item { Text("মাস", fontWeight = FontWeight.SemiBold) }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
            OutlinedCard(Modifier.weight(1f)) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.fillMaxWidth().padding(18.dp), style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) { Icon(Icons.Filled.ChevronRight, "Next month") }
        } }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(hideAmounts, { hideAmounts = it })
            Text("টাকার পরিমাণ লুকিয়ে রাখুন", style = MaterialTheme.typography.titleMedium)
        } }
        item { Text("নিচের ছবিটিই শেয়ার হবে। নাম, নোট বা লেনদেনের বিস্তারিত থাকবে না।", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { MonthlyCardPreview(month, income, expense, hideAmounts, currency) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { download.launch("daily-hisab-${month}.png") },
                modifier = Modifier.weight(1f),
                enabled = !busy,
            ) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text("PNG ডাউনলোড") }
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        savedMessage = runCatching {
                            val shareIntent = withContext(Dispatchers.IO) { MonthlyCardExporter.createShareIntent(context, month, income, expense, hideAmounts, currency) }
                            context.startActivity(Intent.createChooser(shareIntent, "Share monthly card"))
                            "Share sheet opened"
                        }.getOrElse { "Share failed: ${it.localizedMessage ?: "unknown error"}" }
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !busy,
            ) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(6.dp)); Text("শেয়ার") }
        } }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        savedMessage?.let { message -> item { Text(message, color = if (message.contains("failed", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable
private fun MonthlyCardPreview(month: YearMonth, income: Long, expense: Long, hidden: Boolean, currency: String) {
    val shownIncome = if (hidden) "••••" else appMoney(income, currency)
    val shownExpense = if (hidden) "••••" else appMoney(expense, currency)
    val shownBalance = if (hidden) "••••" else appMoney(income - expense, currency)
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF082A85), androidx.compose.ui.graphics.Color(0xFF1455C8)))).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Daily Hisab", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            Text("আমার মাসের হিসাব", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), color = androidx.compose.ui.graphics.Color.White.copy(alpha = .8f))
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = .12f))) { Metric("রেকর্ড করা আয়", shownIncome) }
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = .12f))) { Metric("রেকর্ড করা খরচ", shownExpense) }
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = .12f))) { Metric("আয় – খরচ", shownBalance) }
            Text("নিজের হিসাব, নিজের নিয়ন্ত্রণে।", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
            Text("ব্যক্তিগত পরিমাণ লুকানো আছে", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
            Text("dailyhisab.xyz", color = androidx.compose.ui.graphics.Color(0xFFFFA23A), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(14.dp)) {
        Text(label, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .78f))
        Text(value, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

private object MonthlyCardExporter {
    fun write(context: Context, uri: Uri, month: YearMonth, income: Long, expense: Long, hidden: Boolean, currency: String) {
        context.contentResolver.openOutputStream(uri)?.use { stream -> render(month, income, expense, hidden, currency).compress(Bitmap.CompressFormat.PNG, 100, stream) }
    }

    fun createShareIntent(context: Context, month: YearMonth, income: Long, expense: Long, hidden: Boolean, currency: String): Intent {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "daily-hisab-${month}.png")
        file.outputStream().use { render(month, income, expense, hidden, currency).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Daily Hisab — ${month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("Daily Hisab monthly card", uri)
        }
    }

    private fun render(month: YearMonth, income: Long, expense: Long, hidden: Boolean, currency: String): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(12, 49, 139))
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 62f; typeface = Typeface.DEFAULT_BOLD }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(205, 220, 255); textSize = 34f }
        val amount = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 54f; typeface = Typeface.DEFAULT_BOLD }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 164, 58); textSize = 36f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText("Daily Hisab", 90f, 115f, amount)
        canvas.drawText("আমার মাসের হিসাব", 90f, 220f, title)
        canvas.drawText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), 90f, 275f, label)
        val values = listOf("রেকর্ড করা আয়" to income, "রেকর্ড করা খরচ" to expense, "আয় – খরচ" to (income - expense))
        values.forEachIndexed { index, (name, value) ->
            val top = 350f + index * 210f
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(35, 255, 255, 255) }.also { canvas.drawRoundRect(75f, top, 1005f, top + 170f, 28f, 28f, it) }
            canvas.drawText(name, 110f, top + 58f, label)
            canvas.drawText(if (hidden) "••••" else appMoney(value, currency), 110f, top + 128f, amount)
        }
        canvas.drawText("নিজের হিসাব, নিজের নিয়ন্ত্রণে।", 90f, 1080f, amount)
        canvas.drawText(if (hidden) "ব্যক্তিগত পরিমাণ লুকানো আছে" else "মাসিক আয়-ব্যয়ের সারাংশ", 90f, 1140f, label)
        canvas.drawText("dailyhisab.xyz", 90f, 1245f, accent)
        return bitmap
    }
}
