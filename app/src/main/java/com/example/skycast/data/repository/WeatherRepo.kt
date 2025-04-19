package com.example.skycast.data.repository

import com.example.skycast.data.api.WeatherApi
import com.example.skycast.data.entity.WeatherResponse

import com.example.skycast.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WeatherRepo(private val weatherApi: WeatherApi) {
    fun getWeather(cityName: String): Flow<Resource<WeatherResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = weatherApi.getWeather(cityName)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }
}