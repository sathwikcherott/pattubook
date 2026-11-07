package com.pattubook.app.data.local

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.data.repository.PattubookRepository
import com.pattubook.app.ui.util.AmountInputUtil
import com.pattubook.app.ui.util.ProfilePhotoManager
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

    @Test
    fun calculatedExpressionIsSavedAsOneEntryAndCanBeUndone() = runBlocking {
        val personId = database.personDao().insertPerson(Person(name = "Expression"))
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database,
        )
        val amountPaise = AmountInputUtil.parseAdditionExpressionToPaise("58+56")!!

        val entryId = repository.addMoneyGiven(
            personId = personId,
            amountPaise = amountPaise,
            timestamp = 1_000L,
            note = "Combined amount",
        ).getOrThrow()

        val savedEntries = database.ledgerEntryDao().observeEntriesForPerson(personId).first()
        assertEquals(1, savedEntries.size)
        assertEquals(11_400L, savedEntries.single().amountPaise)
        assertEquals("Combined amount", savedEntries.single().note)

        val undoneEntry = repository.undoLastLedgerEntry(personId).getOrThrow()
        assertEquals(entryId, undoneEntry.id)
        assertTrue(database.ledgerEntryDao().getEntryById(entryId)!!.isDeleted)
    }

    @Test
    fun editingEntryWithExpressionTotalPreservesItsOtherLedgerFields() = runBlocking {
        val personId = database.personDao().insertPerson(Person(name = "Edit expression"))
        val entryId = database.ledgerEntryDao().insertEntry(
            LedgerEntry(
                personId = personId,
                amountPaise = 5_000L,
                type = LedgerEntryType.GIVEN_BACK,
                timestamp = 1_234L,
                note = "Original note",
            )
        )
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database,
        )
        val existingEntry = database.ledgerEntryDao().getEntryById(entryId)!!
        val calculatedPaise = AmountInputUtil.parseAdditionExpressionToPaise("10.50+20.25")!!

        assertTrue(repository.updateEntry(existingEntry.copy(amountPaise = calculatedPaise)).isSuccess)

        val updatedEntry = database.ledgerEntryDao().getEntryById(entryId)!!
        assertEquals(3_075L, updatedEntry.amountPaise)
        assertEquals(personId, updatedEntry.personId)
        assertEquals(LedgerEntryType.GIVEN_BACK, updatedEntry.type)
        assertEquals(1_234L, updatedEntry.timestamp)
        assertEquals("Original note", updatedEntry.note)
    }

    @Test
    fun personWithPhotoPathIsSavedUpdatedAndPreservedInBackup() = runBlocking {
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database,
        )

        val photoPath = "profile_photos/person_123.jpg"
        val personId = repository.addPerson("John Doe", photoPath).getOrThrow()

        val person = database.personDao().getPersonById(personId)!!
        assertEquals("John Doe", person.name)
        assertEquals(photoPath, person.photoPath)

        val newPhotoPath = "profile_photos/person_456.jpg"
        val updateResult = repository.updatePerson(person.copy(name = "John Smith", photoPath = newPhotoPath))
        assertTrue(updateResult.isSuccess)

        val updatedPerson = database.personDao().getPersonById(personId)!!
        assertEquals("John Smith", updatedPerson.name)
        assertEquals(newPhotoPath, updatedPerson.photoPath)

        val jsonResult = repository.generateBackupJson()
        assertTrue(jsonResult.isSuccess)
        val jsonString = jsonResult.getOrThrow()
        assertTrue(jsonString.contains("profile_photos/person_456.jpg"))
    }

    @Test
    fun interactiveCropSquareBitmapProducesTargetSquareResolution() = runBlocking {
        val sourceBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val cropped = ProfilePhotoManager.cropSquareBitmap(
            sourceBitmap = sourceBitmap,
            panX = 0f,
            panY = 0f,
            userScale = 1.0f,
            viewportSizePx = 300f,
            targetSizePx = 512
        )
        assertEquals(512, cropped.width)
        assertEquals(512, cropped.height)
    }
}
