package com.pattubook.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_entry",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personId")
    ]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val amountPaise: Long,
    val type: LedgerEntryType,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val isDeleted: Boolean = false,
) {
    init {
        require(amountPaise >= 0) { "amountPaise must be non-negative" }
    }
}
