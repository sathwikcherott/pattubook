package com.pattubook.app.ui.breakdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pattubook.app.data.local.entity.Person
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
import com.pattubook.app.ui.viewmodel.PeopleUiState
import kotlin.math.max

@Composable
fun OutstandingBreakdownScreen(
    uiState: PeopleUiState,
    onBackClick: () -> Unit,
    onPersonClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Filter contacts who have a positive outstanding balance (> 0)
    val positivePeople = uiState.people.filter { person ->
        (uiState.outstandingBalancePaiseByPerson[person.id] ?: 0L) > 0L
    }

    val totalOutstandingPaise = max(0L, uiState.totalOutstandingPaise)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Outstanding Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Total Summary Card
            val summaryCardShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(summaryCardShape)
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle, summaryCardShape)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL OUTSTANDING TO COLLECT",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = CurrencyFormatter.formatPaiseToRupees(totalOutstandingPaise),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val subtitle = if (positivePeople.isEmpty()) {
                        "All settled"
                    } else {
                        "Distributed across ${positivePeople.size} contacts"
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Text(
                text = "Share of Total Outstanding",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }

                positivePeople.isEmpty() || totalOutstandingPaise <= 0L -> {
                    // Empty Settled State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "You're all settled",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹0 outstanding. Nobody owes you money right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(
                            items = positivePeople,
                            key = { person -> person.id }
                        ) { person ->
                            val personBalancePaise = uiState.outstandingBalancePaiseByPerson[person.id] ?: 0L
                            val progressFraction = (personBalancePaise.toFloat() / max(1L, totalOutstandingPaise).toFloat()).coerceIn(0.01f, 1f)
                            val percentageInt = (progressFraction * 100).toInt()

                            val cardShape = RoundedCornerShape(18.dp)
                            val initialLetter = person.name.trim().take(1).uppercase()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(DarkSurface)
                                    .border(1.dp, BorderSubtle, cardShape)
                                    .clickable { onPersonClick(person.id) }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
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

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = person.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "$percentageInt% of total outstanding",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = CurrencyFormatter.formatPaiseToRupees(personBalancePaise),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = AccentGreen,
                                        trackColor = DarkSurfaceVariant,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun OutstandingBreakdownScreenPreview() {
    PattubookTheme {
        val samplePeople = listOf(
            Person(id = 1, name = "Jai"),
            Person(id = 2, name = "Karthik"),
        )
        val sampleBalances = mapOf(
            1L to 245000L,
            2L to 100000L,
        )
        OutstandingBreakdownScreen(
            uiState = PeopleUiState(
                people = samplePeople,
                outstandingBalancePaiseByPerson = sampleBalances,
                totalOutstandingPaise = 345000L,
                isLoading = false
            ),
            onBackClick = {},
            onPersonClick = {}
        )
    }
}
