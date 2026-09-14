package com.aristidevs.cursopremiumandroid.domain.model

enum class DogValidationField {
    NAME, BREED, AGE, DESCRIPTION, PHOTO, WEIGHT, ORIGIN, TEMPERAMENT
}

enum class DogValidationReason {
    REQUIRED, TOO_LONG, OUT_OF_RANGE
}

/** Field-level validation error, without any UI text (RF-13, decisión de idiomas). */
data class DogValidationError(val field: DogValidationField, val reason: DogValidationReason)
