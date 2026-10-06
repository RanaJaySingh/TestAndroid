package com.piplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.AppState
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.ui.navigation.AppLaunchRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVVM + UDF scaffolding: exposes [AppState] via StateFlow.
 * Feature screens/ViewModels arrive in later tickets.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
) : ViewModel() {

    val uiState: StateFlow<AppState> = repository.observeState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppState.EMPTY,
        )

    fun resetDemo() {
        viewModelScope.launch {
            repository.resetDemo()
        }
    }

    /** Resolves the NavHost start route from persisted state (first-run / post-reset / setup-complete). */
    suspend fun resolveStartRoute(): String {
        return AppLaunchRouter.startRoute(repository.loadState())
    }
}
