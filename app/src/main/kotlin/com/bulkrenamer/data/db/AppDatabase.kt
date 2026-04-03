package com.bulkrenamer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RenameJournalEntity::class,
        RenameJournalFtsEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun renameJournalDao(): RenameJournalDao
}
