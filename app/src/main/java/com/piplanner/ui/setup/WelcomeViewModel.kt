package com.piplanner.ui.setup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** One "How it works" step on Welcome (design frame 1). */
data class WelcomeStep(
    val id: Int,
    val title: String,
    val detail: String,
)

/**
 * View model for Welcome (frame 1) — PRD R1.
 * Copy matches iOS PIP-35 / design frame 1.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        WelcomeUiState(steps = DEFAULT_STEPS),
    )
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun setUpSavings() {
        _uiState.update { it.copy(shouldNavigateToAccounts = true) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(shouldNavigateToAccounts = false) }
    }

    companion object {
        val DEFAULT_STEPS: List<WelcomeStep> = listOf(
            WelcomeStep(
                id = 1,
                title = "Pick a savings account",
                detail = "Only its balance is read.",
            ),
            WelcomeStep(
                id = 2,
                title = "Set your goals",
                detail = "A target, an end date and a share of each credit.",
            ),
            WelcomeStep(
                id = 3,
                title = "Split every new credit",
                detail = "Saved amounts lock, so they stay put.",
            ),
        )
    }
}

data class WelcomeUiState(
    val steps: List<WelcomeStep> = emptyList(),
    val shouldNavigateToAccounts: Boolean = false,
)
