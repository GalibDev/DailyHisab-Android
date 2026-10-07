package com.dailyhisab.android.feature.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyhisab.android.core.designsystem.DailyHisabCard
import com.dailyhisab.android.domain.model.Category

@Composable
fun CategoryScreen(contentPadding: PaddingValues, onBack: (() -> Unit)? = null, viewModel: CategoryViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    var editing by remember { mutableStateOf<Category?>(null) }
    var dialogOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Categories", style = MaterialTheme.typography.headlineMedium)
                    Text("Add, edit and arrange expense categories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(onClick = { editing = null; dialogOpen = true }) {
                    Icon(Icons.Filled.Add, "Add category")
                }
            }
        }
        itemsIndexed(categories, key = { _, item -> item.id }) { index, category ->
            DailyHisabCard(contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Category, null, tint = Color(category.colorArgb), modifier = Modifier.size(28.dp))
                    Text(category.name, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.move(category, -1) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Move up")
                    }
                    IconButton(onClick = { viewModel.move(category, 1) }, enabled = index < categories.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Move down")
                    }
                    IconButton(onClick = { editing = category; dialogOpen = true }) {
                        Icon(Icons.Filled.Edit, "Edit")
                    }
                    if (!category.isDefault) {
                        IconButton(onClick = { viewModel.delete(category) }) {
                            Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (dialogOpen) CategoryDialog(
        category = editing,
        onDismiss = { dialogOpen = false },
        onSave = { viewModel.save(it, editing); dialogOpen = false },
    )
}

@Composable
private fun CategoryDialog(category: Category?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(category) { mutableStateOf(category?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add category" else "Edit category") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Category name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
