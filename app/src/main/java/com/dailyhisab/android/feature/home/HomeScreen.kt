package com.dailyhisab.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.core.designsystem.DailyHisabSectionTitle
import com.dailyhisab.android.ui.theme.DailyBlue
import com.dailyhisab.android.ui.theme.DailyOrange

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { HomeHeader() }
        item { OverviewCard(summary) }
        item { QuickAddCard() }
        item { StatisticsRow(summary) }
        item {
            DailyHisabCard(modifier = Modifier.fillMaxWidth()) {
                DailyHisabSectionTitle(
                    title = "This month overview",
                    supportingText = "Your expense chart will appear after the first transaction.",
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            Text("Your daily expense tracker", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {}) { Icon(Icons.Filled.NotificationsNone, "Notifications") }
    }
}

@Composable
private fun OverviewCard(summary: DashboardSummary) {
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
                        text = "TODAY'S OVERVIEW",
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
            Text("Today's expense", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
            Text(formatMoney(summary.todayExpenseMinor), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewMetric("THIS MONTH", formatMoney(summary.monthExpenseMinor), Modifier.weight(1f))
                OverviewMetric("ALL EXPENSE", formatMoney(summary.allExpenseMinor), Modifier.weight(1f))
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
private fun QuickAddCard() {
    Card(
        Modifier.fillMaxWidth(), RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick add", Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Surface(color = DailyBlue.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp), DailyBlue)
                        Text("Expense", color = DailyBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                QuickAction("Breakfast", Icons.Filled.Restaurant, Color(0xFFFF9D00))
                QuickAction("Transport", Icons.Filled.TwoWheeler, Color(0xFF0875D1))
                QuickAction("Income", Icons.Filled.Payments, Color(0xFF00A46C))
                QuickAction("More", Icons.Filled.Add, Color(0xFF7549E8))
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = color) }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatisticsRow(summary: DashboardSummary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Total expense", formatMoney(summary.monthExpenseMinor), Icons.Filled.AccountBalanceWallet, Modifier.weight(1f))
        StatCard("Total days", "${summary.countedDays} Days", Icons.Filled.CalendarMonth, Modifier.weight(1f))
        StatCard("Daily average", formatMoney(summary.dailyAverageMinor), Icons.Filled.ArrowUpward, Modifier.weight(1f))
    }
}

private fun formatMoney(amountMinor: Long): String {
    val whole = amountMinor / 100
    val fraction = amountMinor % 100
    return if (fraction == 0L) "৳ $whole" else "৳ $whole.${fraction.toString().padStart(2, '0')}"
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier, RoundedCornerShape(20.dp),
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
