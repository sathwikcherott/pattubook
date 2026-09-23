package com.pattubook.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentBlueContainer
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextMuted
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonBalanceRow(
    person: Person,
    outstandingBalancePaise: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onHideClick: () -> Unit = {},
    onUnhideClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    val cardShape = RoundedCornerShape(18.dp)
    val initialLetter = person.name.trim().take(1).uppercase()
    var dropdownExpanded by remember { mutableStateOf(false) }

    val (statusLabel, statusColor, statusContainer) = when {
        outstandingBalancePaise > 0 -> Triple("Owes you", AccentGreen, AccentGreenContainer)
        outstandingBalancePaise < 0 -> Triple("You owe", AccentBlue, AccentBlueContainer)
        else -> Triple("Settled", TextMuted, DarkSurfaceVariant)
    }

    val alphaAmount = if (person.isHidden) 0.65f else 1.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .alpha(alphaAmount)
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { dropdownExpanded = true }
            )
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
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(statusContainer)
                        .border(1.dp, statusColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (person.isHidden) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Hidden",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                fontWeight = FontWeight.Bold
            )
        }

        // Long-Press Contextual Action Menu
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
            modifier = Modifier.background(DarkSurfaceVariant)
        ) {
            if (person.isHidden) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Unhide Person",
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Unhide Person",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    onClick = {
                        dropdownExpanded = false
                        onUnhideClick()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Hide Person",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Hide Person",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    onClick = {
                        dropdownExpanded = false
                        onHideClick()
                    }
                )
            }

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Person",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Delete Person",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    dropdownExpanded = false
                    onDeleteClick()
                }
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
                person = Person(id = 2, name = "Ananya", isHidden = true),
                outstandingBalancePaise = 30000L,
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
