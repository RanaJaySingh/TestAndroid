package com.piplanner.data.local

import com.piplanner.data.model.AppState
import kotlinx.coroutines.flow.Flow

/**
 * Local demo persistence (Spec §5.1–5.2).
 * Fresh install and post-[resetDemo] expose empty [AppState].
 */
interface PersistenceService {
    /** Observes persisted demo state; emits [AppState.EMPTY] when nothing is stored. */
    fun observeState(): Flow<AppState>

    suspend fun loadState(): AppState

    suspend fun saveState(state: AppState)

    /** Clears all persisted data (Reset demo). */
    suspend fun resetDemo()
}
