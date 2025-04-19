package com.example.skycast.utils


sealed class Resource<out T> {
    data class Success<out T>(val data: T?) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
    object Loading : Resource<Nothing>()
}