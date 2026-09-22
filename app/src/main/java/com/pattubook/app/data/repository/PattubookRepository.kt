package com.pattubook.app.data.repository

import androidx.room.withTransaction
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.local.dao.LedgerEntryDao
import com.pattubook.app.data.local.dao.PersonDao
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Main repository handling data operations and business rules for Pattubook.
 * Serves as the single source of truth and boundary between application logic and Room.
 */
class PattubookRepository(
    private val personDao: PersonDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val database: PattubookDatabase? = null,
) {

    // --- Person Operations ---

    fun observeAllPeople(): Flow<List<Person>> = personDao.observeAllPeople()

    fun observePersonById(id: Long): Flow<Person?> = personDao.observePersonById(id)

    suspend fun getPersonById(id: Long): Person? = personDao.getPersonById(id)

    suspend fun addPerson(name: String): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Person name cannot be empty or whitespace."))
        }

        val person = Person(
            name = trimmedName,
            createdAt = System.currentTimeMillis(),
        )
        return try {
            val id = personDao.insertPerson(person)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePerson(person: Person): Result<Unit> {
        val trimmedName = person.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Person name cannot be empty or whitespace."))
        }

        val updatedPerson = person.copy(name = trimmedName)
        return try {
            personDao.updatePerson(updatedPerson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePerson(person: Person): Result<Unit> {
        return try {
            personDao.deletePerson(person)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Transaction Operations (Money Given / Given Back) ---

    suspend fun addMoneyGiven(
        personId: Long,
        amountPaise: Long,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null
    ): Result<Long> {
        return createLedgerEntry(
            personId = personId,
            amountPaise = amountPaise,
            type = LedgerEntryType.GIVEN,
            timestamp = timestamp,
            note = note
        )
    }

    suspend fun addMoneyGivenBack(
        personId: Long,
        amountPaise: Long,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null
    ): Result<Long> {
        return createLedgerEntry(
            personId = personId,
            amountPaise = amountPaise,
            type = LedgerEntryType.GIVEN_BACK,
            timestamp = timestamp,
            note = note
        )
    }

    private suspend fun createLedgerEntry(
        personId: Long,
        amountPaise: Long,
        type: LedgerEntryType,
        timestamp: Long,
        note: String?
    ): Result<Long> {
        if (amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0."))
        }

        val sanitizedNote = note?.trim()?.ifEmpty { null }

        val insertOperation: suspend () -> Result<Long> = {
            val personExists = personDao.getPersonById(personId) != null
            if (!personExists) {
                Result.failure(IllegalArgumentException("Person with ID $personId does not exist."))
            } else {
                val entry = LedgerEntry(
                    personId = personId,
                    amountPaise = amountPaise,
                    type = type,
                    timestamp = timestamp,
                    note = sanitizedNote
                )
                val id = ledgerEntryDao.insertEntry(entry)
                Result.success(id)
            }
        }

        return try {
            if (database != null) {
                database.withTransaction { insertOperation() }
            } else {
                insertOperation()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Balance Calculation Operations ---

    fun observeTotalGiven(personId: Long): Flow<Long> =
        ledgerEntryDao.observeTotalGiven(personId)

    fun observeTotalGivenBack(personId: Long): Flow<Long> =
        ledgerEntryDao.observeTotalGivenBack(personId)

    fun observeOutstandingBalance(personId: Long): Flow<Long> {
        return combine(
            observeTotalGiven(personId),
            observeTotalGivenBack(personId)
        ) { totalGiven, totalGivenBack ->
            totalGiven - totalGivenBack
        }
    }

    // --- Ledger History Operations ---

    fun observeEntriesForPerson(personId: Long): Flow<List<LedgerEntry>> =
        ledgerEntryDao.observeEntriesForPerson(personId)

    fun observeGivenEntriesForPerson(personId: Long): Flow<List<LedgerEntry>> =
        ledgerEntryDao.observeGivenEntriesForPerson(personId)

    fun observeGivenBackEntriesForPerson(personId: Long): Flow<List<LedgerEntry>> =
        ledgerEntryDao.observeGivenBackEntriesForPerson(personId)

    suspend fun getEntryById(id: Long): LedgerEntry? =
        ledgerEntryDao.getEntryById(id)

    suspend fun updateEntry(entry: LedgerEntry): Result<Unit> {
        if (entry.amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0."))
        }

        val sanitizedNote = entry.note?.trim()?.ifEmpty { null }
        val updatedEntry = entry.copy(note = sanitizedNote)

        val updateOperation: suspend () -> Result<Unit> = {
            val personExists = personDao.getPersonById(updatedEntry.personId) != null
            if (!personExists) {
                Result.failure(IllegalArgumentException("Person with ID ${updatedEntry.personId} does not exist."))
            } else {
                ledgerEntryDao.updateEntry(updatedEntry)
                Result.success(Unit)
            }
        }

        return try {
            if (database != null) {
                database.withTransaction { updateOperation() }
            } else {
                updateOperation()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(entry: LedgerEntry): Result<Unit> {
        return try {
            ledgerEntryDao.deleteEntry(entry)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Persistent Undo Operations ---

    /**
     * Observes the most recent undoable ledger entry (globally or for a specific person).
     */
    fun observeLatestEntry(personId: Long? = null): Flow<LedgerEntry?> {
        return if (personId == null) {
            ledgerEntryDao.observeLatestEntry()
        } else {
            ledgerEntryDao.observeLatestEntryForPerson(personId)
        }
    }

    /**
     * Finds and deletes the most recent undoable LedgerEntry atomically.
     * Orders entries by timestamp DESC, id DESC to ensure reverse chronological undoing,
     * using the auto-generated ID as a tie-breaker for same-millisecond transactions.
     *
     * @param personId Optional person ID to undo the last transaction specifically for that person.
     *                 If null, undos the globally latest ledger entry.
     * @return Result containing the deleted LedgerEntry details on success,
     *         or a failure result (e.g. NoSuchElementException) if nothing is available to undo.
     */
    suspend fun undoLastLedgerEntry(personId: Long? = null): Result<LedgerEntry> {
        val undoOperation: suspend () -> Result<LedgerEntry> = {
            val latestEntry = if (personId == null) {
                ledgerEntryDao.getLatestEntry()
            } else {
                ledgerEntryDao.getLatestEntryForPerson(personId)
            }

            if (latestEntry == null) {
                Result.failure(NoSuchElementException("No transaction available to undo."))
            } else {
                ledgerEntryDao.deleteEntry(latestEntry)
                Result.success(latestEntry)
            }
        }

        return try {
            if (database != null) {
                database.withTransaction { undoOperation() }
            } else {
                undoOperation()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
