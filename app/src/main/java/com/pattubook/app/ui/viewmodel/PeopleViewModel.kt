package com.pattubook.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pattubook.app.data.local.entity.Person
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

class PeopleViewModel(
    private val repository: PattubookRepository,
) : ViewModel() {

    private val _eventChannel = Channel<PeopleUiEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PeopleUiEvent> = _eventChannel.receiveAsFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val uiState: StateFlow<PeopleUiState> = combine(
        repository.observeAllPeople(),
        repository.observeDeletedPeople(),
        repository.observeAllPersonBalances(),
        repository.observeGlobalTotalGiven(),
        repository.observeGlobalTotalGivenBack(),
        repository.observeGlobalTotalOutstanding(),
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val people = flows[0] as List<Person>
        @Suppress("UNCHECKED_CAST")
        val deletedPeople = flows[1] as List<Person>
        @Suppress("UNCHECKED_CAST")
        val balancesMap = flows[2] as Map<Long, Long>
        val totalGiven = flows[3] as Long
        val totalGivenBack = flows[4] as Long
        val totalOutstanding = flows[5] as Long

        PeopleUiState(
            people = people,
            deletedPeople = deletedPeople,
            outstandingBalancePaiseByPerson = balancesMap,
            totalGivenPaise = totalGiven,
            totalGivenBackPaise = totalGivenBack,
            totalOutstandingPaise = totalOutstanding,
            isLoading = false,
            errorMessage = null,
        )
    }.catch { throwable ->
        emit(
            PeopleUiState(
                people = emptyList(),
                deletedPeople = emptyList(),
                outstandingBalancePaiseByPerson = emptyMap(),
                totalGivenPaise = 0L,
                totalGivenBackPaise = 0L,
                totalOutstandingPaise = 0L,
                isLoading = false,
                errorMessage = throwable.localizedMessage ?: "Failed to load overview.",
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PeopleUiState(isLoading = true),
    )

    fun addPerson(name: String) {
        viewModelScope.launch {
            val result = repository.addPerson(name)
            result.onSuccess { personId ->
                _eventChannel.send(PeopleUiEvent.PersonAdded(personId))
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to add person."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun addMoneyGiven(
        personId: Long,
        amountPaise: Long,
        note: String? = null,
    ) {
        viewModelScope.launch {
            val result = repository.addMoneyGiven(
                personId = personId,
                amountPaise = amountPaise,
                note = note,
            )
            result.onSuccess { entryId ->
                _eventChannel.send(PeopleUiEvent.MoneyAdded(entryId))
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to add money given."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun addMoneyGivenBack(
        personId: Long,
        amountPaise: Long,
        note: String? = null,
    ) {
        viewModelScope.launch {
            val result = repository.addMoneyGivenBack(
                personId = personId,
                amountPaise = amountPaise,
                note = note,
            )
            result.onSuccess { entryId ->
                _eventChannel.send(PeopleUiEvent.MoneyAdded(entryId))
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to record money given back."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            val result = repository.updatePerson(person)
            result.onSuccess {
                _eventChannel.send(PeopleUiEvent.PersonUpdated)
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to update person."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun setPersonHidden(personId: Long, isHidden: Boolean) {
        viewModelScope.launch {
            val result = repository.setPersonHidden(personId, isHidden)
            result.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to update person visibility."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun movePersonToRecycleBin(person: Person) {
        viewModelScope.launch {
            val result = repository.movePersonToRecycleBin(person.id)
            result.onSuccess {
                _eventChannel.send(PeopleUiEvent.PersonDeleted)
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to move person to Recycle Bin."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun restorePerson(person: Person) {
        viewModelScope.launch {
            val result = repository.restorePerson(person.id)
            result.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to restore person."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun permanentlyDeletePerson(person: Person) {
        viewModelScope.launch {
            val result = repository.permanentlyDeletePerson(person)
            result.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to permanently delete person."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val result = repository.emptyRecycleBin()
            result.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to empty Recycle Bin."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun deletePerson(person: Person) {
        movePersonToRecycleBin(person)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val result = repository.deleteAllData()
            result.onSuccess {
                _eventChannel.send(PeopleUiEvent.AllDataDeleted)
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to delete all data."
                _errorMessage.value = userMsg
                _eventChannel.send(PeopleUiEvent.Error(userMsg))
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: PattubookRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PeopleViewModel::class.java)) {
                return PeopleViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
