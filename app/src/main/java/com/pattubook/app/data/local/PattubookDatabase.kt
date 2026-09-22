package com.pattubook.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pattubook.app.data.local.dao.LedgerEntryDao
import com.pattubook.app.data.local.dao.PersonDao
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.Person

@Database(
    entities = [Person::class, LedgerEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PattubookDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun ledgerEntryDao(): LedgerEntryDao

    companion object {
        @Volatile
        private var INSTANCE: PattubookDatabase? = null

        fun getDatabase(context: Context): PattubookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PattubookDatabase::class.java,
                    "pattubook_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
