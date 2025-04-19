package com.example.skycast.data.api

import com.example.skycast.data.entity.WeatherResponse
import retrofit2.http.GET
import com.example.skycast.BuildConfig

import retrofit2.http.Query


interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = BuildConfig.API_KEY,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}