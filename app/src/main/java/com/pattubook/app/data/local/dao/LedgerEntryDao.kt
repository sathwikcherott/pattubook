package com.pattubook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pattubook.app.data.local.entity.LedgerEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerEntryDao {
    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND isDeleted = 0 ORDER BY isArchived ASC, timestamp DESC")
    fun observeEntriesForPerson(personId: Long): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun observeDeletedEntries(): Flow<List<LedgerEntry>>

    @Query("""
        SELECT l.*
        FROM ledger_entry l
        INNER JOIN person p ON l.personId = p.id
        WHERE l.isDeleted = 0 AND p.isDeleted = 0
        ORDER BY l.timestamp DESC, l.id DESC
    """)
    fun observeAllActiveEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN' AND isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC")
    fun observeGivenEntriesForPerson(personId: Long): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN_BACK' AND isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC")
    fun observeGivenBackEntriesForPerson(personId: Long): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE id = :id")
    suspend fun getEntryById(id: Long): LedgerEntry?

    @Query("SELECT * FROM ledger_entry")
    suspend fun getAllEntriesOnce(): List<LedgerEntry>

    @Query("SELECT * FROM ledger_entry WHERE isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestEntry(): LedgerEntry?

    @Query("SELECT * FROM ledger_entry WHERE isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLatestEntry(): Flow<LedgerEntry?>

    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestEntryForPerson(personId: Long): LedgerEntry?

    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLatestEntryForPerson(personId: Long): Flow<LedgerEntry?>

    @Query("UPDATE ledger_entry SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun setEntryDeleted(id: Long, isDeleted: Boolean): Int

    @Query("UPDATE ledger_entry SET isArchived = :isArchived WHERE id = :id")
    suspend fun setEntryArchived(id: Long, isArchived: Boolean): Int

    @Query("UPDATE ledger_entry SET isPaid = :isPaid WHERE id = :id AND type = 'GIVEN'")
    suspend fun setEntryPaid(id: Long, isPaid: Boolean): Int

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'GIVEN' AND isPaid = 0 THEN amountPaise
                WHEN type = 'GIVEN_BACK' THEN -amountPaise
                ELSE 0
            END
        ), 0)
        FROM ledger_entry
        WHERE personId = :personId AND isDeleted = 0
    """)
    fun observeOutstandingBalance(personId: Long): Flow<Long>

    @Query("DELETE FROM ledger_entry WHERE id = :id")
    suspend fun permanentlyDeleteEntry(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntry): Int

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry): Int

    @Query("DELETE FROM ledger_entry")
    suspend fun deleteAllEntries(): Int

    @Query("DELETE FROM ledger_entry WHERE personId = :personId")
    suspend fun deleteEntriesForPerson(personId: Long): Int

    @Query("DELETE FROM ledger_entry WHERE isDeleted = 1 OR personId IN (SELECT id FROM person WHERE isDeleted = 1)")
    suspend fun emptyRecycleBinEntries(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntry>): List<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN' AND isDeleted = 0")
    fun observeTotalGiven(personId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN' AND isDeleted = 0")
    suspend fun getTotalGiven(personId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN_BACK' AND isDeleted = 0")
    fun observeTotalGivenBack(personId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN_BACK' AND isDeleted = 0")
    suspend fun getTotalGivenBack(personId: Long): Long

    @Query("""
        SELECT COALESCE(SUM(l.amountPaise), 0)
        FROM ledger_entry l
        INNER JOIN person p ON l.personId = p.id
        WHERE p.isDeleted = 0 AND p.isHidden = 0 AND l.isDeleted = 0 AND l.type = 'GIVEN'
    """)
    fun observeGlobalTotalGiven(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(l.amountPaise), 0)
        FROM ledger_entry l
        INNER JOIN person p ON l.personId = p.id
        WHERE p.isDeleted = 0 AND p.isHidden = 0 AND l.isDeleted = 0 AND l.type = 'GIVEN_BACK'
    """)
    fun observeGlobalTotalGivenBack(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN l.type = 'GIVEN' AND l.isPaid = 0 THEN l.amountPaise
                WHEN l.type = 'GIVEN_BACK' THEN -l.amountPaise
                ELSE 0
            END
        ), 0)
        FROM ledger_entry l
        INNER JOIN person p ON l.personId = p.id
        WHERE p.isDeleted = 0 AND p.isHidden = 0 AND l.isDeleted = 0
    """)
    fun observeGlobalTotalOutstanding(): Flow<Long>

    @Query("""
        SELECT 
            personId,
            COALESCE(SUM(
                CASE
                    WHEN type = 'GIVEN' AND isPaid = 0 THEN amountPaise
                    WHEN type = 'GIVEN_BACK' THEN -amountPaise
                    ELSE 0
                END
            ), 0) AS outstandingBalancePaise
        FROM ledger_entry
        WHERE isDeleted = 0
        GROUP BY personId
    """)
    fun observeAllPersonBalances(): Flow<List<PersonBalance>>
}
