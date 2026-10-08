package com.dailyhisab.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dailyhisab.android.feature.home.HomeScreen
import com.dailyhisab.android.feature.category.CategoryScreen
import com.dailyhisab.android.feature.transaction.AddTransactionScreen
import com.dailyhisab.android.feature.reports.ReportsHubScreen
import com.dailyhisab.android.feature.calendar.CalendarScreen
import com.dailyhisab.android.feature.navigation.PrimaryPlaceholderScreen
import com.dailyhisab.android.feature.profile.LocalProfile
import com.dailyhisab.android.feature.profile.ProfilePreferences
import com.dailyhisab.android.feature.profile.ProfileScreen
import com.dailyhisab.android.feature.sync.CloudSyncViewModel
import com.dailyhisab.android.feature.auth.AuthViewModel
import com.dailyhisab.android.feature.home.HomeViewModel
import com.dailyhisab.android.feature.savings.SavingsGoalScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private enum class PrimaryDestination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home), Reports("Reports", Icons.Filled.BarChart),
    Add("Add", Icons.Filled.Add), Calendar("Calendar", Icons.Filled.CalendarMonth),
    Profile("Profile", Icons.Filled.Person),
}

@Composable
fun DailyHisabApp(profile: LocalProfile, preferences: ProfilePreferences) {
    val cloudSyncViewModel: CloudSyncViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()
    val summary by homeViewModel.summary.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var destination by rememberSaveable { mutableStateOf(PrimaryDestination.Home) }
    var managingCategories by rememberSaveable { mutableStateOf(false) }
    var reportsSection by rememberSaveable { mutableIntStateOf(0) }
    var drawerRoute by rememberSaveable { mutableStateOf(DrawerRoute.Dashboard) }

    fun navigate(route: DrawerRoute) {
        drawerRoute = route
        managingCategories = route == DrawerRoute.Categories
        when (route) {
            DrawerRoute.Dashboard -> destination = PrimaryDestination.Home
            DrawerRoute.Expenses -> { destination = PrimaryDestination.Reports; reportsSection = 3 }
            DrawerRoute.Reports -> { destination = PrimaryDestination.Reports; reportsSection = 0 }
            DrawerRoute.Categories -> destination = PrimaryDestination.Add
            DrawerRoute.Budgets -> { destination = PrimaryDestination.Reports; reportsSection = 1 }
            DrawerRoute.Savings -> destination = PrimaryDestination.Home
            DrawerRoute.Loans -> { destination = PrimaryDestination.Reports; reportsSection = 2 }
            DrawerRoute.Calendar -> destination = PrimaryDestination.Calendar
            DrawerRoute.Backup, DrawerRoute.Profile -> destination = PrimaryDestination.Profile
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                profile = profile,
                user = authState.user,
                summary = summary,
                selected = drawerRoute,
                close = { scope.launch { drawerState.close() } },
                navigate = ::navigate,
                logout = { authViewModel.signOut(); scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                ) {
                    PrimaryDestination.entries.forEach { item ->
                        val shownLabel = when (item) {
                            PrimaryDestination.Home -> appText("হোম", "Home")
                            PrimaryDestination.Reports -> appText("রিপোর্ট", "Reports")
                            PrimaryDestination.Add -> appText("যোগ করুন", "Add")
                            PrimaryDestination.Calendar -> appText("ক্যালেন্ডার", "Calendar")
                            PrimaryDestination.Profile -> appText("প্রোফাইল", "Profile")
                        }
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = {
                                destination = item
                                managingCategories = false
                                drawerRoute = when (item) {
                                    PrimaryDestination.Home -> DrawerRoute.Dashboard
                                    PrimaryDestination.Reports -> DrawerRoute.Reports
                                    PrimaryDestination.Calendar -> DrawerRoute.Calendar
                                    PrimaryDestination.Profile -> DrawerRoute.Profile
                                    PrimaryDestination.Add -> drawerRoute
                                }
                            },
                            icon = {
                                if (item == PrimaryDestination.Add) {
                                    FloatingActionButton(
                                        onClick = { destination = item; managingCategories = false },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) { Icon(item.icon, contentDescription = item.label) }
                                } else Icon(item.icon, contentDescription = shownLabel)
                            },
                            label = { Text(shownLabel) },
                        )
                    }
                }
            },
        ) { contentPadding ->
            if (drawerRoute == DrawerRoute.Savings) {
                SavingsGoalScreen(contentPadding = contentPadding, currency = profile.currency, back = { navigate(DrawerRoute.Dashboard) })
            } else when (destination) {
                PrimaryDestination.Home -> HomeScreen(
                    contentPadding = contentPadding,
                    viewModel = homeViewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAddExpense = { destination = PrimaryDestination.Add; managingCategories = false },
                )
                PrimaryDestination.Reports -> ReportsHubScreen(contentPadding = contentPadding, initialSection = reportsSection)
                PrimaryDestination.Calendar -> CalendarScreen(contentPadding = contentPadding)
                PrimaryDestination.Profile -> ProfileScreen(contentPadding, profile, preferences, authViewModel = authViewModel, cloudSyncViewModel = cloudSyncViewModel)
                PrimaryDestination.Add -> if (managingCategories) {
                    CategoryScreen(contentPadding = contentPadding, onBack = { navigate(DrawerRoute.Dashboard) })
                } else {
                    AddTransactionScreen(contentPadding = contentPadding, onManageCategories = { managingCategories = true })
                }
            }
        }
    }
}
