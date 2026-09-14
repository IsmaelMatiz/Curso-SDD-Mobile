package com.aristidevs.cursopremiumandroid.presentation.create

import com.aristidevs.cursopremiumandroid.domain.usecase.AddDogUseCase
import com.aristidevs.cursopremiumandroid.fakes.FakeDogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddDogViewModelTest {

    private lateinit var repository: FakeDogRepository
    private lateinit var viewModel: AddDogViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = FakeDogRepository()
        viewModel = AddDogViewModel(AddDogUseCase(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm() {
        viewModel.onNameChange("Kira")
        viewModel.onBreedChange("Mestiza")
        viewModel.onAgeChange("4")
        viewModel.onDescriptionChange("Muy cariñosa")
        viewModel.onPhotoPicked("content://picked")
    }

    @Test
    fun `saving valid data persists one dog and signals navigating back`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        fillValidForm()

        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(1, repository.addedDogs.size)
        assertTrue(viewModel.uiState.value.navigateBack)
        job.cancel()
    }

    @Test
    fun `tapping save several times in a row only creates one dog`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        fillValidForm()

        viewModel.onSaveClicked()
        viewModel.onSaveClicked()
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(1, repository.addedDogs.size)
        job.cancel()
    }

    @Test
    fun `an invalid submission reports errors and persists nothing while keeping typed data`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        viewModel.onNameChange("Kira")
        // breed, age, description and photo left empty on purpose.

        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errors.isNotEmpty())
        assertTrue(repository.addedDogs.isEmpty())
        assertEquals("Kira", viewModel.uiState.value.name)
        job.cancel()
    }

    @Test
    fun `leaving with no data typed does not ask for confirmation`() = runTest {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onBackRequested()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.showDiscardConfirmation)
        assertTrue(viewModel.uiState.value.navigateBack)
        job.cancel()
    }

    @Test
    fun `leaving with unsaved data asks for confirmation and only discards when confirmed`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        viewModel.onNameChange("Kira")

        viewModel.onBackRequested()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDiscardConfirmation)
        assertEquals(false, viewModel.uiState.value.navigateBack)

        viewModel.onDiscardDismissed()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.showDiscardConfirmation)
        assertEquals("Kira", viewModel.uiState.value.name)

        viewModel.onBackRequested()
        viewModel.onDiscardConfirmed()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.navigateBack)
        job.cancel()
    }
}
