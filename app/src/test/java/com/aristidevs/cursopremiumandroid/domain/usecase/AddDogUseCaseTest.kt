package com.aristidevs.cursopremiumandroid.domain.usecase

import com.aristidevs.cursopremiumandroid.domain.model.DogValidationError
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationReason
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import com.aristidevs.cursopremiumandroid.fakes.FakeDogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddDogUseCaseTest {

    private lateinit var repository: FakeDogRepository
    private lateinit var useCase: AddDogUseCase

    private val validDog = NewDog(
        name = "Kira", breed = "Mestiza", age = 4, description = "Muy cariñosa",
        photoUri = "content://picked", weight = "8kg", origin = "Italia", temperament = "Calmada"
    )

    @Before
    fun setUp() {
        repository = FakeDogRepository()
        useCase = AddDogUseCase(repository)
    }

    @Test
    fun `valid data is persisted and nothing is reported as invalid`() = runTest {
        val result = useCase(validDog)

        assertTrue(result is AddDogResult.Success)
        assertEquals(1, repository.addedDogs.size)
    }

    @Test
    fun `a blank required field is reported and nothing is persisted`() = runTest {
        val result = useCase(validDog.copy(name = "   "))

        assertTrue(result is AddDogResult.Invalid)
        assertEquals(
            setOf(DogValidationError(DogValidationField.NAME, DogValidationReason.REQUIRED)),
            (result as AddDogResult.Invalid).errors
        )
        assertTrue(repository.addedDogs.isEmpty())
    }

    @Test
    fun `a missing photo is reported and nothing is persisted`() = runTest {
        val result = useCase(validDog.copy(photoUri = null))

        assertTrue(result is AddDogResult.Invalid)
        assertTrue(
            (result as AddDogResult.Invalid).errors
                .contains(DogValidationError(DogValidationField.PHOTO, DogValidationReason.REQUIRED))
        )
        assertTrue(repository.addedDogs.isEmpty())
    }

    @Test
    fun `age 31 is out of range and nothing is persisted`() = runTest {
        val result = useCase(validDog.copy(age = 31))

        assertTrue(result is AddDogResult.Invalid)
        assertEquals(
            setOf(DogValidationError(DogValidationField.AGE, DogValidationReason.OUT_OF_RANGE)),
            (result as AddDogResult.Invalid).errors
        )
        assertTrue(repository.addedDogs.isEmpty())
    }

    @Test
    fun `age 0 and age 30 are both within range`() = runTest {
        assertTrue(useCase(validDog.copy(age = 0)) is AddDogResult.Success)
        assertTrue(useCase(validDog.copy(age = 30)) is AddDogResult.Success)
    }

    @Test
    fun `a name longer than 50 characters is reported and nothing is persisted`() = runTest {
        val result = useCase(validDog.copy(name = "n".repeat(51)))

        assertTrue(result is AddDogResult.Invalid)
        assertEquals(
            setOf(DogValidationError(DogValidationField.NAME, DogValidationReason.TOO_LONG)),
            (result as AddDogResult.Invalid).errors
        )
        assertTrue(repository.addedDogs.isEmpty())
    }

    @Test
    fun `a description longer than 300 characters is reported and nothing is persisted`() = runTest {
        val result = useCase(validDog.copy(description = "d".repeat(301)))

        assertTrue(result is AddDogResult.Invalid)
        assertEquals(
            setOf(DogValidationError(DogValidationField.DESCRIPTION, DogValidationReason.TOO_LONG)),
            (result as AddDogResult.Invalid).errors
        )
        assertTrue(repository.addedDogs.isEmpty())
    }

    @Test
    fun `several dogs with the same name are all allowed`() = runTest {
        useCase(validDog)
        val result = useCase(validDog)

        assertTrue(result is AddDogResult.Success)
        assertEquals(2, repository.addedDogs.size)
    }
}
