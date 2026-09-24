package com.pattubook.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.repository.PattubookRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonDetailViewModel(
    val personId: Long,
    private val repository: PattubookRepository,
) : ViewModel() {

    private val _eventChannel = Channel<PersonDetailUiEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PersonDetailUiEvent> = _eventChannel.receiveAsFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastUndoneEntry: LedgerEntry? = null

    val uiState: StateFlow<PersonDetailUiState> = combine(
        repository.observePersonById(personId),
        repository.observeEntriesForPerson(personId),
        repository.observeTotalGiven(personId),
        repository.observeTotalGivenBack(personId),
        repository.observeOutstandingBalance(personId),
    ) { person, transactions, totalGiven, totalGivenBack, outstandingBalance ->
        PersonDetailUiState(
            person = person,
            transactions = transactions,
            totalGivenPaise = totalGiven,
            totalGivenBackPaise = totalGivenBack,
            outstandingBalancePaise = outstandingBalance,
            isLoading = false,
            errorMessage = if (person == null) "Person not found." else null,
        )
    }.catch { throwable ->
        emit(
            PersonDetailUiState(
                person = null,
                transactions = emptyList(),
                totalGivenPaise = 0L,
                totalGivenBackPaise = 0L,
                outstandingBalancePaise = 0L,
                isLoading = false,
                errorMessage = throwable.localizedMessage ?: "Failed to load person details.",
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonDetailUiState(isLoading = true),
    )

    fun addMoneyGiven(
        amountPaise: Long,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null,
    ) {
        lastUndoneEntry = null
        viewModelScope.launch {
            val result = repository.addMoneyGiven(
                personId = personId,
                amountPaise = amountPaise,
                timestamp = timestamp,
                note = note,
            )
            result.onSuccess { entryId ->
                _eventChannel.send(PersonDetailUiEvent.MoneyAdded(entryId))
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to add money given."
                _errorMessage.value = userMsg
                _eventChannel.send(PersonDetailUiEvent.Error(userMsg))
            }
        }
    }

    fun addMoneyGivenBack(
        amountPaise: Long,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null,
    ) {
        lastUndoneEntry = null
        viewModelScope.launch {
            val result = repository.addMoneyGivenBack(
                personId = personId,
                amountPaise = amountPaise,
                timestamp = timestamp,
                note = note,
            )
            result.onSuccess { entryId ->
                _eventChannel.send(PersonDetailUiEvent.MoneyAdded(entryId))
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to record money given back."
                _errorMessage.value = userMsg
                _eventChannel.send(PersonDetailUiEvent.Error(userMsg))
            }
        }
    }

    fun undoLastLedgerEntry() {
        viewModelScope.launch {
            val result = repository.undoLastLedgerEntry(personId)
            result.onSuccess { undoneEntry ->
                lastUndoneEntry = undoneEntry
                _eventChannel.send(PersonDetailUiEvent.UndoSuccess(undoneEntry))
            }.onFailure { throwable ->
                if (throwable is NoSuchElementException) {
                    _eventChannel.send(PersonDetailUiEvent.UndoNothingToUndo)
                } else {
                    val userMsg = throwable.message ?: "Failed to undo transaction."
                    _errorMessage.value = userMsg
                    _eventChannel.send(PersonDetailUiEvent.Error(userMsg))
                }
            }
        }
    }

    fun redoLastUndoneEntry() {
        val entryToRedo = lastUndoneEntry ?: return
        viewModelScope.launch {
            val result = repository.restoreUndoneEntry(entryToRedo)
            result.onSuccess {
                lastUndoneEntry = null
                _eventChannel.send(PersonDetailUiEvent.RedoSuccess)
            }.onFailure { throwable ->
                lastUndoneEntry = null
                val userMsg = throwable.message ?: "Failed to redo transaction."
                _errorMessage.value = userMsg
                _eventChannel.send(PersonDetailUiEvent.Error(userMsg))
            }
        }
    }

    fun updateEntry(entry: LedgerEntry) {
        lastUndoneEntry = null
        viewModelScope.launch {
            val result = repository.updateEntry(entry)
            result.onSuccess {
                _eventChannel.send(PersonDetailUiEvent.EntryUpdated)
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to update entry."
                _errorMessage.value = userMsg
                _eventChannel.send(PersonDetailUiEvent.Error(userMsg))
            }
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        lastUndoneEntry = entry
        viewModelScope.launch {
            val result = repository.deleteEntry(entry)
            result.onSuccess {
                _eventChannel.send(PersonDetailUiEvent.EntryDeleted(entry))
            }.onFailure { throwable ->
                lastUndoneEntry = null
                val userMsg = throwable.message ?: "Failed to delete entry."
                _errorMessage.value = userMsg
                _eventChannel.send(PersonDetailUiEvent.Error(userMsg))
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val personId: Long,
        private val repository: PattubookRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PersonDetailViewModel::class.java)) {
                return PersonDetailViewModel(personId, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
