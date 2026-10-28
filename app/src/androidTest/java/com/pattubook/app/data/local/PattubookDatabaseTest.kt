package com.pattubook.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.data.repository.PattubookRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PattubookDatabaseTest {
    private lateinit var database: PattubookDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PattubookDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun archivedEntriesRemainInBalancesButHiddenPeopleStayOutOfGlobalTotals() = runBlocking {
        val visiblePersonId = database.personDao().insertPerson(Person(name = "Visible"))
        val hiddenPersonId = database.personDao().insertPerson(Person(name = "Hidden", isHidden = true))
        database.ledgerEntryDao().insertEntry(
            LedgerEntry(
                personId = visiblePersonId,
                amountPaise = 500L,
                type = LedgerEntryType.GIVEN,
                isArchived = true,
            ),
        )
        database.ledgerEntryDao().insertEntry(
            LedgerEntry(
                personId = visiblePersonId,
                amountPaise = 100L,
                type = LedgerEntryType.GIVEN_BACK,
            ),
        )
        database.ledgerEntryDao().insertEntry(
            LedgerEntry(
                personId = hiddenPersonId,
                amountPaise = 2_000L,
                type = LedgerEntryType.GIVEN,
            ),
        )

        val visibleBalances = database.ledgerEntryDao().observeAllPersonBalances().first()
            .associate { it.personId to it.outstandingBalancePaise }

        assertEquals(400L, visibleBalances[visiblePersonId])
        assertEquals(2_000L, visibleBalances[hiddenPersonId])
        assertEquals(500L, database.ledgerEntryDao().getTotalGiven(visiblePersonId))
        assertEquals(500L, database.ledgerEntryDao().observeGlobalTotalGiven().first())
    }

    @Test
    fun deletedTransactionCannotBeRestoredUntilItsPersonIsRestored() = runBlocking {
        val personId = database.personDao().insertPerson(Person(name = "Deleted person"))
        val entryId = database.ledgerEntryDao().insertEntry(
            LedgerEntry(
                personId = personId,
                amountPaise = 500L,
                type = LedgerEntryType.GIVEN,
                isDeleted = true,
            ),
        )
        database.personDao().setPersonDeleted(personId, true)
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database,
        )

        val blockedRestore = repository.restoreDeletedEntry(entryId)

        assertTrue(blockedRestore.isFailure)
        assertTrue(database.ledgerEntryDao().getEntryById(entryId)!!.isDeleted)

        database.personDao().setPersonDeleted(personId, false)
        assertTrue(repository.restoreDeletedEntry(entryId).isSuccess)
        assertFalse(database.ledgerEntryDao().getEntryById(entryId)!!.isDeleted)
    }
}
