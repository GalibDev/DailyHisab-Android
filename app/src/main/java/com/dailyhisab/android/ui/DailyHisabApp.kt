package com.dailyhisab.android.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dailyhisab.android.feature.home.HomeScreen
import com.dailyhisab.android.feature.navigation.PrimaryPlaceholderScreen

private enum class PrimaryDestination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home), Reports("Reports", Icons.Filled.BarChart),
    Add("Add", Icons.Filled.Add), Calendar("Calendar", Icons.Filled.CalendarMonth),
    Profile("Profile", Icons.Filled.Person),
}

@Composable
fun DailyHisabApp() {
    var destination by rememberSaveable { mutableStateOf(PrimaryDestination.Home) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                PrimaryDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            if (item == PrimaryDestination.Add) {
                                FloatingActionButton(
                                    onClick = { destination = item },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) { Icon(item.icon, contentDescription = item.label) }
                            } else Icon(item.icon, contentDescription = item.label)
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { contentPadding ->
        when (destination) {
            PrimaryDestination.Home -> HomeScreen(contentPadding = contentPadding)
            else -> PrimaryPlaceholderScreen(destination.label, destination.icon, contentPadding)
        }
    }
}

