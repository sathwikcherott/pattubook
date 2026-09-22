package com.pattubook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter

@Composable
fun PattubookBalanceCard(
    outstandingBalancePaise: Long,
    peopleCount: Int,
    modifier: Modifier = Modifier,
    totalGivenPaise: Long = 0L,
    totalReturnedPaise: Long = 0L,
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurfaceVariant,
                        DarkSurface,
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentGreen.copy(alpha = 0.25f),
                        BorderSubtle,
                    )
                ),
                shape = shape
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Total outstanding",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = CurrencyFormatter.formatPaiseToRupees(outstandingBalancePaise),
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            val peopleLabel = when (peopleCount) {
                0 -> "No active borrowing ledger"
                1 -> "1 person owes you"
                else -> "$peopleCount people owe you"
            }

            Text(
                text = peopleLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = AccentGreen,
            )

            if (totalGivenPaise > 0 || totalReturnedPaise > 0) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderSubtle)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Given",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = CurrencyFormatter.formatPaiseToRupees(totalGivenPaise),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Returned",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = CurrencyFormatter.formatPaiseToRupees(totalReturnedPaise),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PattubookBalanceCardPreview() {
    PattubookTheme {
        PattubookBalanceCard(
            outstandingBalancePaise = 1245000L,
            peopleCount = 4,
            totalGivenPaise = 1520000L,
            totalReturnedPaise = 275000L,
        )
    }
}
