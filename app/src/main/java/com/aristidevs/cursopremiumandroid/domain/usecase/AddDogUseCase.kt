package com.aristidevs.cursopremiumandroid.domain.usecase

import com.aristidevs.cursopremiumandroid.domain.DogRepository
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationError
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.AGE
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.BREED
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.DESCRIPTION
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.NAME
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.ORIGIN
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.PHOTO
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.TEMPERAMENT
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField.WEIGHT
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationReason.OUT_OF_RANGE
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationReason.REQUIRED
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationReason.TOO_LONG
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import javax.inject.Inject

sealed interface AddDogResult {
    data class Success(val id: Long) : AddDogResult
    data class Invalid(val errors: Set<DogValidationError>) : AddDogResult
}

/** Validates a [NewDog] against SPEC.md's rules and, if valid, persists it (RF-01). */
class AddDogUseCase @Inject constructor(private val repository: DogRepository) {

    suspend operator fun invoke(newDog: NewDog): AddDogResult {
        val errors = validate(newDog)
        if (errors.isNotEmpty()) return AddDogResult.Invalid(errors)
        return AddDogResult.Success(repository.addDog(newDog))
    }

    private fun validate(newDog: NewDog): Set<DogValidationError> {
        val errors = mutableSetOf<DogValidationError>()

        if (newDog.name.isBlank()) {
            errors += DogValidationError(NAME, REQUIRED)
        } else if (newDog.name.length > SHORT_FIELD_MAX_LENGTH) {
            errors += DogValidationError(NAME, TOO_LONG)
        }

        if (newDog.breed.isBlank()) {
            errors += DogValidationError(BREED, REQUIRED)
        } else if (newDog.breed.length > SHORT_FIELD_MAX_LENGTH) {
            errors += DogValidationError(BREED, TOO_LONG)
        }

        val age = newDog.age
        if (age == null) {
            errors += DogValidationError(AGE, REQUIRED)
        } else if (age !in MIN_AGE..MAX_AGE) {
            errors += DogValidationError(AGE, OUT_OF_RANGE)
        }

        if (newDog.description.isBlank()) {
            errors += DogValidationError(DESCRIPTION, REQUIRED)
        } else if (newDog.description.length > DESCRIPTION_MAX_LENGTH) {
            errors += DogValidationError(DESCRIPTION, TOO_LONG)
        }

        if (newDog.photoUri.isNullOrBlank()) {
            errors += DogValidationError(PHOTO, REQUIRED)
        }

        validateOptionalShortField(newDog.weight, WEIGHT)?.let { errors += it }
        validateOptionalShortField(newDog.origin, ORIGIN)?.let { errors += it }
        validateOptionalShortField(newDog.temperament, TEMPERAMENT)?.let { errors += it }

        return errors
    }

    private fun validateOptionalShortField(
        value: String?,
        field: DogValidationField
    ): DogValidationError? {
        if (value.isNullOrBlank()) return null
        if (value.length > SHORT_FIELD_MAX_LENGTH) return DogValidationError(field, TOO_LONG)
        return null
    }

    private companion object {
        const val MIN_AGE = 0
        const val MAX_AGE = 30
        const val SHORT_FIELD_MAX_LENGTH = 50
        const val DESCRIPTION_MAX_LENGTH = 300
    }
}
