package com.pattubook.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.components.AddPersonDialog
import com.pattubook.app.ui.components.AddTransactionDialog
import com.pattubook.app.ui.components.DeletePersonDialog
import com.pattubook.app.ui.components.EmptyFilterState
import com.pattubook.app.ui.components.EmptySearchState
import com.pattubook.app.ui.components.EmptyState
import com.pattubook.app.ui.components.HidePersonDialog
import com.pattubook.app.ui.components.NavDestination
import com.pattubook.app.ui.components.PattubookBalanceCard
import com.pattubook.app.ui.components.PattubookBottomBar
import com.pattubook.app.ui.components.PattubookQuickActions
import com.pattubook.app.ui.components.PersonBalanceRow
import com.pattubook.app.ui.components.UnhidePersonDialog
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkBackground
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextMuted
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter
import com.pattubook.app.ui.viewmodel.PeopleUiState
import kotlin.math.abs
import kotlin.math.absoluteValue

enum class PeopleFilterOption(val label: String) {
    ALL("All active people"),
    HIDDEN("Hidden people"),
    THEY_OWE_YOU("They owe you"),
    YOU_OWE_THEM("You owe them"),
    SETTLED("Settled"),
}

internal fun filterPeopleForHome(
    people: List<Person>,
    outstandingBalancePaiseByPerson: Map<Long, Long>,
    searchQuery: String,
    selectedFilter: PeopleFilterOption,
): List<Person> {
    val peopleInView = when (selectedFilter) {
        PeopleFilterOption.HIDDEN -> people.filter { it.isHidden }
        else -> people.filter { !it.isHidden }
    }
    val query = searchQuery.trim()

    return peopleInView.filter { person ->
        val balance = outstandingBalancePaiseByPerson[person.id] ?: 0L
        val matchesFilter = when (selectedFilter) {
            PeopleFilterOption.ALL, PeopleFilterOption.HIDDEN -> true
            PeopleFilterOption.THEY_OWE_YOU -> balance > 0L
            PeopleFilterOption.YOU_OWE_THEM -> balance < 0L
            PeopleFilterOption.SETTLED -> balance == 0L
        }
        val matchesSearch = query.isEmpty() || person.name.contains(query, ignoreCase = true)
        matchesFilter && matchesSearch
    }
}

@Composable
fun HomeScreen(
    uiState: PeopleUiState,
    onPersonClick: (Long) -> Unit,
    onAddPersonConfirm: (String) -> Unit,
    onGiveMoneyConfirm: (personId: Long, amountPaise: Long, note: String?, timestamp: Long) -> Unit,
    onRecordReturnConfirm: (personId: Long, amountPaise: Long, note: String?, timestamp: Long) -> Unit,
    modifier: Modifier = Modifier,
    onBottomNavSelected: (NavDestination) -> Unit = {},
    onBalanceCardClick: () -> Unit = {},
    onHidePersonConfirm: (Person) -> Unit = {},
    onUnhidePersonConfirm: (Person) -> Unit = {},
    onDeletePersonConfirm: (Person) -> Unit = {},
) {
    var selectedNavDestination by remember { mutableStateOf(NavDestination.HOME) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var activeTransactionType by remember { mutableStateOf<LedgerEntryType?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PeopleFilterOption.ALL) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    var personToHide by remember { mutableStateOf<Person?>(null) }
    var personToUnhide by remember { mutableStateOf<Person?>(null) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }

    val activePeople = remember(uiState.people) {
        uiState.people.filter { !it.isHidden }
    }

    val filteredPeople = remember(
        uiState.people,
        searchQuery,
        selectedFilter,
        uiState.outstandingBalancePaiseByPerson
    ) {
        filterPeopleForHome(
            people = uiState.people,
            outstandingBalancePaiseByPerson = uiState.outstandingBalancePaiseByPerson,
            searchQuery = searchQuery,
            selectedFilter = selectedFilter,
        )
    }

    // Exact financial summary calculations
    val (grossToCollectPaise, grossToPayPaise, unsettledPeopleCount) = remember(
        activePeople,
        uiState.outstandingBalancePaiseByPerson
    ) {
        var collect = 0L
        var pay = 0L
        var unsettled = 0
        activePeople.forEach { person ->
            val balance = uiState.outstandingBalancePaiseByPerson[person.id] ?: 0L
            if (balance > 0L) {
                collect += balance
                unsettled++
            } else if (balance < 0L) {
                pay += (-balance)
                unsettled++
            }
        }
        Triple(collect, pay, unsettled)
    }

    val netBalancePaise = remember(grossToCollectPaise, grossToPayPaise) {
        grossToCollectPaise - grossToPayPaise
    }

    personToHide?.let { person ->
        HidePersonDialog(
            person = person,
            onDismiss = { personToHide = null },
            onConfirmHide = {
                personToHide = null
                onHidePersonConfirm(person)
            }
        )
    }

    personToUnhide?.let { person ->
        UnhidePersonDialog(
            person = person,
            onDismiss = { personToUnhide = null },
            onConfirmUnhide = {
                personToUnhide = null
                onUnhidePersonConfirm(person)
            }
        )
    }

    personToDelete?.let { person ->
        DeletePersonDialog(
            person = person,
            onDismiss = { personToDelete = null },
            onConfirmDelete = {
                personToDelete = null
                onDeletePersonConfirm(person)
            }
        )
    }

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
            onConfirm = { personId, amountPaise, note, timestamp ->
                activeTransactionType = null
                if (type == LedgerEntryType.GIVEN) {
                    onGiveMoneyConfirm(personId, amountPaise, note, timestamp)
                } else {
                    onRecordReturnConfirm(personId, amountPaise, note, timestamp)
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
                        NavDestination.ACTIVITY -> {
                            onBottomNavSelected(NavDestination.ACTIVITY)
                        }
                        NavDestination.SETTINGS -> {
                            onBottomNavSelected(NavDestination.SETTINGS)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // A. Top Header App Bar
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
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Personal Lending Ledger",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        AddPersonIconButton(
                            onClick = { showAddPersonDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // B. Financial Summary
                    PattubookBalanceCard(
                        netBalancePaise = netBalancePaise,
                        grossToCollectPaise = grossToCollectPaise,
                        grossToPayPaise = grossToPayPaise,
                        unsettledPeopleCount = unsettledPeopleCount,
                        totalPeopleCount = activePeople.size,
                        onClick = onBalanceCardClick,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // C. Quick Actions
                    PattubookQuickActions(
                        onGiveMoneyClick = { activeTransactionType = LedgerEntryType.GIVEN },
                        onRecordReturnClick = { activeTransactionType = LedgerEntryType.GIVEN_BACK },
                    )

                    if (activePeople.isNotEmpty()) {
                        val topPerson = remember(activePeople, uiState.outstandingBalancePaiseByPerson) {
                            activePeople
                                .filter { (uiState.outstandingBalancePaiseByPerson[it.id] ?: 0L) != 0L }
                                .maxByOrNull { abs(uiState.outstandingBalancePaiseByPerson[it.id] ?: 0L) }
                        }
                        val topBalancePaise = topPerson?.let { uiState.outstandingBalancePaiseByPerson[it.id] ?: 0L } ?: 0L

                        Spacer(modifier = Modifier.height(14.dp))
                        // D. Smart Insight
                        SmartInsightCard(
                            topPersonName = topPerson?.name,
                            topBalancePaise = topBalancePaise,
                            unsettledCount = unsettledPeopleCount,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // E. People Section Header & F. Expandable Search & Compact Filter Menu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "People",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.people.isNotEmpty()) {
                                Text(
                                    text = "${activePeople.size} active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )

                                // Filter Menu Button
                                Box {
                                    IconButton(onClick = { filterMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = "Filter People",
                                            tint = if (selectedFilter != PeopleFilterOption.ALL) AccentGreen else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = filterMenuExpanded,
                                        onDismissRequest = { filterMenuExpanded = false },
                                        modifier = Modifier.background(DarkSurfaceVariant)
                                    ) {
                                        PeopleFilterOption.entries.forEach { option ->
                                            val isSelected = option == selectedFilter
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = option.label,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isSelected) AccentGreen else TextPrimary,
                                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                        )
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = AccentGreen,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedFilter = option
                                                    filterMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Search Toggle Button
                                IconButton(
                                    onClick = {
                                        isSearchExpanded = !isSearchExpanded
                                        if (!isSearchExpanded) searchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                                        contentDescription = if (isSearchExpanded) "Close Search" else "Search People",
                                        tint = if (isSearchExpanded || searchQuery.isNotBlank()) AccentGreen else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Compact Active Filter Indicator Pill (if a filter is active)
                    if (selectedFilter != PeopleFilterOption.ALL) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(AccentGreenContainer)
                                    .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                    .clickable { selectedFilter = PeopleFilterOption.ALL }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Filter: ${selectedFilter.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Reset Filter",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSearchExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)),
                                placeholder = { Text("Search by name...", color = TextMuted) },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = AccentGreen,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = AccentGreen.copy(alpha = 0.5f),
                                    unfocusedBorderColor = BorderSubtle,
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                }
            }

            // G. People List or Empty States
            when {
                uiState.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }

                uiState.people.isEmpty() -> item {
                    EmptyState(
                        title = "Start your first ledger",
                        description = "Track money you lend and receive, privately on this device.",
                        actionLabel = "Add Person",
                        onAddPersonClick = { showAddPersonDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                filteredPeople.isEmpty() && searchQuery.isNotBlank() -> item {
                    EmptySearchState(
                        searchQuery = searchQuery,
                        onClearSearch = { searchQuery = "" },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                filteredPeople.isEmpty() && selectedFilter != PeopleFilterOption.ALL -> item {
                    EmptyFilterState(
                        filterName = selectedFilter.label,
                        onResetFilter = { selectedFilter = PeopleFilterOption.ALL },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                activePeople.isEmpty() && selectedFilter == PeopleFilterOption.ALL -> item {
                    EmptyState(
                        title = "No active people",
                        description = "Hidden people are still saved. View them to restore someone to the overview.",
                        actionLabel = "View hidden people",
                        actionIcon = Icons.AutoMirrored.Filled.List,
                        icon = Icons.AutoMirrored.Filled.List,
                        onAddPersonClick = { selectedFilter = PeopleFilterOption.HIDDEN },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                else -> itemsIndexed(
                    items = filteredPeople,
                    key = { _, person -> person.id }
                ) { _, person ->
                    val personBalance = uiState.outstandingBalancePaiseByPerson[person.id] ?: 0L
                    val latestTimestamp = uiState.activeEntries.firstOrNull { it.personId == person.id }?.timestamp

                    PersonBalanceRow(
                        person = person,
                        outstandingBalancePaise = personBalance,
                        latestActivityTimestamp = latestTimestamp,
                        onClick = { onPersonClick(person.id) },
                        onHideClick = { personToHide = person },
                        onUnhideClick = { personToUnhide = person },
                        onDeleteClick = { personToDelete = person }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPersonIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(AccentGreenContainer)
            .border(1.dp, AccentGreen.copy(alpha = 0.45f), shape)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add person",
            tint = AccentGreen,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SmartInsightCard(
    topPersonName: String?,
    topBalancePaise: Long,
    unsettledCount: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)

    val (insightText, indicatorColor) = when {
        unsettledCount == 0 || topPersonName == null || topBalancePaise == 0L ->
            "All settled up. Your ledger is clean." to AccentGreen

        topBalancePaise > 0L ->
            "Top balance: $topPersonName owes you ${CurrencyFormatter.formatPaiseToRupees(topBalancePaise)}" to AccentGreen

        else ->
            "Top balance: You owe $topPersonName ${CurrencyFormatter.formatPaiseToRupees(topBalancePaise.absoluteValue)}" to AccentBlue
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = insightText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    PattubookTheme {
        val samplePeople = listOf(
            Person(id = 1, name = "Jai"),
            Person(id = 2, name = "Ananya", isHidden = true),
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
            onGiveMoneyConfirm = { _, _, _, _ -> },
            onRecordReturnConfirm = { _, _, _, _ -> }
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
            onGiveMoneyConfirm = { _, _, _, _ -> },
            onRecordReturnConfirm = { _, _, _, _ -> }
        )
    }
}
