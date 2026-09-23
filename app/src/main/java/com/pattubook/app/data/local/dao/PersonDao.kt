package com.pattubook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pattubook.app.data.local.entity.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM person WHERE isDeleted = 0 ORDER BY isHidden ASC, name ASC")
    fun observeAllPeople(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE isDeleted = 1 ORDER BY name ASC")
    fun observeDeletedPeople(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE id = :id AND isDeleted = 0")
    fun observePersonById(id: Long): Flow<Person?>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun getPersonById(id: Long): Person?

    @Query("SELECT * FROM person")
    suspend fun getAllPeopleOnce(): List<Person>

    @Query("UPDATE person SET isHidden = :isHidden WHERE id = :id")
    suspend fun setPersonHidden(id: Long, isHidden: Boolean): Int

    @Query("UPDATE person SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun setPersonDeleted(id: Long, isDeleted: Boolean): Int

    @Query("DELETE FROM person WHERE isDeleted = 1")
    suspend fun emptyRecycleBinPeople(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Update
    suspend fun updatePerson(person: Person): Int

    @Delete
    suspend fun deletePerson(person: Person): Int

    @Query("DELETE FROM person")
    suspend fun deleteAllPeople(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<Person>): List<Long>
}
