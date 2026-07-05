package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fm_stations")
data class FmStation(
    @PrimaryKey
    val frequencyMHz: Float,
    val name: String,
    val isFavorite: Boolean = false,
    val rdsText: String = "",
    val signalStrengthRssi: Int = 0,
    val isStereo: Boolean = true
) {
    val formattedFrequency: String
        get() = String.format("%.1f", frequencyMHz)
}
