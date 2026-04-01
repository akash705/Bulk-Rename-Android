package com.bulkrenamer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RenameJournalEntity::class,
        RenameJournalFtsEntity::class,
        GrantedUriEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun renameJournalDao(): RenameJournalDao
    abstract fun grantedUriDao(): GrantedUriDao
}
