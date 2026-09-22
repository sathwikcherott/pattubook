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
    @Query("SELECT * FROM ledger_entry WHERE personId = :personId ORDER BY timestamp DESC")
    fun observeEntriesForPerson(personId: Long): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN' ORDER BY timestamp DESC")
    fun observeGivenEntriesForPerson(personId: Long): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN_BACK' ORDER BY timestamp DESC")
    fun observeGivenBackEntriesForPerson(personId: Long): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entry WHERE id = :id")
    suspend fun getEntryById(id: Long): LedgerEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntry): Int

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry): Int

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN'")
    fun observeTotalGiven(personId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN'")
    suspend fun getTotalGiven(personId: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN_BACK'")
    fun observeTotalGivenBack(personId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM ledger_entry WHERE personId = :personId AND type = 'GIVEN_BACK'")
    suspend fun getTotalGivenBack(personId: Long): Long
}
