package com.pattubook.app.ui.viewmodel

import com.pattubook.app.data.local.entity.Person

data class PeopleUiState(
    val people: List<Person> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PeopleUiEvent {
    data class PersonAdded(val personId: Long) : PeopleUiEvent
    data object PersonUpdated : PeopleUiEvent
    data object PersonDeleted : PeopleUiEvent
    data class Error(val message: String) : PeopleUiEvent
}
