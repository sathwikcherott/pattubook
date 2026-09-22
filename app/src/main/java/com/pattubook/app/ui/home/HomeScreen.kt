package com.pattubook.app.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.components.AddPersonDialog
import com.pattubook.app.ui.components.AddTransactionDialog
import com.pattubook.app.ui.components.EmptyState
import com.pattubook.app.ui.components.NavDestination
import com.pattubook.app.ui.components.PattubookBalanceCard
import com.pattubook.app.ui.components.PattubookBottomBar
import com.pattubook.app.ui.components.PattubookQuickActions
import com.pattubook.app.ui.components.PersonBalanceRow
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.DarkBackground
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.viewmodel.PeopleUiState

@Composable
fun HomeScreen(
    uiState: PeopleUiState,
    onPersonClick: (Long) -> Unit,
    onAddPersonConfirm: (String) -> Unit,
    onGiveMoneyConfirm: (personId: Long, amountPaise: Long, note: String?) -> Unit,
    onRecordReturnConfirm: (personId: Long, amountPaise: Long, note: String?) -> Unit,
    modifier: Modifier = Modifier,
    onBottomNavSelected: (NavDestination) -> Unit = {},
) {
    var selectedNavDestination by remember { mutableStateOf(NavDestination.HOME) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var activeTransactionType by remember { mutableStateOf<LedgerEntryType?>(null) }

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { name ->
                showAddPersonDialog = false
                onAddPersonConfirm(name)
            }
        )
    }

    activeTransactionType?.let { type ->
        AddTransactionDialog(
            type = type,
            people = uiState.people,
            personBalancesMap = uiState.outstandingBalancePaiseByPerson,
            onDismiss = { activeTransactionType = null },
            onConfirm = { personId, amountPaise, note ->
                activeTransactionType = null
                if (type == LedgerEntryType.GIVEN) {
                    onGiveMoneyConfirm(personId, amountPaise, note)
                } else {
                    onRecordReturnConfirm(personId, amountPaise, note)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            PattubookBottomBar(
                currentDestination = selectedNavDestination,
                onDestinationSelected = { destination ->
                    when (destination) {
                        NavDestination.HOME -> {
                            selectedNavDestination = NavDestination.HOME
                        }
                        NavDestination.MORE -> {
                            onBottomNavSelected(NavDestination.MORE)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Top Header
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pattubook",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your lending overview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clickable { showAddPersonDialog = true }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Person",
                        tint = AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Balance Card
            PattubookBalanceCard(
                outstandingBalancePaise = uiState.totalOutstandingPaise,
                peopleCount = uiState.people.size,
                totalGivenPaise = uiState.totalGivenPaise,
                totalReturnedPaise = uiState.totalGivenBackPaise,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions
            PattubookQuickActions(
                onGiveMoneyClick = { activeTransactionType = LedgerEntryType.GIVEN },
                onRecordReturnClick = { activeTransactionType = LedgerEntryType.GIVEN_BACK },
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "People",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                if (uiState.people.isNotEmpty()) {
                    Text(
                        text = "${uiState.people.size} contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // People List or Loading/Empty States
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

                uiState.people.isEmpty() -> {
                    EmptyState(
                        onAddPersonClick = { showAddPersonDialog = true },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = uiState.people,
                            key = { person -> person.id }
                        ) { person ->
                            val personBalance = uiState.outstandingBalancePaiseByPerson[person.id] ?: 0L
                            PersonBalanceRow(
                                person = person,
                                outstandingBalancePaise = personBalance,
                                onClick = { onPersonClick(person.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    PattubookTheme {
        val samplePeople = listOf(
            Person(id = 1, name = "Jai"),
            Person(id = 2, name = "Ananya"),
            Person(id = 3, name = "Karthik"),
        )
        val sampleBalances = mapOf(
            1L to 245000L,
            2L to 0L,
            3L to -50000L,
        )
        HomeScreen(
            uiState = PeopleUiState(
                people = samplePeople,
                outstandingBalancePaiseByPerson = sampleBalances,
                totalGivenPaise = 295000L,
                totalGivenBackPaise = 100000L,
                totalOutstandingPaise = 195000L,
                isLoading = false,
            ),
            onPersonClick = {},
            onAddPersonConfirm = {},
            onGiveMoneyConfirm = { _, _, _ -> },
            onRecordReturnConfirm = { _, _, _ -> }
        )
    }
}

@Preview
@Composable
fun HomeScreenEmptyPreview() {
    PattubookTheme {
        HomeScreen(
            uiState = PeopleUiState(people = emptyList()),
            onPersonClick = {},
            onAddPersonConfirm = {},
            onGiveMoneyConfirm = { _, _, _ -> },
            onRecordReturnConfirm = { _, _, _ -> }
        )
    }
}
