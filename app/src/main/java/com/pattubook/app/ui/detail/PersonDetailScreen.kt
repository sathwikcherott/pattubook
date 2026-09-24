package com.pattubook.app.ui.detail

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.components.AddTransactionDialog
import com.pattubook.app.ui.components.PattubookQuickActions
import com.pattubook.app.ui.components.TransactionRow
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
import com.pattubook.app.ui.viewmodel.PersonDetailUiEvent
import com.pattubook.app.ui.viewmodel.PersonDetailUiState
import kotlinx.coroutines.flow.Flow

@Composable
fun PersonDetailScreen(
    uiState: PersonDetailUiState,
    onBackClick: () -> Unit,
    onGiveMoneyConfirm: (amountPaise: Long, note: String?, timestamp: Long) -> Unit,
    onRecordReturnConfirm: (amountPaise: Long, note: String?, timestamp: Long) -> Unit,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRedoClick: () -> Unit = {},
    eventFlow: Flow<PersonDetailUiEvent>? = null,
) {
    var activeTransactionType by remember { mutableStateOf<LedgerEntryType?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (!uiState.isLoading && uiState.person == null) {
            onBackClick()
        }
    }

    LaunchedEffect(eventFlow) {
        eventFlow?.collect { event ->
            when (event) {
                is PersonDetailUiEvent.UndoSuccess -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Transaction undone",
                        actionLabel = "Redo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onRedoClick()
                    }
                }
                is PersonDetailUiEvent.UndoNothingToUndo -> {
                    snackbarHostState.showSnackbar("Nothing to undo")
                }
                else -> {}
            }
        }
    }

    activeTransactionType?.let { type ->
        val person = uiState.person
        if (person != null) {
            AddTransactionDialog(
                type = type,
                people = listOf(person),
                personBalancesMap = mapOf(person.id to uiState.outstandingBalancePaise),
                initialPersonId = person.id,
                showPersonSelector = false,
                onDismiss = { activeTransactionType = null },
                onConfirm = { _, amountPaise, note, timestamp ->
                    activeTransactionType = null
                    if (type == LedgerEntryType.GIVEN) {
                        onGiveMoneyConfirm(amountPaise, note, timestamp)
                    } else {
                        onRecordReturnConfirm(amountPaise, note, timestamp)
                    }
                }
            )
        }
    }

    val personName = uiState.person?.name ?: "Person Details"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = personName,
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal lending ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (uiState.transactions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { onUndoClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Undo",
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Undo",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Concise Personal Balance Summary Card
            val balanceCardShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(balanceCardShape)
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle, balanceCardShape)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "OUTSTANDING BALANCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = CurrencyFormatter.formatPaiseToRupees(uiState.outstandingBalancePaise),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Given: ${CurrencyFormatter.formatPaiseToRupees(uiState.totalGivenPaise)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Returned: ${CurrencyFormatter.formatPaiseToRupees(uiState.totalGivenBackPaise)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Actions contextual to this person
            PattubookQuickActions(
                onGiveMoneyClick = { activeTransactionType = LedgerEntryType.GIVEN },
                onRecordReturnClick = { activeTransactionType = LedgerEntryType.GIVEN_BACK },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction History Title - Focal Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.transactions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreenContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${uiState.transactions.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction History List
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

                uiState.transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Use Give Money or Record Return above to record transactions.",
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
                            items = uiState.transactions,
                            key = { entry -> entry.id }
                        ) { entry ->
                            TransactionRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PersonDetailScreenPreview() {
    PattubookTheme {
        val samplePerson = Person(id = 1, name = "Jai")
        val sampleTransactions = listOf(
            LedgerEntry(
                id = 1,
                personId = 1,
                amountPaise = 200000L,
                type = LedgerEntryType.GIVEN,
                note = "Trip expenses"
            ),
            LedgerEntry(
                id = 2,
                personId = 1,
                amountPaise = 50000L,
                type = LedgerEntryType.GIVEN_BACK,
                note = "GPay repayment"
            )
        )
        PersonDetailScreen(
            uiState = PersonDetailUiState(
                person = samplePerson,
                transactions = sampleTransactions,
                totalGivenPaise = 200000L,
                totalGivenBackPaise = 50000L,
                outstandingBalancePaise = 150000L,
                isLoading = false
            ),
            onBackClick = {},
            onGiveMoneyConfirm = { _, _, _ -> },
            onRecordReturnConfirm = { _, _, _ -> },
            onUndoClick = {}
        )
    }
}
