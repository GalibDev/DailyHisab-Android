package com.dailyhisab.android.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.BuildConfig
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import java.math.BigDecimal

private enum class ProfilePage { Main, Details, Personalization, Security, About }

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    profile: LocalProfile,
    preferences: ProfilePreferences,
    viewModel: ProfileViewModel = viewModel(),
) {
    var page by remember { mutableStateOf(ProfilePage.Main) }
    when (page) {
        ProfilePage.Main -> ProfileMain(contentPadding, profile, viewModel) { page = it }
        ProfilePage.Details -> ProfileDetails(contentPadding, profile, preferences) { page = ProfilePage.Main }
        ProfilePage.Personalization -> Personalization(contentPadding, profile, preferences) { page = ProfilePage.Main }
        ProfilePage.Security -> InformationPage(contentPadding, "Security & password", listOf(
            "Your finance data is stored locally on this device.",
            "Create password, Google sign-in and password recovery will arrive with Authentication in Batch 7.",
            "Never share your User ID or device backup with an untrusted person.",
        )) { page = ProfilePage.Main }
        ProfilePage.About -> InformationPage(contentPadding, "About Daily Hisab", listOf(
            "Daily Hisab is a native Kotlin expense tracker built for simple daily money management.",
            "Version ${BuildConfig.VERSION_NAME}",
            "Privacy: current financial and profile data stays on this device until cloud sync is enabled.",
        )) { page = ProfilePage.Main }
    }
}

@Composable
private fun ProfileMain(
    contentPadding: PaddingValues,
    profile: LocalProfile,
    viewModel: ProfileViewModel,
    openPage: (ProfilePage) -> Unit,
) {
    val stats by viewModel.stats.collectAsState()
    var authMessage by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Profile", style = MaterialTheme.typography.headlineMedium) }
        item {
            DailyHisabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(72.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(initials(profile.displayName), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(profile.email.ifBlank { "Local guest profile" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AssistChip(onClick = { authMessage = true }, label = { Text("Create account") }, leadingIcon = { Icon(Icons.Filled.PersonAdd, null) })
                    }
                    IconButton(onClick = { openPage(ProfilePage.Details) }) { Icon(Icons.Filled.ChevronRight, "Profile details") }
                }
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("Expense", money(stats.totalExpenseMinor))
                    ProfileStat("Entries", stats.transactionCount.toString())
                    ProfileStat("Active days", stats.activeDays.toString())
                }
            }
        }
        if (authMessage) item {
            AssistChip(onClick = { authMessage = false }, label = { Text("Account creation will be enabled in Batch 7") }, leadingIcon = { Icon(Icons.Filled.Info, null) })
        }
        item { Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            DailyHisabCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenu("Personal information", Icons.Filled.Person, { openPage(ProfilePage.Details) })
                HorizontalDivider()
                ProfileMenu("Security & password", Icons.Filled.Security, { openPage(ProfilePage.Security) })
                HorizontalDivider()
                ProfileMenu("Backup & cloud sync", Icons.Filled.CloudUpload, { authMessage = true }, "Coming next")
            }
        }
        item { Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            DailyHisabCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenu("Personalization", Icons.Filled.Palette, { openPage(ProfilePage.Personalization) }, profile.themeStyle)
                HorizontalDivider()
                ProfileMenu("Language", Icons.Filled.Language, { openPage(ProfilePage.Personalization) }, profile.language)
                HorizontalDivider()
                ProfileMenu("Currency", Icons.Filled.CurrencyExchange, { openPage(ProfilePage.Personalization) }, profile.currency)
            }
        }
        item {
            DailyHisabCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenu("About Daily Hisab", Icons.Filled.Info, { openPage(ProfilePage.About) }, "v${BuildConfig.VERSION_NAME}")
                HorizontalDivider()
                ProfileMenu("Privacy policy", Icons.Filled.PrivacyTip, { openPage(ProfilePage.About) })
            }
        }
    }
}

@Composable
private fun ProfileDetails(contentPadding: PaddingValues, profile: LocalProfile, preferences: ProfilePreferences, back: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    ScreenColumn(contentPadding, "Personal information", back) {
        DailyHisabCard {
            DetailRow("Name", profile.displayName)
            DetailRow("Email", profile.email.ifBlank { "Not connected" })
            DetailRow("Account", "Local guest")
            Text("User ID", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(profile.userId, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Use this ID when contacting support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) { Text("Edit profile") }
        }
    }
    if (editing) EditProfileDialog(profile, { editing = false }) { name, email ->
        preferences.update { it.copy(displayName = name, email = email) }; editing = false
    }
}

@Composable
private fun Personalization(contentPadding: PaddingValues, profile: LocalProfile, preferences: ProfilePreferences, back: () -> Unit) {
    ScreenColumn(contentPadding, "Personalization", back) {
        SettingChoices("Theme mode", listOf("System", "Light", "Dark"), profile.themeMode) { preferences.update { p -> p.copy(themeMode = it) } }
        SettingChoices("Design", listOf("Aurora", "Default"), profile.themeStyle) { preferences.update { p -> p.copy(themeStyle = it) } }
        SettingChoices("Language", listOf("Default", "বাংলা", "English"), profile.language) { preferences.update { p -> p.copy(language = it) } }
        SettingChoices("Currency", listOf("BDT", "USD"), profile.currency) { preferences.update { p -> p.copy(currency = it) } }
    }
}

@Composable
private fun InformationPage(contentPadding: PaddingValues, title: String, paragraphs: List<String>, back: () -> Unit) {
    ScreenColumn(contentPadding, title, back) {
        DailyHisabCard { paragraphs.forEach { Text(it); Spacer(Modifier.height(10.dp)) } }
    }
}

@Composable
private fun ScreenColumn(contentPadding: PaddingValues, title: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(contentPadding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun SettingChoices(title: String, values: List<String>, selected: String, select: (String) -> Unit) {
    DailyHisabCard {
        Text(title, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            values.forEach { value -> FilterChip(selected == value, { select(value) }, { Text(value) }) }
        }
    }
}

@Composable
private fun ProfileMenu(title: String, icon: ImageVector, click: () -> Unit, value: String? = null) {
    TextButton(onClick = click, modifier = Modifier.fillMaxWidth().height(62.dp)) {
        Icon(icon, null)
        Text(title, Modifier.padding(start = 14.dp).weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start, color = MaterialTheme.colorScheme.onSurface)
        if (value != null) Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ProfileStat(label: String, value: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable private fun DetailRow(label: String, value: String) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp)) }

@Composable
private fun EditProfileDialog(profile: LocalProfile, dismiss: () -> Unit, save: (String, String) -> Unit) {
    var name by remember { mutableStateOf(profile.displayName) }
    var email by remember { mutableStateOf(profile.email) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Edit profile") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(email, { email = it }, label = { Text("Email (optional)") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) save(name.trim(), email.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

internal fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "G" }
private fun money(value: Long) = "৳ " + BigDecimal(value).movePointLeft(2).stripTrailingZeros().toPlainString()
