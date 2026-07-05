package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FmStation::class], version = 1, exportSchema = false)
abstract class FmDatabase : RoomDatabase() {
    abstract fun fmStationDao(): FmStationDao

    companion object {
        @Volatile
        private var INSTANCE: FmDatabase? = null

        fun getDatabase(context: Context): FmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FmDatabase::class.java,
                    "fm_radio_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
