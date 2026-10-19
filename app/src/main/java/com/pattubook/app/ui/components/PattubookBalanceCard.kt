package com.pattubook.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkBackground
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter

@Composable
fun PattubookBalanceCard(
    netBalancePaise: Long,
    grossToCollectPaise: Long,
    grossToPayPaise: Long,
    unsettledPeopleCount: Int,
    totalPeopleCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cardShape = RoundedCornerShape(24.dp)
    val isAllSettled = grossToCollectPaise == 0L && grossToPayPaise == 0L

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurfaceVariant,
                        DarkSurface,
                        DarkBackground,
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentGreen.copy(alpha = 0.30f),
                        BorderSubtle,
                    )
                ),
                shape = cardShape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET OUTSTANDING",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )

                val peopleLabel = when {
                    totalPeopleCount == 0 -> "No active ledger"
                    isAllSettled -> "All settled"
                    unsettledPeopleCount == 1 -> "1 active contact"
                    else -> "$unsettledPeopleCount active contacts"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreenContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = peopleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = netBalancePaise,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "net_balance_anim"
            ) { net ->
                if (totalPeopleCount == 0) {
                    Column {
                        Text(
                            text = "No active people",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add someone or restore a person from Hidden people.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else if (isAllSettled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "All settled up",
                                style = MaterialTheme.typography.displayMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "No pending balances right now",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Column {
                        val netFormatted = CurrencyFormatter.formatPaiseToRupees(net)
                        Text(
                            text = netFormatted,
                            style = MaterialTheme.typography.displayLarge,
                            color = if (net >= 0) TextPrimary else AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (net > 0) "Overall owed to you" else if (net < 0) "Overall you owe" else "Balanced",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderSubtle)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gross Owed To User (To Collect)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "To Collect",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = CurrencyFormatter.formatPaiseToRupees(grossToCollectPaise),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Gross Owed By User (To Pay)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "To Pay",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = CurrencyFormatter.formatPaiseToRupees(grossToPayPaise),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
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
            netBalancePaise = 1002000L,
            grossToCollectPaise = 1502000L,
            grossToPayPaise = 500000L,
            unsettledPeopleCount = 3,
            totalPeopleCount = 4,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun PattubookBalanceCardSettledPreview() {
    PattubookTheme {
        PattubookBalanceCard(
            netBalancePaise = 0L,
            grossToCollectPaise = 0L,
            grossToPayPaise = 0L,
            unsettledPeopleCount = 0,
            totalPeopleCount = 2,
            onClick = {}
        )
    }
}
