package com.dailyhisab.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailyhisab.android.BuildConfig
import com.dailyhisab.android.feature.auth.AuthUser
import com.dailyhisab.android.feature.home.DashboardSummary
import com.dailyhisab.android.feature.profile.LocalProfile
import coil3.compose.AsyncImage

internal enum class DrawerRoute {
    Dashboard, Expenses, Categories, Budgets, Savings, Loans, Reports, Calendar, Backup, Profile,
}

@Composable
internal fun AppDrawer(
    profile: LocalProfile,
    user: AuthUser?,
    summary: DashboardSummary,
    selected: DrawerRoute,
    close: () -> Unit,
    navigate: (DrawerRoute) -> Unit,
    logout: () -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.width(330.dp), drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        if (profile.photoUrl.isNotBlank()) AsyncImage(profile.photoUrl, "Profile image", Modifier.fillMaxSize())
                        else Text(initials(user?.displayName?.ifBlank { profile.displayName } ?: profile.displayName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(user?.displayName?.ifBlank { profile.displayName } ?: profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(user?.email?.ifBlank { profile.email } ?: profile.email.ifBlank { "Local guest" }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close menu") }
            }

            Card(
                onClick = { navigate(DrawerRoute.Expenses) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(appText("এই মাসের খরচ", "This month's expense", "এই মাসের খরচ"), fontWeight = FontWeight.SemiBold)
                        Text(appMoney(summary.monthExpenseMinor, profile.currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }

            Text(appText("প্রধান", "Main", "প্রধান"), Modifier.padding(top = 12.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            DrawerItem(appText("ড্যাশবোর্ড", "Dashboard", "ড্যাশবোর্ড"), Icons.Filled.Home, DrawerRoute.Dashboard, selected, navigate)
            DrawerItem(appText("সব খরচ", "All expenses", "সব খরচ"), Icons.Filled.ReceiptLong, DrawerRoute.Expenses, selected, navigate)
            DrawerItem(appText("ক্যাটাগরি", "Categories", "ক্যাটাগরি"), Icons.Filled.Folder, DrawerRoute.Categories, selected, navigate)
            DrawerItem(appText("বাজেট ব্যবস্থাপনা", "Budget management", "বাজেট ব্যবস্থাপনা"), Icons.Filled.TrackChanges, DrawerRoute.Budgets, selected, navigate)
            DrawerItem(appText("সঞ্চয়ের লক্ষ্য", "Savings goals", "সঞ্চয়ের লক্ষ্য"), Icons.Filled.Savings, DrawerRoute.Savings, selected, navigate)
            DrawerItem(appText("ঋণ ও বকেয়া", "Loans & dues", "ঋণ ও বকেয়া"), Icons.Filled.Handshake, DrawerRoute.Loans, selected, navigate)
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            DrawerItem(appText("রিপোর্ট ও বিশ্লেষণ", "Reports & analytics", "রিপোর্ট ও বিশ্লেষণ"), Icons.Filled.BarChart, DrawerRoute.Reports, selected, navigate)
            DrawerItem(appText("ক্যালেন্ডার ভিউ", "Calendar view", "ক্যালেন্ডার ভিউ"), Icons.Filled.CalendarMonth, DrawerRoute.Calendar, selected, navigate)
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text(appText("টুলস", "Tools", "টুলস"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            ComingSoonItem(appText("এআই সহকারী", "AI Helper", "এআই সহকারী"), Icons.Filled.SmartToy)
            DrawerItem(appText("ব্যাকআপ ও রিস্টোর", "Backup & restore", "ব্যাকআপ ও রিস্টোর"), Icons.Filled.CloudSync, DrawerRoute.Backup, selected, navigate)
            DrawerItem(appText("প্রোফাইল ও সেটিংস", "Profile & settings", "প্রোফাইল ও সেটিংস"), Icons.Filled.Settings, DrawerRoute.Profile, selected, navigate)
            Spacer(Modifier.height(12.dp))
            if (user != null) NavigationDrawerItem(
                label = { Text("Logout") },
                selected = false,
                onClick = logout,
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                colors = NavigationDrawerItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.error, unselectedTextColor = MaterialTheme.colorScheme.error),
            )
            Text("Version ${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth().padding(bottom = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, route: DrawerRoute, selected: DrawerRoute, navigate: (DrawerRoute) -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected == route) FontWeight.Bold else FontWeight.Medium) },
        selected = selected == route,
        onClick = { navigate(route) },
        icon = { Icon(icon, null) },
    )
}

@Composable
private fun ComingSoonItem(label: String, icon: ImageVector) {
    NavigationDrawerItem(
        modifier = Modifier.fillMaxWidth(),
        label = { Row(verticalAlignment = Alignment.CenterVertically) { Text(label); Spacer(Modifier.width(8.dp)); Text("শীঘ্রই", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } },
        selected = false,
        onClick = {},
        icon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

private fun initials(name: String) = name.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "G" }
