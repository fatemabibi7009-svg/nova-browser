package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shield_stats")
data class ShieldStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val trackersBlocked: Long = 0,
    val adsBlocked: Long = 0,
    val dataSavedKb: Long = 0,
    val timeSavedMs: Long = 0
)
