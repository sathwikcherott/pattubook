package com.pattubook.app.data.local

import androidx.room.TypeConverter
import com.pattubook.app.data.local.entity.LedgerEntryType

class Converters {
    @TypeConverter
    fun fromLedgerEntryType(type: LedgerEntryType): String {
        return type.name
    }

    @TypeConverter
    fun toLedgerEntryType(value: String): LedgerEntryType {
        return LedgerEntryType.valueOf(value)
    }
}
