package com.pattubook.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pattubook.app.data.local.dao.LedgerEntryDao
import com.pattubook.app.data.local.dao.PersonDao
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.Person

@Database(
    entities = [Person::class, LedgerEntry::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PattubookDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun ledgerEntryDao(): LedgerEntryDao

    companion object {
        @Volatile
        private var INSTANCE: PattubookDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE person ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE person ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ledger_entry ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): PattubookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PattubookDatabase::class.java,
                    "pattubook_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
