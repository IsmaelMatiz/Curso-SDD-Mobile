package com.aristidevs.cursopremiumandroid.presentation.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.aristidevs.cursopremiumandroid.domain.model.DogDetailModel
import org.junit.Rule
import org.junit.Test

class DogDetailContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun detail(weight: String?, origin: String?, temperament: String?) = DogDetailModel(
        id = 1, name = "Rex", breed = "Mestizo", age = 3, description = "Buen perro",
        image = "img", weight = weight, origin = origin, temperament = temperament
    )

    @Test
    fun emptyOptionalFieldsDoNotRender() {
        composeRule.setContent {
            DogDetailContent(
                uiState = DogDetailUiState.Success(detail(weight = null, origin = null, temperament = null)),
                onBackSelected = {}
            )
        }

        composeRule.onNodeWithText("Peso").assertDoesNotExist()
        composeRule.onNodeWithText("Origen").assertDoesNotExist()
    }

    @Test
    fun filledOptionalFieldsDoRender() {
        composeRule.setContent {
            DogDetailContent(
                uiState = DogDetailUiState.Success(
                    detail(weight = "12kg", origin = "Francia", temperament = "Juguetón")
                ),
                onBackSelected = {}
            )
        }

        composeRule.onNodeWithText("Peso").assertExists()
        composeRule.onNodeWithText("12kg").assertExists()
        composeRule.onNodeWithText("Origen").assertExists()
        composeRule.onNodeWithText("Francia").assertExists()
    }
}
