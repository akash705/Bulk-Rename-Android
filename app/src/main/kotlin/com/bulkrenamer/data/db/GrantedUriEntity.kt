package com.bulkrenamer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "granted_uris")
data class GrantedUriEntity(
    @PrimaryKey val uri: String,
    val grantedAt: Long
)
