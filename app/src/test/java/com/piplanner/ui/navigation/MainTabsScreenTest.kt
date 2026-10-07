package com.piplanner.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.piplanner.ui.theme.MainTabChrome
import com.piplanner.ui.theme.PiPlannerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI check on MainTabsScreen chrome (PIP-72 / PRD R5).
 * Uses [MainTabsBottomBar] so Hilt ViewModels are not required.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainTabsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomBar_showsExactlyThreeTabsGoalsHistoryAsk_withMaterialIconsNotGlyphs() {
        composeRule.setContent {
            PiPlannerTheme {
                MainTabsBottomBar(
                    selectedIndex = MainTabChrome.Tab.Goals.index,
                    onSelect = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Main tab bar").assertIsDisplayed()
        composeRule.onNodeWithText("Goals").assertIsDisplayed()
        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText("Ask").assertIsDisplayed()

        // Material Icons expose tab contentDescriptions (not ◎ / ◷ / ? glyphs).
        composeRule.onAllNodesWithContentDescription("Goals tab", useUnmergedTree = true)
            .assertCountEquals(2)
        composeRule.onAllNodesWithContentDescription("History tab", useUnmergedTree = true)
            .assertCountEquals(2)
        composeRule.onAllNodesWithContentDescription("Ask tab", useUnmergedTree = true)
            .assertCountEquals(2)

        composeRule.onAllNodesWithText("Settings").assertCountEquals(0)
        composeRule.onAllNodesWithText("◎").assertCountEquals(0)
        composeRule.onAllNodesWithText("◷").assertCountEquals(0)
        composeRule.onAllNodesWithText("?").assertCountEquals(0)
    }

    @Test
    fun bottomBar_selectedVsUnselected_updatesOnClick() {
        var selected by mutableIntStateOf(MainTabChrome.Tab.Goals.index)
        composeRule.setContent {
            PiPlannerTheme {
                MainTabsBottomBar(
                    selectedIndex = selected,
                    onSelect = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("Goals").assertIsSelected()
        composeRule.onNodeWithText("History").assertIsNotSelected()
        composeRule.onNodeWithText("Ask").assertIsNotSelected()

        composeRule.onNodeWithText("History").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("History").assertIsSelected()
        composeRule.onNodeWithText("Goals").assertIsNotSelected()
        composeRule.onNodeWithText("Ask").assertIsNotSelected()
    }
}
