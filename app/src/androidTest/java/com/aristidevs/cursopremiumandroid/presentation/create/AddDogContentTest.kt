package com.aristidevs.cursopremiumandroid.presentation.create

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aristidevs.cursopremiumandroid.R
import org.junit.Rule
import org.junit.Test

class AddDogContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    @Test
    fun confirmingTheDiscardDialogInvokesTheCallback() {
        var confirmed = false
        composeRule.setContent {
            AddDogContent(
                uiState = AddDogUiState(name = "Kira", showDiscardConfirmation = true),
                onNameChange = {}, onBreedChange = {}, onAgeChange = {}, onDescriptionChange = {},
                onWeightChange = {}, onOriginChange = {}, onTemperamentChange = {}, onPhotoPicked = {},
                onBackSelected = {}, onSaveClicked = {},
                onDiscardConfirmed = { confirmed = true }, onDiscardDismissed = {}
            )
        }

        composeRule.onNodeWithText(
            string(R.string.add_dog_discard_confirm)
        ).performClick()

        assert(confirmed)
    }

    @Test
    fun cancellingTheDiscardDialogKeepsWhatWasTyped() {
        var dismissed = false
        composeRule.setContent {
            AddDogContent(
                uiState = AddDogUiState(name = "Kira", showDiscardConfirmation = true),
                onNameChange = {}, onBreedChange = {}, onAgeChange = {}, onDescriptionChange = {},
                onWeightChange = {}, onOriginChange = {}, onTemperamentChange = {}, onPhotoPicked = {},
                onBackSelected = {}, onSaveClicked = {},
                onDiscardConfirmed = {}, onDiscardDismissed = { dismissed = true }
            )
        }

        composeRule.onNodeWithText(
            string(R.string.add_dog_discard_cancel)
        ).performClick()

        assert(dismissed)
        composeRule.onNodeWithText("Kira").assertExists()
    }
}
