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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextMuted
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter

@Composable
fun PersonBalanceRow(
    person: Person,
    outstandingBalancePaise: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)
    val initialLetter = person.name.trim().take(1).uppercase()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, cardShape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Initial Avatar Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )

                    val statusLabel = when {
                        outstandingBalancePaise > 0 -> "Owes you"
                        outstandingBalancePaise < 0 -> "You owe"
                        else -> "All settled"
                    }

                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            // Balance Display
            val (balanceColor, balanceText) = when {
                outstandingBalancePaise > 0 ->
                    AccentGreen to CurrencyFormatter.formatPaiseToRupees(outstandingBalancePaise)

                outstandingBalancePaise < 0 ->
                    AccentBlue to CurrencyFormatter.formatPaiseToRupees(outstandingBalancePaise)

                else ->
                    TextMuted to "Settled"
            }

            Text(
                text = balanceText,
                style = MaterialTheme.typography.titleMedium,
                color = balanceColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
fun PersonBalanceRowPreview() {
    PattubookTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersonBalanceRow(
                person = Person(id = 1, name = "Jai"),
                outstandingBalancePaise = 245000L,
                onClick = {}
            )
            PersonBalanceRow(
                person = Person(id = 2, name = "Ananya"),
                outstandingBalancePaise = 0L,
                onClick = {}
            )
            PersonBalanceRow(
                person = Person(id = 3, name = "Karthik"),
                outstandingBalancePaise = -50000L,
                onClick = {}
            )
        }
    }
}
