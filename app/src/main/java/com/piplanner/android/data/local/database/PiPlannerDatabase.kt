package com.piplanner.android.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.piplanner.android.data.local.dao.AccountDao
import com.piplanner.android.data.local.dao.GoalDao
import com.piplanner.android.data.local.dao.HistoryDao
import com.piplanner.android.data.local.entity.AccountEntity
import com.piplanner.android.data.local.entity.GoalEntity
import com.piplanner.android.data.local.entity.HistoryEntryEntity

@Database(
    entities = [
        AccountEntity::class,
        GoalEntity::class,
        HistoryEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PiPlannerDatabase : RoomDatabase() {
    
    abstract fun accountDao(): AccountDao
    abstract fun goalDao(): GoalDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: PiPlannerDatabase? = null
        
        fun getDatabase(context: Context): PiPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PiPlannerDatabase::class.java,
                    "piplanner_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
