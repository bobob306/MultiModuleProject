package com.bsdevs.uicomponents

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class MMPScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mmpScaffold_displaysTitle() {
        val title = "Test Title"
        composeTestRule.setContent {
            MMPScaffold(
                title = title,
                onBackClick = {}
            ) {
                Text("Content")
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun mmpScaffold_displaysContent() {
        val content = "Scaffold Content"
        composeTestRule.setContent {
            MMPScaffold(
                title = "Title"
            ) {
                Text(content)
            }
        }

        composeTestRule.onNodeWithText(content).assertIsDisplayed()
    }
}
