package com.lifepad.app.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifepad.app.domain.finance.FinancialInsight
import com.lifepad.app.domain.finance.InsightSeverity
import com.lifepad.app.domain.finance.InsightType
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor

@Composable
fun InsightCard(
    insight: FinancialInsight,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = when (insight.severity) {
        InsightSeverity.WARNING -> MaterialTheme.colorScheme.errorContainer
        InsightSeverity.POSITIVE -> MaterialTheme.colorScheme.primaryContainer
        InsightSeverity.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (insight.severity) {
        InsightSeverity.WARNING -> MaterialTheme.colorScheme.onErrorContainer
        InsightSeverity.POSITIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        InsightSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon = when (insight.type) {
        InsightType.SPENDING_TREND -> if ((insight.value ?: 0.0) > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
        InsightType.BUDGET_ALERT -> Icons.Filled.Warning
        InsightType.BILL_DUE_SOON -> Icons.Filled.Warning
        else -> Icons.Filled.Info
    }

    val iconTint = when (insight.severity) {
        InsightSeverity.WARNING -> ExpenseColor
        InsightSeverity.POSITIVE -> IncomeColor
        InsightSeverity.INFO -> contentColor
    }

    Card(
        modifier = modifier.then(
            if (compact) Modifier.width(220.dp) else Modifier.fillMaxWidth()
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        if (compact) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = insight.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor
                    )
                    Text(
                        text = insight.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
