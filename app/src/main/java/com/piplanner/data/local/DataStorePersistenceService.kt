package com.piplanner.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.piplanner.data.model.AppState
import com.piplanner.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed [PersistenceService] storing serialized [AppState] JSON.
 */
@Singleton
class DataStorePersistenceService @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val serializer: AppStateJsonSerializer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PersistenceService {

    override fun observeState(): Flow<AppState> {
        return dataStore.data
            .map { preferences -> preferences.toAppState() }
            .catch { error ->
                if (error is IOException) {
                    emit(AppState.EMPTY)
                } else {
                    throw error
                }
            }
    }

    override suspend fun loadState(): AppState = withContext(ioDispatcher) {
        dataStore.data.first().toAppState()
    }

    override suspend fun saveState(state: AppState) {
        withContext(ioDispatcher) {
            val encoded = serializer.encode(state)
            dataStore.edit { preferences ->
                preferences[KEY_APP_STATE] = encoded
            }
            Unit
        }
    }

    override suspend fun resetDemo() {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Unit
        }
    }

    private fun Preferences.toAppState(): AppState {
        val raw = this[KEY_APP_STATE]
        if (raw.isNullOrBlank()) {
            return AppState.EMPTY
        }
        return serializer.decode(raw)
    }

    companion object {
        val KEY_APP_STATE = stringPreferencesKey("app_state_json")
    }
}
