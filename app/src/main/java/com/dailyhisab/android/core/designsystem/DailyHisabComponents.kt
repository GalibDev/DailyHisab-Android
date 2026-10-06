package com.dailyhisab.android.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyhisab.android.ui.theme.LocalDailyHisabSpacing

@Composable
fun DailyHisabCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun DailyHisabPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(vertical = 15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DailyHisabSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val spacing = LocalDailyHisabSpacing.current
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = spacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

