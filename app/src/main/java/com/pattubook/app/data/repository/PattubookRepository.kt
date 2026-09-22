package com.pattubook.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.local.dao.LedgerEntryDao
import com.pattubook.app.data.local.dao.PersonDao
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

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

    fun observeAllPersonBalances(): Flow<Map<Long, Long>> {
        return ledgerEntryDao.observeAllPersonBalances().map { list ->
            list.associate { it.personId to it.outstandingBalancePaise }
        }
    }

    fun observeGlobalTotalGiven(): Flow<Long> =
        ledgerEntryDao.observeGlobalTotalGiven()

    fun observeGlobalTotalGivenBack(): Flow<Long> =
        ledgerEntryDao.observeGlobalTotalGivenBack()

    fun observeGlobalTotalOutstanding(): Flow<Long> {
        return combine(
            observeGlobalTotalGiven(),
            observeGlobalTotalGivenBack()
        ) { totalGiven, totalGivenBack ->
            totalGiven - totalGivenBack
        }
    }

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

    // --- Data Management & Backup Operations ---

    /**
     * Atomically deletes all ledger entries and people from the Room database.
     */
    suspend fun deleteAllData(): Result<Unit> {
        val deleteOperation: suspend () -> Result<Unit> = {
            ledgerEntryDao.deleteAllEntries()
            personDao.deleteAllPeople()
            Result.success(Unit)
        }

        return try {
            if (database != null) {
                database.withTransaction { deleteOperation() }
            } else {
                deleteOperation()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a self-contained, versioned JSON backup of all people and ledger entries.
     * All monetary values are preserved strictly as Long integer paise.
     */
    suspend fun generateBackupJson(): Result<String> {
        return try {
            val people = personDao.getAllPeopleOnce()
            val entries = ledgerEntryDao.getAllEntriesOnce()

            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            val peopleArray = JSONArray()
            for (person in people) {
                val personObj = JSONObject()
                personObj.put("id", person.id)
                personObj.put("name", person.name)
                personObj.put("createdAt", person.createdAt)
                peopleArray.put(personObj)
            }
            root.put("people", peopleArray)

            val entriesArray = JSONArray()
            for (entry in entries) {
                val entryObj = JSONObject()
                entryObj.put("id", entry.id)
                entryObj.put("personId", entry.personId)
                entryObj.put("amountPaise", entry.amountPaise)
                entryObj.put("type", entry.type.name)
                entryObj.put("timestamp", entry.timestamp)
                if (entry.note != null) {
                    entryObj.put("note", entry.note)
                } else {
                    entryObj.put("note", JSONObject.NULL)
                }
                entriesArray.put(entryObj)
            }
            root.put("ledgerEntries", entriesArray)

            Result.success(root.toString(2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes the backup JSON string to an SAF document Uri off the main thread.
     */
    suspend fun exportBackupToUri(context: Context, uri: Uri, jsonString: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                } ?: return@withContext Result.failure(IOException("Failed to open output stream for chosen file location."))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Restore Data Import Operations ---

    /**
     * Reads, parses, validates, and restores a backup JSON file from the chosen SAF Uri.
     * Performs atomic database replacement inside a Room transaction ONLY IF validation passes.
     */
    suspend fun restoreBackupFromUri(context: Context, uri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Read JSON string from Uri
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: return@withContext Result.failure(IOException("Failed to open input stream for backup file."))

                // 2. Parse & Validate JSON
                val root = JSONObject(jsonString)

                if (!root.has("version")) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid backup file: Missing 'version' field."))
                }
                val version = root.getInt("version")
                if (version != 1) {
                    return@withContext Result.failure(IllegalArgumentException("Unsupported backup version: $version."))
                }

                val peopleArray = root.optJSONArray("people")
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid backup file: Missing 'people' array."))

                val entriesArray = root.optJSONArray("ledgerEntries")
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid backup file: Missing 'ledgerEntries' array."))

                val parsedPeople = mutableListOf<Person>()
                val personIdsSet = mutableSetOf<Long>()

                for (i in 0 until peopleArray.length()) {
                    val pObj = peopleArray.getJSONObject(i)
                    val id = pObj.getLong("id")
                    val name = pObj.getString("name").trim()
                    val createdAt = pObj.getLong("createdAt")

                    if (name.isEmpty()) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid backup: Person with ID $id has a blank name."))
                    }

                    if (!personIdsSet.add(id)) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid backup: Duplicate person ID $id found."))
                    }

                    parsedPeople.add(Person(id = id, name = name, createdAt = createdAt))
                }

                val parsedEntries = mutableListOf<LedgerEntry>()
                val entryIdsSet = mutableSetOf<Long>()

                for (i in 0 until entriesArray.length()) {
                    val eObj = entriesArray.getJSONObject(i)
                    val id = eObj.getLong("id")
                    val personId = eObj.getLong("personId")
                    val amountPaise = eObj.getLong("amountPaise")
                    val typeStr = eObj.getString("type")
                    val timestamp = eObj.getLong("timestamp")
                    val note = if (eObj.isNull("note")) null else eObj.optString("note").trim().ifEmpty { null }

                    if (!personIdsSet.contains(personId)) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid backup: Ledger entry $id references non-existent person ID $personId."))
                    }

                    if (amountPaise < 0) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid backup: Ledger entry $id has negative amount $amountPaise."))
                    }

                    val type = try {
                        LedgerEntryType.valueOf(typeStr)
                    } catch (_: Exception) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid backup: Ledger entry $id has unknown transaction type '$typeStr'."))
                    }

                    if (!entryIdsSet.add(id)) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid backup: Duplicate ledger entry ID $id found."))
                    }

                    parsedEntries.add(
                        LedgerEntry(
                            id = id,
                            personId = personId,
                            amountPaise = amountPaise,
                            type = type,
                            timestamp = timestamp,
                            note = note
                        )
                    )
                }

                // 3. Perform Atomic Database Replacement inside a Room Transaction
                val replaceOperation: suspend () -> Result<Unit> = {
                    ledgerEntryDao.deleteAllEntries()
                    personDao.deleteAllPeople()
                    personDao.insertPeople(parsedPeople)
                    ledgerEntryDao.insertEntries(parsedEntries)
                    Result.success(Unit)
                }

                if (database != null) {
                    database.withTransaction { replaceOperation() }
                } else {
                    replaceOperation()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
