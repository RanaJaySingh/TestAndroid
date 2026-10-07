package com.piplanner.ui.setup

import com.google.common.truth.Truth.assertThat
import com.piplanner.util.DemoData
import org.junit.Before
import org.junit.Test

class WelcomeViewModelTest {

    private lateinit var viewModel: WelcomeViewModel

    @Before
    fun setUp() {
        viewModel = WelcomeViewModel()
    }

    @Test
    fun greeting_includesPersonaNameRahul() {
        val state = viewModel.uiState.value
        assertThat(state.personaName).isEqualTo("Rahul")
        assertThat(state.greeting).contains(DemoData.PERSONA_NAME)
        assertThat(state.greeting).matches("Good (morning|afternoon|evening), Rahul")
    }

    @Test
    fun exposesThreeHowItWorksStepsMatchingDesign() {
        val steps = viewModel.uiState.value.steps
        assertThat(steps).hasSize(3)
        assertThat(steps[0].title).isEqualTo("Pick a savings account")
        assertThat(steps[0].detail).isEqualTo("Only its balance is read.")
        assertThat(steps[1].title).isEqualTo("Set your goals")
        assertThat(steps[1].detail).isEqualTo("A target, an end date and a share of each credit.")
        assertThat(steps[2].title).isEqualTo("Split every new credit")
        assertThat(steps[2].detail).isEqualTo("Saved amounts lock, so they stay put.")
    }

    @Test
    fun setUpSavings_signalsNavigationToAccounts() {
        assertThat(viewModel.uiState.value.shouldNavigateToAccounts).isFalse()

        viewModel.setUpSavings()

        assertThat(viewModel.uiState.value.shouldNavigateToAccounts).isTrue()
    }

    @Test
    fun consumeNavigation_clearsNavigationFlag() {
        viewModel.setUpSavings()
        viewModel.consumeNavigation()
        assertThat(viewModel.uiState.value.shouldNavigateToAccounts).isFalse()
    }
}
