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
    @Query("SELECT * FROM person ORDER BY name ASC")
    fun observeAllPeople(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE id = :id")
    fun observePersonById(id: Long): Flow<Person?>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun getPersonById(id: Long): Person?

    @Query("SELECT * FROM person")
    suspend fun getAllPeopleOnce(): List<Person>

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
