package com.aristidevs.cursopremiumandroid.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.domain.usecase.GetDogsUseCase
import com.aristidevs.cursopremiumandroid.domain.usecase.RefreshCatalogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DogViewModel @Inject constructor(
    getDogsUseCase: GetDogsUseCase,
    private val refreshCatalogUseCase: RefreshCatalogUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val refreshError = MutableStateFlow<CatalogRefreshError?>(null)
    private val isRefreshing = MutableStateFlow(true)

    val uiState: StateFlow<DogsUiState> = combine(
        getDogsUseCase(), query, refreshError, isRefreshing
    ) { dogs, currentQuery, error, refreshing ->
        DogsUiState(
            isInitialLoading = refreshing && dogs.isEmpty(),
            dogs = dogs.filterByQuery(currentQuery),
            totalDogsCount = dogs.size,
            query = currentQuery,
            refreshError = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DogsUiState())

    init {
        refresh()
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun retryRefresh() {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                refreshError.value = when (refreshCatalogUseCase()) {
                    CatalogRefreshResult.Success -> null
                    CatalogRefreshResult.NoConnection -> CatalogRefreshError.NO_CONNECTION
                    is CatalogRefreshResult.UnexpectedError -> CatalogRefreshError.UNEXPECTED
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun List<Dog>.filterByQuery(currentQuery: String): List<Dog> {
        if (currentQuery.isBlank()) return this
        return filter { dog ->
            dog.name.contains(currentQuery, ignoreCase = true) ||
                dog.breed.contains(currentQuery, ignoreCase = true)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

enum class CatalogRefreshError { NO_CONNECTION, UNEXPECTED }

data class DogsUiState(
    val isInitialLoading: Boolean = true,
    val dogs: List<Dog> = emptyList(),
    val totalDogsCount: Int = 0,
    val query: String = "",
    val refreshError: CatalogRefreshError? = null
)
