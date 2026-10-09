package com.dailyhisab.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.time.LocalDate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.core.designsystem.DailyHisabSectionTitle
import com.dailyhisab.android.ui.theme.DailyBlue
import com.dailyhisab.android.ui.theme.DailyOrange
import com.dailyhisab.android.ui.LocalAppDisplay
import com.dailyhisab.android.ui.appMoney
import com.dailyhisab.android.ui.appText

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onMenuClick: () -> Unit = {},
    onAddExpense: (LocalDate, Long?) -> Unit = { _, _ -> },
    onAddIncome: () -> Unit = {},
) {
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showDays by remember { mutableStateOf(false) }
    var showExpenseDetails by remember { mutableStateOf(false) }
    var showAverageDetails by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { HomeHeader(onMenuClick, { showNotifications = true }) }
        item { OverviewCarousel(summary) }
        item { QuickAddCard(categories, onAddExpense, onAddIncome) }
        item { StatisticsRow(summary, { showDays = true }, { showExpenseDetails = true }, { showAverageDetails = true }) }
        item {
            DailyHisabCard(modifier = Modifier.fillMaxWidth()) {
                DailyHisabSectionTitle(title = appText("মাসিক ক্যাটাগরি", "Monthly category chart"))
                CategoryChart(transactions, categories)
            }
        }
    }
    if (showDays) DayDetailsSheet(transactions, categories, dismiss = { showDays = false }, addExpense = { date -> showDays = false; onAddExpense(date, null) })
    if (showExpenseDetails) SummaryDialog(appText("মোট খরচ", "Total expense"), appMoney(summary.monthExpenseMinor, LocalAppDisplay.current.currency), appText("এই মাসে যোগ করা সব খরচের মোট পরিমাণ।", "Total expenses recorded in the current month.")) { showExpenseDetails = false }
    if (showAverageDetails) SummaryDialog(appText("দৈনিক গড়", "Daily average"), appMoney(summary.dailyAverageMinor, LocalAppDisplay.current.currency), appText("মোট খরচ ÷ গণনা করা দিন (${summary.countedDays})", "Total expense divided by ${summary.countedDays} counted days.")) { showAverageDetails = false }
    if (showNotifications) NotificationInbox { showNotifications = false }
}

@Composable
private fun HomeHeader(onMenuClick: () -> Unit, notifications: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenuClick) { Icon(Icons.Filled.Menu, "Open menu") }
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(DailyBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AccountBalanceWallet, null, Modifier.size(30.dp), Color.White)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Hisab", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DailyOrange)
            }
            Text(appText("আপনার দৈনিক খরচের হিসাব", "Your daily expense tracker"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = notifications) { Icon(Icons.Filled.NotificationsNone, "Notifications") }
    }
}

@Composable private fun OverviewCarousel(summary: DashboardSummary) {
    var page by remember { mutableStateOf(0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (page == 0) OverviewCard(summary) else DailyHisabCard(Modifier.fillMaxWidth()) {
            Text(appText("মাসিক অগ্রগতি", "Monthly progress"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(appMoney(summary.monthExpenseMinor, LocalAppDisplay.current.currency), style = MaterialTheme.typography.headlineLarge, color = DailyBlue)
            Text(appText("${summary.countedDays} দিন গণনা হয়েছে", "${summary.countedDays} days counted"))
        }
        Row { repeat(2) { index -> IconButton(onClick = { page = index }) { Icon(if (page == index) Icons.Filled.Circle else Icons.Filled.RadioButtonUnchecked, null, Modifier.size(12.dp), DailyBlue) } } }
    }
}

@Composable
private fun OverviewCard(summary: DashboardSummary) {
    val currency = LocalAppDisplay.current.currency
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            Modifier.background(
                Brush.linearGradient(listOf(Color(0xFF071F72), Color(0xFF075FC7), Color(0xFF00A4DB))),
            ).padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = 0.15f), shape = CircleShape) {
                    Text(
                        text = appText("আজকের সারসংক্ষেপ", "TODAY'S OVERVIEW"),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(30.dp), Color.White)
            }
            Spacer(Modifier.height(28.dp))
            Text(appText("আজকের খরচ", "Today's expense"), color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
            Text(appMoney(summary.todayExpenseMinor, currency), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewMetric(appText("এই মাস", "THIS MONTH"), appMoney(summary.monthExpenseMinor, currency), Modifier.weight(1f))
                OverviewMetric(appText("সব খরচ", "ALL EXPENSE"), appMoney(summary.allExpenseMinor, currency), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.12f)).padding(16.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickAddCard(categories: List<com.dailyhisab.android.domain.model.Category>, addExpense: (LocalDate, Long?) -> Unit, addIncome: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(appText("দ্রুত যোগ করুন", "Quick add"), Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Surface(color = DailyBlue.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp), DailyBlue)
                        Text(appText("খরচ", "Expense"), Modifier.clickable { addExpense(LocalDate.now(), null) }, color = DailyBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                QuickAction(appText("সকালের নাস্তা", "Breakfast"), Icons.Filled.Restaurant, Color(0xFFFF9D00)) { addExpense(LocalDate.now(), categories.firstOrNull { it.name.contains("নাস্তা") || it.name.contains("breakfast", true) }?.id) }
                QuickAction(appText("যাতায়াত", "Transport"), Icons.Filled.TwoWheeler, Color(0xFF0875D1)) { addExpense(LocalDate.now(), categories.firstOrNull { it.name.contains("যাতায়াত") || it.name.contains("transport", true) }?.id) }
                QuickAction(appText("আয়", "Income"), Icons.Filled.Payments, Color(0xFF00A46C), addIncome)
                QuickAction(appText("আরও", "More"), Icons.Filled.Add, Color(0xFF7549E8)) { addExpense(LocalDate.now(), null) }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, click: () -> Unit) {
    Column(Modifier.clickable(onClick = click), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = color) }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatisticsRow(summary: DashboardSummary, onDaysClick: () -> Unit, onExpenseClick: () -> Unit, onAverageClick: () -> Unit) {
    val currency = LocalAppDisplay.current.currency
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(appText("মোট খরচ", "Total expense"), appMoney(summary.monthExpenseMinor, currency), Icons.Filled.AccountBalanceWallet, Modifier.weight(1f), onExpenseClick)
        StatCard(appText("মোট দিন", "Total days"), "${summary.countedDays} ${appText("দিন", "Days")}", Icons.Filled.CalendarMonth, Modifier.weight(1f), onDaysClick)
        StatCard(appText("দৈনিক গড়", "Daily average"), appMoney(summary.dailyAverageMinor, currency), Icons.Filled.ArrowUpward, Modifier.weight(1f), onAverageClick)
    }
}

@Composable private fun CategoryChart(transactions: List<com.dailyhisab.android.domain.model.FinanceTransaction>, categories: List<com.dailyhisab.android.domain.model.Category>) {
    val month = LocalDate.now().withDayOfMonth(1)
    val totals = transactions.filter { it.type == com.dailyhisab.android.domain.model.TransactionType.Expense && !it.date.isBefore(month) }.groupBy { it.categoryId }.mapValues { it.value.sumOf { row -> row.amountMinor } }.entries.sortedByDescending { it.value }.take(5)
    if (totals.isEmpty()) Text(appText("এই মাসে এখনো কোনো খরচ নেই।", "No expense recorded this month."), color = MaterialTheme.colorScheme.onSurfaceVariant)
    else totals.forEachIndexed { index, item ->
        val max = totals.first().value.coerceAtLeast(1)
        Row(verticalAlignment = Alignment.CenterVertically) { Text(categories.firstOrNull { it.id == item.key }?.name ?: appText("অন্যান্য", "Other"), Modifier.width(100.dp), maxLines = 1); LinearProgressIndicator({ item.value.toFloat() / max }, Modifier.weight(1f)); Text(appMoney(item.value, LocalAppDisplay.current.currency), Modifier.padding(start = 8.dp)) }
    }
}

@Composable private fun SummaryDialog(title: String, value: String, explanation: String, dismiss: () -> Unit) = AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column { Text(value, style = MaterialTheme.typography.headlineMedium, color = DailyBlue); Spacer(Modifier.height(8.dp)); Text(explanation) } }, confirmButton = { TextButton(onClick = dismiss) { Text("OK") } })

@Composable private fun NotificationInbox(dismiss: () -> Unit) = AlertDialog(onDismissRequest = dismiss, title = { Text(appText("নোটিফিকেশন", "Notifications")) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ListItem({ Text(appText("দৈনিক খরচ মনে করিয়ে দেওয়া", "Daily expense reminder")) }, supportingContent = { Text(appText("প্রোফাইল থেকে সময় পরিবর্তন করুন", "Change reminder time from Profile")) }, leadingContent = { Icon(Icons.Filled.NotificationsActive, null) }); ListItem({ Text(appText("বাজেট ও ঋণ সতর্কতা", "Budget and loan alerts")) }, supportingContent = { Text(appText("গুরুত্বপূর্ণ আপডেট এখানে দেখা যাবে", "Important updates appear here")) }, leadingContent = { Icon(Icons.Filled.Info, null) }) } }, confirmButton = { TextButton(onClick = dismiss) { Text(appText("বন্ধ", "Close")) } })

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = DailyBlue)
            Spacer(Modifier.height(14.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
