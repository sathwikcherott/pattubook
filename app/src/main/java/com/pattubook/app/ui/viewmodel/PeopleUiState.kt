package com.pattubook.app.ui.viewmodel

import com.pattubook.app.data.local.entity.Person

data class PeopleUiState(
    val people: List<Person> = emptyList(),
    val outstandingBalancePaiseByPerson: Map<Long, Long> = emptyMap(),
    val totalGivenPaise: Long = 0L,
    val totalGivenBackPaise: Long = 0L,
    val totalOutstandingPaise: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PeopleUiEvent {
    data class PersonAdded(val personId: Long) : PeopleUiEvent
    data object PersonUpdated : PeopleUiEvent
    data object PersonDeleted : PeopleUiEvent
    data class Error(val message: String) : PeopleUiEvent
}
