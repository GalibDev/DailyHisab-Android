package com.dailyhisab.android.feature.profile

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.BuildConfig
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.feature.auth.AuthScreen
import com.dailyhisab.android.feature.auth.AuthUiState
import com.dailyhisab.android.feature.auth.AuthUser
import com.dailyhisab.android.feature.auth.AuthViewModel
import java.math.BigDecimal
import com.dailyhisab.android.feature.sync.CloudSyncViewModel
import com.dailyhisab.android.feature.sync.SyncPhase
import com.dailyhisab.android.feature.sync.SyncUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.ContextCompat
import com.dailyhisab.android.notifications.ReminderPreferences
import com.dailyhisab.android.notifications.ReminderScheduler
import coil3.compose.AsyncImage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri

private enum class ProfilePage { Main, Details, Personalization, Notifications, Security, Sync, Payment, Help, Contact, About, Privacy }

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    profile: LocalProfile,
    preferences: ProfilePreferences,
    viewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    cloudSyncViewModel: CloudSyncViewModel,
) {
    var page by remember { mutableStateOf(ProfilePage.Main) }
    var showAuth by remember { mutableStateOf(false) }
    val authState by authViewModel.state.collectAsState()
    val syncState by cloudSyncViewModel.state.collectAsState()
    LaunchedEffect(authState.user) {
        authState.user?.let { user ->
            preferences.update { it.copy(displayName = user.displayName.ifBlank { it.displayName }, email = user.email) }
            FirebaseDatabase.getInstance().reference.child("users/${user.uid}/profile").get().addOnSuccessListener { snapshot ->
                val photo = snapshot.child("photoUrl").getValue(String::class.java).orEmpty()
                preferences.update { it.copy(photoUrl = photo.ifBlank { it.photoUrl }) }
            }
            showAuth = false
        }
    }
    if (showAuth && authState.user == null) {
        AuthScreen(contentPadding, authState, authViewModel) { showAuth = false }
        return
    }
    when (page) {
        ProfilePage.Main -> ProfileMain(
            contentPadding, profile, viewModel, authState,
            openAuth = { showAuth = true },
            signOut = authViewModel::signOut,
            syncState = syncState,
        ) { page = it }
        ProfilePage.Details -> ProfileDetails(contentPadding, profile, preferences, authState.user) { page = ProfilePage.Main }
        ProfilePage.Personalization -> Personalization(contentPadding, profile, preferences) { page = ProfilePage.Main }
        ProfilePage.Notifications -> NotificationSettingsScreen(contentPadding) { page = ProfilePage.Main }
        ProfilePage.Security -> SecurityScreen(contentPadding, authState, authViewModel, { showAuth = true }) { page = ProfilePage.Main }
        ProfilePage.Sync -> CloudSyncScreen(contentPadding, authState.user, syncState, cloudSyncViewModel) { page = ProfilePage.Main }
        ProfilePage.Payment -> PaymentMethodsScreen(contentPadding, profile, preferences) { page = ProfilePage.Main }
        ProfilePage.Help -> InformationPage(contentPadding, "Help Center", listOf("Add income or expense from the + button or Quick Add.", "Use Reports to review, export PDF/Excel, and share monthly cards.", "For sync issues, sign in with the same account and open Backup & cloud sync.")) { page = ProfilePage.Main }
        ProfilePage.Contact -> InformationPage(contentPadding, "Contact Us", listOf("Email: mirza.galib.palash@gmail.com", "Website: mirzagalib.xyz", "When requesting support, include the User ID shown in Personal information.")) { page = ProfilePage.Main }
        ProfilePage.About -> InformationPage(contentPadding, "About Daily Hisab", listOf(
            "Daily Hisab is a native Kotlin expense tracker built for simple daily money management.",
            "Version ${BuildConfig.VERSION_NAME}",
            "Privacy: signed-in financial data is encrypted in transit and synced with your Daily Hisab account.",
        )) { page = ProfilePage.Main }
        ProfilePage.Privacy -> InformationPage(contentPadding, "Privacy Policy", listOf("Your finance data stays on your device and is synced to your private Firebase account only after sign-in.", "Authentication and cloud traffic are encrypted in transit. Daily Hisab does not sell personal or financial data.", "You may delete your account from Security & password. Download or back up data first if required.")) { page = ProfilePage.Main }
    }
}

@Composable
private fun ProfileMain(
    contentPadding: PaddingValues,
    profile: LocalProfile,
    viewModel: ProfileViewModel,
    authState: AuthUiState,
    openAuth: () -> Unit,
    signOut: () -> Unit,
    syncState: SyncUiState,
    openPage: (ProfilePage) -> Unit,
) {
    val stats by viewModel.stats.collectAsState()
    var authMessage by remember { mutableStateOf(false) }
    val user = authState.user
    val shownName = user?.displayName?.ifBlank { profile.displayName } ?: profile.displayName
    val shownEmail = user?.email?.ifBlank { profile.email } ?: profile.email
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
                        Box(contentAlignment = Alignment.Center) { Text(initials(shownName), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(shownName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(shownEmail.ifBlank { "Local guest profile" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (user == null) AssistChip(onClick = openAuth, label = { Text("Sign in or create account") }, leadingIcon = { Icon(Icons.Filled.PersonAdd, null) })
                        else AssistChip(onClick = {}, label = { Text(if (user.isGoogleUser) "Google account" else "Email account") }, leadingIcon = { Icon(Icons.Filled.VerifiedUser, null) })
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
            AssistChip(onClick = { authMessage = false }, label = { Text("Cloud data sync will be enabled in Batch 8") }, leadingIcon = { Icon(Icons.Filled.Info, null) })
        }
        item { Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            DailyHisabCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenu("Personal information", Icons.Filled.Person, { openPage(ProfilePage.Details) })
                HorizontalDivider()
                ProfileMenu("Security & password", Icons.Filled.Security, { openPage(ProfilePage.Security) })
                HorizontalDivider()
                ProfileMenu("Backup & cloud sync", Icons.Filled.CloudUpload, { openPage(ProfilePage.Sync) }, syncLabel(syncState))
                HorizontalDivider()
                ProfileMenu("Payment methods", Icons.Filled.CreditCard, { openPage(ProfilePage.Payment) })
                if (user != null) {
                    HorizontalDivider()
                    ProfileMenu("Logout", Icons.AutoMirrored.Filled.Logout, signOut)
                }
            }
        }
        item { Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            DailyHisabCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenu("Personalization", Icons.Filled.Palette, { openPage(ProfilePage.Personalization) }, profile.themeStyle)
                HorizontalDivider()
                ProfileMenu("Notifications & reminders", Icons.Filled.NotificationsActive, { openPage(ProfilePage.Notifications) })
                HorizontalDivider()
                ProfileMenu("Language", Icons.Filled.Language, { openPage(ProfilePage.Personalization) }, profile.language)
                HorizontalDivider()
                ProfileMenu("Currency", Icons.Filled.CurrencyExchange, { openPage(ProfilePage.Personalization) }, profile.currency)
            }
        }
        item {
            DailyHisabCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenu("Help Center", Icons.Filled.Help, { openPage(ProfilePage.Help) })
                HorizontalDivider()
                ProfileMenu("Contact Us", Icons.Filled.ContactSupport, { openPage(ProfilePage.Contact) })
                HorizontalDivider()
                ProfileMenu("About Daily Hisab", Icons.Filled.Info, { openPage(ProfilePage.About) }, "v${BuildConfig.VERSION_NAME}")
                HorizontalDivider()
                ProfileMenu("Privacy policy", Icons.Filled.PrivacyTip, { openPage(ProfilePage.Privacy) })
            }
        }
    }
}

@Composable
private fun NotificationSettingsScreen(contentPadding: PaddingValues, back: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { ReminderPreferences(context.applicationContext) }
    val settings by preferences.settings.collectAsState()
    var permissionGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    fun update(change: (com.dailyhisab.android.notifications.ReminderSettings) -> com.dailyhisab.android.notifications.ReminderSettings) {
        preferences.update(change)
        ReminderScheduler.scheduleAll(context.applicationContext)
    }
    ScreenColumn(contentPadding, "Notifications & reminders", back) {
        if (!permissionGranted && Build.VERSION.SDK_INT >= 33) {
            DailyHisabCard {
                Text("Notification permission required", fontWeight = FontWeight.Bold)
                Text("Allow notifications to receive loan, budget and daily reminders.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.fillMaxWidth()) { Text("Allow notifications") }
            }
        }
        ReminderToggle("Daily expense reminder", "Remind only when today's হিসাব is empty", settings.dailyEnabled) { update { s -> s.copy(dailyEnabled = it) } }
        TimeSetting("Daily reminder time", settings.dailyHour, settings.dailyMinute) { hour, minute -> update { it.copy(dailyHour = hour, dailyMinute = minute) } }
        ReminderToggle("Loan due notification", "Alerts one day before, on due date and when overdue", settings.loanEnabled) { update { s -> s.copy(loanEnabled = it) } }
        TimeSetting("Loan reminder time", settings.loanHour, settings.loanMinute) { hour, minute -> update { it.copy(loanHour = hour, loanMinute = minute) } }
        ReminderToggle("Budget limit warning", "Warn when spending reaches ${settings.budgetThreshold}%", settings.budgetEnabled) { update { s -> s.copy(budgetEnabled = it) } }
        SettingChoices("Warning threshold", listOf("70%", "80%", "90%", "100%"), "${settings.budgetThreshold}%") {
            update { current -> current.copy(budgetThreshold = it.removeSuffix("%").toInt()) }
        }
        Text("Reminders are restored automatically after device restart or app update.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReminderToggle(title: String, description: String, checked: Boolean, change: (Boolean) -> Unit) {
    DailyHisabCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, change)
        }
    }
}

@Composable
private fun TimeSetting(title: String, hour: Int, minute: Int, change: (Int, Int) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { TimePickerDialog(context, { _, h, m -> change(h, m) }, hour, minute, false).show() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Schedule, null)
        Spacer(Modifier.width(8.dp))
        Text("$title: ${String.format("%02d:%02d", hour, minute)}")
    }
}

@Composable
private fun CloudSyncScreen(
    contentPadding: PaddingValues,
    user: AuthUser?,
    state: SyncUiState,
    viewModel: CloudSyncViewModel,
    back: () -> Unit,
) {
    var confirmRestore by remember { mutableStateOf(false) }
    ScreenColumn(contentPadding, "Backup & cloud sync", back) {
        DailyHisabCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (state.phase) {
                        SyncPhase.Synced -> Icons.Filled.CloudDone
                        SyncPhase.Syncing -> Icons.Filled.Sync
                        SyncPhase.Offline -> Icons.Filled.CloudOff
                        SyncPhase.Error -> Icons.Filled.Error
                        SyncPhase.Guest -> Icons.Filled.CloudQueue
                    },
                    null,
                    tint = if (state.phase == SyncPhase.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(syncLabel(state), fontWeight = FontWeight.Bold)
                    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.lastSyncEpochMillis > 0) Text("Last sync: ${formatSyncTime(state.lastSyncEpochMillis)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (user == null) {
            Text("Sign in from Profile to enable private cloud backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Button(onClick = viewModel::backupNow, Modifier.fillMaxWidth(), enabled = state.phase != SyncPhase.Syncing) { Text("Back up now") }
            OutlinedButton(onClick = { confirmRestore = true }, Modifier.fillMaxWidth(), enabled = state.phase != SyncPhase.Syncing) { Text("Restore from cloud") }
            Text("Automatic sync runs after local changes. If local and cloud data conflict, the newest backup wins. First sign-in merges guest data with the account backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.history.isNotEmpty()) {
            Text("Backup & restore history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.history.forEach { entry ->
                val parts = entry.split('|', limit = 3)
                val time = parts.getOrNull(0)?.toLongOrNull()?.let(::formatSyncTime).orEmpty()
                ListItem(
                    headlineContent = { Text(parts.getOrNull(2).orEmpty()) },
                    supportingContent = { Text(time) },
                    leadingContent = { Icon(if (parts.getOrNull(1) == "SUCCESS") Icons.Filled.CheckCircle else Icons.Filled.Error, null, tint = if (parts.getOrNull(1) == "SUCCESS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
                )
            }
        }
    }
    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("Restore cloud backup?") },
        text = { Text("Current local finance data will be replaced by the latest cloud backup.") },
        confirmButton = { TextButton(onClick = { confirmRestore = false; viewModel.restoreNow() }) { Text("Restore") } },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ProfileDetails(contentPadding: PaddingValues, profile: LocalProfile, preferences: ProfilePreferences, user: AuthUser?, back: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            if (user == null) {
                preferences.update { it.copy(photoUrl = uri.toString()) }; uploadMessage = "Profile image saved on this device"
            } else {
                uploadMessage = "Uploading profile image…"
                val ref = FirebaseStorage.getInstance().reference.child("profileImages/${user.uid}.jpg")
                ref.putFile(uri).continueWithTask { ref.downloadUrl }.addOnSuccessListener { download ->
                    val url = download.toString()
                    preferences.update { it.copy(photoUrl = url) }
                    FirebaseDatabase.getInstance().reference.child("users/${user.uid}/profile").updateChildren(mapOf("photoUrl" to url, "displayName" to (user.displayName.ifBlank { profile.displayName }), "email" to user.email))
                    uploadMessage = "Profile image updated"
                }.addOnFailureListener { uploadMessage = it.localizedMessage ?: "Upload failed" }
            }
        }
    }
    ScreenColumn(contentPadding, "Personal information", back) {
        DailyHisabCard {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(Modifier.size(92.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    if (profile.photoUrl.isNotBlank()) AsyncImage(profile.photoUrl, "Profile image", Modifier.fillMaxSize())
                    else Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null, Modifier.size(44.dp)) }
                }
            }
            OutlinedButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Upload profile image") }
            uploadMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            DetailRow("Name", user?.displayName?.ifBlank { profile.displayName } ?: profile.displayName)
            DetailRow("Email", user?.email?.ifBlank { profile.email }?.ifBlank { "Not connected" } ?: "Not connected")
            DetailRow("Account", if (user == null) "Local guest" else if (user.isGoogleUser) "Google account" else "Email account")
            Text("User ID", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(user?.uid ?: profile.userId, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun PaymentMethodsScreen(contentPadding: PaddingValues, profile: LocalProfile, preferences: ProfilePreferences, back: () -> Unit) {
    val available = listOf("Cash", "bKash", "Nagad", "Bank", "Card")
    val selected = profile.paymentMethods.split(',').filter(String::isNotBlank).toSet()
    ScreenColumn(contentPadding, "Payment methods", back) {
        Text("Choose the methods shown while adding a transaction.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        available.forEach { method ->
            DailyHisabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (method == "Cash") Icons.Filled.Payments else Icons.Filled.AccountBalanceWallet, null)
                    Text(method, Modifier.padding(start = 12.dp).weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(method in selected, onCheckedChange = { enabled ->
                        val updated = if (enabled) selected + method else selected - method
                        if (updated.isNotEmpty()) preferences.update { it.copy(paymentMethods = updated.joinToString(",")) }
                    })
                }
            }
        }
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
private fun SecurityScreen(
    contentPadding: PaddingValues,
    state: AuthUiState,
    viewModel: AuthViewModel,
    openAuth: () -> Unit,
    back: () -> Unit,
) {
    var email by remember(state.user?.email) { mutableStateOf(state.user?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    ScreenColumn(contentPadding, "Security & password", back) {
        if (state.user == null) {
            DailyHisabCard {
                Text("Sign in to manage password security.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(onClick = openAuth, modifier = Modifier.fillMaxWidth()) { Text("Sign in or create account") }
            }
        } else {
            DailyHisabCard {
                DetailRow("Signed in email", state.user.email)
                Text(if (state.user.hasPassword) "Change password" else "Create password for this Google account", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    password, { password = it }, Modifier.fillMaxWidth(), label = { Text("New password") }, singleLine = true,
                    visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } },
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { if (state.user.hasPassword) viewModel.changePassword(password) else viewModel.createPassword(password) },
                    modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                ) { Text(if (state.user.hasPassword) "Change password" else "Create password") }
            }
            DailyHisabCard {
                Text("Forgot password", fontWeight = FontWeight.Bold)
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { viewModel.sendPasswordReset(email) }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Send reset email") }
            }
            OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Filled.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Delete account") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Permanently delete account?") },
        text = { Text("This removes the Firebase login account. Back up any data you need first. Recent sign-in may be required by Firebase.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteAccount() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
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
private fun syncLabel(state: SyncUiState) = when (state.phase) {
    SyncPhase.Guest -> "Sign in required"
    SyncPhase.Syncing -> "Syncing"
    SyncPhase.Synced -> "Synced"
    SyncPhase.Offline -> "Waiting for internet"
    SyncPhase.Error -> "Sync needs attention"
}
private fun formatSyncTime(epochMillis: Long): String = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
