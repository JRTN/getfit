package com.jrussell.fitness.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import com.jrussell.fitness.navigation.TopLevelBackStack
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Each tab keeps its screen state (here, a saveable counter) while another tab is showing. */
@RunWith(RobolectricTestRunner::class)
class TabNavDisplayTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    private val navigation: TopLevelBackStack<String> = TopLevelBackStack("home", tabs = listOf("home", "body"))

    @Test
    fun switchingTabs_keepsTheHiddenTabsState() {
        show()
        rule.onNodeWithText("home 0").assertIsDisplayed()
        rule.runOnIdle { navigation.switchTo("body") }
        rule.onNodeWithText("body 0").performClick()
        rule.onNodeWithText("body 1").performClick()

        rule.runOnIdle { navigation.switchTo("home") }
        rule.onNodeWithText("home 0").assertIsDisplayed()
        rule.runOnIdle { navigation.switchTo("body") }

        rule.onNodeWithText("body 2").assertIsDisplayed()
    }

    @Test
    fun backFromAnotherTabsRoot_keepsItsStateForNextTime() {
        show()
        rule.runOnIdle { navigation.switchTo("body") }
        rule.onNodeWithText("body 0").performClick()

        rule.runOnIdle { navigation.removeLast() }
        rule.onNodeWithText("home 0").assertIsDisplayed()
        rule.runOnIdle { navigation.switchTo("body") }

        rule.onNodeWithText("body 1").assertIsDisplayed()
    }

    private fun show() {
        rule.setContent {
            TabNavDisplay(
                navigation = navigation,
                onExit = {},
                entryProvider = { key: String ->
                    NavEntry(key) {
                        var taps: Int by rememberSaveable { mutableIntStateOf(0) }
                        Button(onClick = { taps++ }) { Text("$key $taps") }
                    }
                },
            )
        }
    }
}
