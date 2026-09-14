package com.aristidevs.cursopremiumandroid.domain.model

data class Dog(
    val id: Long,
    val name: String,
    val age: Int,
    val description: String,
    val image: String,
    val breed: String,
    val isOwn: Boolean
)
