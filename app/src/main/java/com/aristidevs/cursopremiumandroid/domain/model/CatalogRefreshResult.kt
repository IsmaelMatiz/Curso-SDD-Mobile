package com.aristidevs.cursopremiumandroid.domain.model

sealed interface CatalogRefreshResult {
    data object Success : CatalogRefreshResult
    data object NoConnection : CatalogRefreshResult
    data class UnexpectedError(val message: String?) : CatalogRefreshResult
}
