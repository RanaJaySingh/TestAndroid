package com.piplanner.data.repository

import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.AppState
import com.piplanner.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single repository between ViewModels and [PersistenceService] (MVVM / UDF).
 */
@Singleton
class PiPlannerRepository @Inject constructor(
    private val persistenceService: PersistenceService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeState(): Flow<AppState> = persistenceService.observeState()

    suspend fun loadState(): AppState = withContext(ioDispatcher) {
        persistenceService.loadState()
    }

    suspend fun saveState(state: AppState) = withContext(ioDispatcher) {
        persistenceService.saveState(state)
    }

    /** Reset demo — clears all persisted data (fresh / post-reset state). */
    suspend fun resetDemo() = withContext(ioDispatcher) {
        persistenceService.resetDemo()
    }
}
