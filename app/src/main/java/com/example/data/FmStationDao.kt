package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FmStationDao {
    @Query("SELECT * FROM fm_stations ORDER BY frequencyMHz ASC")
    fun getAllStations(): Flow<List<FmStation>>

    @Query("SELECT * FROM fm_stations WHERE isFavorite = 1 ORDER BY frequencyMHz ASC")
    fun getFavoriteStations(): Flow<List<FmStation>>

    @Query("SELECT * FROM fm_stations WHERE frequencyMHz = :freq LIMIT 1")
    suspend fun getStationByFrequency(freq: Float): FmStation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStation(station: FmStation)

    @Delete
    suspend fun deleteStation(station: FmStation)

    @Query("DELETE FROM fm_stations")
    suspend fun deleteAllStations()
}
