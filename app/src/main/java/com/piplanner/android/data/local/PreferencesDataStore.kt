package com.piplanner.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.piplanner.android.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "piplanner_preferences")

class PreferencesDataStore(private val context: Context) {
    
    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val HAS_COMPLETED_SETUP = booleanPreferencesKey("has_completed_setup")
        val AUTO_BALANCE_UPDATES = booleanPreferencesKey("auto_balance_updates")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val DEFAULT_INFLATION_RATE = doublePreferencesKey("default_inflation_rate")
    }
    
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            userName = preferences[PreferencesKeys.USER_NAME] ?: "Rahul",
            hasCompletedSetup = preferences[PreferencesKeys.HAS_COMPLETED_SETUP] ?: false,
            autoBalanceUpdates = preferences[PreferencesKeys.AUTO_BALANCE_UPDATES] ?: true,
            lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP],
            defaultInflationRate = preferences[PreferencesKeys.DEFAULT_INFLATION_RATE] ?: 7.0
        )
    }
    
    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }
    
    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_SETUP] = completed
        }
    }
    
    suspend fun setAutoBalanceUpdates(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BALANCE_UPDATES] = enabled
        }
    }
    
    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }
    
    suspend fun setDefaultInflationRate(rate: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_INFLATION_RATE] = rate
        }
    }
    
    suspend fun resetAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
