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
import kotlinx.coroutines.flow.map
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

    val uiState: StateFlow<PeopleUiState> = repository.observeAllPeople()
        .map { people ->
            PeopleUiState(
                people = people,
                isLoading = false,
                errorMessage = null,
            )
        }
        .catch { throwable ->
            emit(
                PeopleUiState(
                    people = emptyList(),
                    isLoading = false,
                    errorMessage = throwable.localizedMessage ?: "Failed to load people list.",
                ),
            )
        }
        .stateIn(
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

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            val result = repository.deletePerson(person)
            result.onSuccess {
                _eventChannel.send(PeopleUiEvent.PersonDeleted)
            }.onFailure { throwable ->
                val userMsg = throwable.message ?: "Failed to delete person."
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
