package com.aristidevs.cursopremiumandroid.presentation.list

import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.domain.usecase.GetDogsUseCase
import com.aristidevs.cursopremiumandroid.domain.usecase.RefreshCatalogUseCase
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
class DogViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeDogRepository) =
        DogViewModel(GetDogsUseCase(repository), RefreshCatalogUseCase(repository))

    private fun dog(id: Long, name: String = "Firulais", breed: String = "Mestizo", isOwn: Boolean = false) =
        Dog(id = id, name = name, age = 3, description = "d", image = "img", breed = breed, isOwn = isOwn)

    @Test
    fun `search filters by name or breed including own dogs`() = runTest {
        val repository = FakeDogRepository().apply {
            dogs.value = listOf(
                dog(1, name = "Rex", breed = "Pastor"),
                dog(2, name = "Kira", breed = "Bulldog", isOwn = true)
            )
        }
        val viewModel = viewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onQueryChange("kira")
        advanceUntilIdle()
        assertEquals(listOf(2L), viewModel.uiState.value.dogs.map { it.id })

        viewModel.onQueryChange("bulldog")
        advanceUntilIdle()
        assertEquals(listOf(2L), viewModel.uiState.value.dogs.map { it.id })

        viewModel.onQueryChange("rex")
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.dogs.map { it.id })

        viewModel.onQueryChange("pastor")
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.dogs.map { it.id })

        job.cancel()
    }

    @Test
    fun `with data already saved there is no full-screen loading while refreshing`() = runTest {
        val repository = FakeDogRepository().apply { dogs.value = listOf(dog(1)) }
        val viewModel = viewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isInitialLoading)
        assertEquals(1, viewModel.uiState.value.dogs.size)
        job.cancel()
    }

    @Test
    fun `an empty catalog is distinguished from a search with no results`() = runTest {
        val repository = FakeDogRepository()
        val viewModel = viewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.totalDogsCount)
        assertTrue(viewModel.uiState.value.dogs.isEmpty())

        repository.dogs.value = listOf(dog(1, name = "Rex"))
        advanceUntilIdle()
        viewModel.onQueryChange("no coincide")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.totalDogsCount)
        assertTrue(viewModel.uiState.value.dogs.isEmpty())
        job.cancel()
    }

    @Test
    fun `a failed refresh is reported without clearing already loaded dogs`() = runTest {
        val repository = FakeDogRepository().apply {
            dogs.value = listOf(dog(1))
            refreshResult = CatalogRefreshResult.NoConnection
        }
        val viewModel = viewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(CatalogRefreshError.NO_CONNECTION, viewModel.uiState.value.refreshError)
        assertEquals(1, viewModel.uiState.value.dogs.size)
        job.cancel()
    }

    @Test
    fun `retryRefresh triggers another refresh call`() = runTest {
        val repository = FakeDogRepository()
        val viewModel = viewModel(repository)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val callsAfterInit = repository.refreshCallCount

        viewModel.retryRefresh()
        advanceUntilIdle()

        assertEquals(callsAfterInit + 1, repository.refreshCallCount)
        job.cancel()
    }
}
