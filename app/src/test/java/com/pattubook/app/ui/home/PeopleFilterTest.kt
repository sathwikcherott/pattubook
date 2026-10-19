package com.pattubook.app.ui.home

import com.pattubook.app.data.local.entity.Person
import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleFilterTest {
    private val activeAlice = Person(id = 1L, name = "Alice")
    private val hiddenAlice = Person(id = 2L, name = "Alice Archive", isHidden = true)
    private val activeBob = Person(id = 3L, name = "Bob")
    private val people = listOf(activeAlice, hiddenAlice, activeBob)
    private val balances = mapOf(1L to 500L, 2L to 900L, 3L to -200L)

    @Test
    fun activeOverviewAndSearchExcludeHiddenPeople() {
        assertEquals(
            listOf(activeAlice, activeBob),
            filterPeopleForHome(people, balances, "", PeopleFilterOption.ALL),
        )
        assertEquals(
            listOf(activeAlice),
            filterPeopleForHome(people, balances, "alice", PeopleFilterOption.ALL),
        )
    }

    @Test
    fun hiddenFilterShowsAndSearchesOnlyHiddenPeople() {
        assertEquals(
            listOf(hiddenAlice),
            filterPeopleForHome(people, balances, "", PeopleFilterOption.HIDDEN),
        )
        assertEquals(
            listOf(hiddenAlice),
            filterPeopleForHome(people, balances, "archive", PeopleFilterOption.HIDDEN),
        )
    }

    @Test
    fun balanceFiltersApplyOnlyToActivePeople() {
        assertEquals(
            listOf(activeAlice),
            filterPeopleForHome(people, balances, "", PeopleFilterOption.THEY_OWE_YOU),
        )
        assertEquals(
            listOf(activeBob),
            filterPeopleForHome(people, balances, "", PeopleFilterOption.YOU_OWE_THEM),
        )
    }
}
