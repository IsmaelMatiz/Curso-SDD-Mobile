package com.aristidevs.cursopremiumandroid.presentation.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.aristidevs.cursopremiumandroid.R
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import org.junit.Rule
import org.junit.Test

class DogContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun dog(id: Long, name: String, isOwn: Boolean) =
        Dog(id = id, name = name, age = 3, description = "d", image = "img", breed = "Mestizo", isOwn = isOwn)

    @Test
    fun emptyCatalogShowsTheNoDogsMessageNotTheSearchMessage() {
        composeRule.setContent {
            DogContent(
                uiState = DogsUiState(isInitialLoading = false, totalDogsCount = 0),
                onQueryChange = {}, onDogClicked = {}, onAddDogClicked = {}, onRetryRefresh = {}
            )
        }

        composeRule.onNodeWithText(
            string(R.string.dogs_empty_no_dogs_title)
        ).assertExists()
    }

    @Test
    fun aSearchWithNoMatchesShowsTheNoResultsMessageNotTheEmptyCatalogMessage() {
        composeRule.setContent {
            DogContent(
                uiState = DogsUiState(
                    isInitialLoading = false,
                    totalDogsCount = 1,
                    dogs = emptyList(),
                    query = "no coincide"
                ),
                onQueryChange = {}, onDogClicked = {}, onAddDogClicked = {}, onRetryRefresh = {}
            )
        }

        composeRule.onNodeWithText(
            string(R.string.dogs_empty_no_search_results)
        ).assertExists()
    }

    @Test
    fun ownDogsAppearFirstAndCarryTheOwnLabel() {
        composeRule.setContent {
            DogContent(
                uiState = DogsUiState(
                    isInitialLoading = false,
                    totalDogsCount = 2,
                    dogs = listOf(dog(1, "Rex", isOwn = false), dog(2, "Kira", isOwn = true))
                ),
                onQueryChange = {}, onDogClicked = {}, onAddDogClicked = {}, onRetryRefresh = {}
            )
        }

        composeRule.onNodeWithText(
            string(R.string.dogs_own_label)
        ).assertExists()
    }
}
