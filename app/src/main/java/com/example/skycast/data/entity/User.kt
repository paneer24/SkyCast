package com.example.skycast.data.entity



data class User(
    val uid: String = "",
    val name: String? = "",
    val email: String? = "",
    val profilePictureUrl: String? = "",
    val createdAt: Long = System.currentTimeMillis()
)