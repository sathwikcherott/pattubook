package com.pattubook.app.ui.viewmodel

import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.Person

data class PersonDetailUiState(
    val person: Person? = null,
    val transactions: List<LedgerEntry> = emptyList(),
    val totalGivenPaise: Long = 0L,
    val totalGivenBackPaise: Long = 0L,
    val outstandingBalancePaise: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PersonDetailUiEvent {
    data class MoneyAdded(val entryId: Long) : PersonDetailUiEvent
    data class UndoSuccess(val undoneEntry: LedgerEntry) : PersonDetailUiEvent
    data object UndoNothingToUndo : PersonDetailUiEvent
    data object RedoSuccess : PersonDetailUiEvent
    data object EntryUpdated : PersonDetailUiEvent
    data object EntryDeleted : PersonDetailUiEvent
    data class Error(val message: String) : PersonDetailUiEvent
}
