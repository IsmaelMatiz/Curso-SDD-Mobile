package com.aristidevs.cursopremiumandroid.domain.model

/**
 * Data entered in the add-dog form. [age] is nullable to distinguish an
 * empty field from a value out of range. [photoUri] travels as a String so
 * the domain layer does not depend on `android.net.Uri` (PLAN.md).
 */
data class NewDog(
    val name: String,
    val breed: String,
    val age: Int?,
    val description: String,
    val photoUri: String?,
    val weight: String?,
    val origin: String?,
    val temperament: String?
)
