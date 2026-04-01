package com.bulkrenamer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GrantedUriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(uri: GrantedUriEntity)

    @Delete
    suspend fun delete(uri: GrantedUriEntity)

    @Query("SELECT * FROM granted_uris ORDER BY grantedAt ASC")
    suspend fun getAll(): List<GrantedUriEntity>

    @Query("SELECT COUNT(*) FROM granted_uris")
    suspend fun getCount(): Int
}
