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
import androidx.compose.runtime.Composable
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
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkBackground
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextMuted
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter
import com.pattubook.app.ui.viewmodel.PeopleUiState

@Composable
fun RecycleBinScreen(
    uiState: PeopleUiState,
    onBackClick: () -> Unit,
    onRestorePerson: (Person) -> Unit,
    onPermanentlyDeletePerson: (Person) -> Unit,
    onEmptyRecycleBin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var personToRestore by remember { mutableStateOf<Person?>(null) }
    var personToPermanentlyDelete by remember { mutableStateOf<Person?>(null) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }

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

    if (showEmptyBinDialog) {
        EmptyRecycleBinDialog(
            onDismiss = { showEmptyBinDialog = false },
            onConfirmEmpty = {
                showEmptyBinDialog = false
                onEmptyRecycleBin()
            }
        )
    }

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

                if (uiState.deletedPeople.isNotEmpty()) {
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

                uiState.deletedPeople.isEmpty() -> {
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
                                text = "Deleted people and their transaction history will appear here until permanently deleted.",
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
                            items = uiState.deletedPeople,
                            key = { person -> person.id }
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                androidx.compose.material3.TextButton(onClick = onDismiss) {
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                androidx.compose.material3.TextButton(onClick = onDismiss) {
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                text = "This will permanently delete all people in the Recycle Bin and their entire transaction history. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
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
        RecycleBinScreen(
            uiState = PeopleUiState(
                deletedPeople = samplePeople,
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
