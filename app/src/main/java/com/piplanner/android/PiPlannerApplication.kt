package com.piplanner.android

import android.app.Application
import com.piplanner.android.data.GrokStubService
import com.piplanner.android.data.local.PreferencesDataStore
import com.piplanner.android.data.local.database.PiPlannerDatabase
import com.piplanner.android.data.repository.AccountRepository
import com.piplanner.android.data.repository.GoalRepository
import com.piplanner.android.data.repository.HistoryRepository

class PiPlannerApplication : Application() {
    
    val database by lazy { PiPlannerDatabase.getDatabase(this) }
    val preferencesDataStore by lazy { PreferencesDataStore(this) }
    
    val accountRepository by lazy { AccountRepository(database.accountDao()) }
    val goalRepository by lazy { GoalRepository(database.goalDao()) }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    
    val grokService by lazy { GrokStubService() }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: PiPlannerApplication
            private set
    }
}
