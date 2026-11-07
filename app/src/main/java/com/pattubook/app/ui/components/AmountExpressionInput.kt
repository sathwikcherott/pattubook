package com.pattubook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.AmountInputUtil
import com.pattubook.app.ui.util.CurrencyFormatter

private const val BACKSPACE_KEY = '\b'

private val amountKeyRows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('+', '0', '.'),
)

@Composable
fun AmountExpressionInput(
    expression: String,
    accent: Color,
    onKeyPress: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val liveTotalPaise = AmountInputUtil.previewAdditionExpressionToPaise(expression)
    val totalText = when {
        expression.isEmpty() -> "₹0"
        liveTotalPaise != null -> CurrencyFormatter.formatPaiseToRupees(liveTotalPaise)
        else -> "₹—"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Amount (₹)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        val displayShape = RoundedCornerShape(12.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(displayShape)
                .background(DarkSurfaceVariant)
                .border(1.dp, BorderSubtle, displayShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AmountInputUtil.formatAdditionExpression(expression).ifEmpty { "Enter amount" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                AmountBackspaceKey(
                    accent = accent,
                    onClick = { onKeyPress(BACKSPACE_KEY) }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = totalText,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        amountKeyRows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    AmountKey(
                        key = key,
                        accent = accent,
                        onClick = { onKeyPress(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AmountKey(
    key: Char,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOperator = key == '+'
    val keyShape = RoundedCornerShape(10.dp)
    val accessibleLabel = when (key) {
        '+' -> "Plus"
        '.' -> "Decimal point"
        else -> key.toString()
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(keyShape)
            .background(if (isOperator) accent.copy(alpha = 0.16f) else DarkSurfaceVariant)
            .border(1.dp, if (isOperator) accent.copy(alpha = 0.5f) else BorderSubtle, keyShape)
            .semantics {
                contentDescription = accessibleLabel
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (isOperator) accent else TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AmountBackspaceKey(
    accent: Color,
    onClick: () -> Unit,
) {
    val keyShape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = "Backspace"
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(keyShape)
                .background(DarkSurfaceVariant)
                .border(1.dp, BorderSubtle, keyShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌫",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
