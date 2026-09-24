package com.pattubook.app.ui.recyclebin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentBlueContainer
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkBackground
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextMuted
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter
import com.pattubook.app.ui.util.DateFormatter
import com.pattubook.app.ui.viewmodel.PeopleUiState

@Composable
fun RecycleBinScreen(
    uiState: PeopleUiState,
    onBackClick: () -> Unit,
    onRestorePerson: (Person) -> Unit,
    onPermanentlyDeletePerson: (Person) -> Unit,
    modifier: Modifier = Modifier,
    onRestoreEntry: (LedgerEntry) -> Unit = {},
    onPermanentlyDeleteEntry: (LedgerEntry) -> Unit = {},
    onEmptyRecycleBin: () -> Unit = {},
) {
    var personToRestore by remember { mutableStateOf<Person?>(null) }
    var personToPermanentlyDelete by remember { mutableStateOf<Person?>(null) }

    var entryToRestore by remember { mutableStateOf<LedgerEntry?>(null) }
    var entryToPermanentlyDelete by remember { mutableStateOf<LedgerEntry?>(null) }

    var showEmptyBinDialog by remember { mutableStateOf(false) }

    val personNameMap = remember(uiState.people, uiState.deletedPeople) {
        (uiState.people + uiState.deletedPeople).associate { it.id to it.name }
    }

    personToRestore?.let { person ->
        RestorePersonDialog(
            person = person,
            onDismiss = { personToRestore = null },
            onConfirmRestore = {
                personToRestore = null
                onRestorePerson(person)
            }
        )
    }

    personToPermanentlyDelete?.let { person ->
        PermanentDeleteDialog(
            person = person,
            onDismiss = { personToPermanentlyDelete = null },
            onConfirmDelete = {
                personToPermanentlyDelete = null
                onPermanentlyDeletePerson(person)
            }
        )
    }

    entryToRestore?.let { entry ->
        RestoreTransactionDialog(
            entry = entry,
            personName = personNameMap[entry.personId] ?: "Person #${entry.personId}",
            onDismiss = { entryToRestore = null },
            onConfirmRestore = {
                entryToRestore = null
                onRestoreEntry(entry)
            }
        )
    }

    entryToPermanentlyDelete?.let { entry ->
        PermanentDeleteTransactionDialog(
            entry = entry,
            personName = personNameMap[entry.personId] ?: "Person #${entry.personId}",
            onDismiss = { entryToPermanentlyDelete = null },
            onConfirmDelete = {
                entryToPermanentlyDelete = null
                onPermanentlyDeleteEntry(entry)
            }
        )
    }

    if (showEmptyBinDialog) {
        EmptyRecycleBinDialog(
            onDismiss = { showEmptyBinDialog = false },
            onConfirmEmpty = {
                showEmptyBinDialog = false
                onEmptyRecycleBin()
            }
        )
    }

    val hasDeletedContent = uiState.deletedPeople.isNotEmpty() || uiState.deletedEntries.isNotEmpty()

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

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Recycle Bin",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (hasDeletedContent) {
                    Button(
                        onClick = { showEmptyBinDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentRed,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Empty Bin",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Empty Bin",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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

                !hasDeletedContent -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Recycle Bin is empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Deleted people and transactions will appear here until permanently deleted.",
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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        // People Section
                        if (uiState.deletedPeople.isNotEmpty()) {
                            item {
                                Text(
                                    text = "PEOPLE (${uiState.deletedPeople.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            items(
                                items = uiState.deletedPeople,
                                key = { person -> "person_${person.id}" }
                            ) { person ->
                                val balance = uiState.outstandingBalancePaiseByPerson[person.id] ?: 0L
                                val cardShape = RoundedCornerShape(18.dp)
                                val initialLetter = person.name.trim().take(1).uppercase()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .background(DarkSurface)
                                        .border(1.dp, BorderSubtle, cardShape)
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
                                                        text = "In Recycle Bin",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            val (balanceColor, balanceText) = when {
                                                balance > 0 -> AccentGreen to CurrencyFormatter.formatPaiseToRupees(balance)
                                                balance < 0 -> AccentBlue to CurrencyFormatter.formatPaiseToRupees(balance)
                                                else -> TextMuted to "Settled"
                                            }

                                            Text(
                                                text = balanceText,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = balanceColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { personToRestore = person },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Restore",
                                                    tint = AccentGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Restore",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AccentGreen
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = { personToPermanentlyDelete = person },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentRed.copy(alpha = 0.15f),
                                                    contentColor = AccentRed
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Permanently",
                                                    tint = AccentRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Delete Permanently",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AccentRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Transactions Section
                        if (uiState.deletedEntries.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "TRANSACTIONS (${uiState.deletedEntries.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            items(
                                items = uiState.deletedEntries,
                                key = { entry -> "entry_${entry.id}" }
                            ) { entry ->
                                val cardShape = RoundedCornerShape(18.dp)
                                val isGiven = entry.type == LedgerEntryType.GIVEN
                                val accentColor = if (isGiven) AccentGreen else AccentBlue
                                val containerColor = if (isGiven) AccentGreenContainer else AccentBlueContainer
                                val typeIcon = if (isGiven) Icons.Default.Add else Icons.Default.Refresh
                                val typeTitle = if (isGiven) "Money Given" else "Money Returned"
                                val personName = personNameMap[entry.personId] ?: "Person #${entry.personId}"

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .alpha(0.85f)
                                        .background(DarkSurface)
                                        .border(1.dp, BorderSubtle, cardShape)
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
                                                        .background(containerColor)
                                                        .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = typeIcon,
                                                        contentDescription = typeTitle,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                    Text(
                                                        text = "$typeTitle • $personName",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = DateFormatter.formatEpochMillis(entry.timestamp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            Text(
                                                text = CurrencyFormatter.formatPaiseToRupees(entry.amountPaise),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(DarkSurfaceVariant)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = note,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { entryToRestore = entry },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Restore",
                                                    tint = AccentGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Restore",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AccentGreen
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = { entryToPermanentlyDelete = entry },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentRed.copy(alpha = 0.15f),
                                                    contentColor = AccentRed
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Permanently",
                                                    tint = AccentRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Delete Permanently",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AccentRed
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
        }
    }
}

@Composable
fun RestorePersonDialog(
    person: Person,
    onDismiss: () -> Unit,
    onConfirmRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Restore ${person.name}?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This will restore ${person.name} and their transaction history to your active ledger.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restore", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun RestoreTransactionDialog(
    entry: LedgerEntry,
    personName: String,
    onDismiss: () -> Unit,
    onConfirmRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Restore Transaction?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This will restore this ${CurrencyFormatter.formatPaiseToRupees(entry.amountPaise)} transaction for $personName to your active ledger.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restore", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun PermanentDeleteDialog(
    person: Person,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Delete ${person.name} permanently?",
                style = MaterialTheme.typography.titleLarge,
                color = AccentRed,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This will permanently delete ${person.name} and all associated transaction history. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Permanently", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun PermanentDeleteTransactionDialog(
    entry: LedgerEntry,
    personName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Delete Transaction Permanently?",
                style = MaterialTheme.typography.titleLarge,
                color = AccentRed,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This will permanently delete this ${CurrencyFormatter.formatPaiseToRupees(entry.amountPaise)} transaction for $personName from Pattubook. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Permanently", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun EmptyRecycleBinDialog(
    onDismiss: () -> Unit,
    onConfirmEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Empty Recycle Bin?",
                style = MaterialTheme.typography.titleLarge,
                color = AccentRed,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This will permanently delete all people and transactions in the Recycle Bin. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmEmpty,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Empty Recycle Bin", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview
@Composable
fun RecycleBinScreenPreview() {
    PattubookTheme {
        val samplePeople = listOf(
            Person(id = 1, name = "Alex", isDeleted = true)
        )
        val sampleEntries = listOf(
            LedgerEntry(id = 1, personId = 1, amountPaise = 100000L, type = LedgerEntryType.GIVEN, isDeleted = true)
        )
        RecycleBinScreen(
            uiState = PeopleUiState(
                deletedPeople = samplePeople,
                deletedEntries = sampleEntries,
                outstandingBalancePaiseByPerson = mapOf(1L to 30000L),
                isLoading = false
            ),
            onBackClick = {},
            onRestorePerson = {},
            onPermanentlyDeletePerson = {},
            onEmptyRecycleBin = {}
        )
    }
}
