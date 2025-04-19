package com.example.skycast.data.entity


data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val visibility: Int,
    val sys: Sys
)

data class Main(
    val temp: Double,
    val pressure: Double,
    val feels_like: Double,
    val humidity: Int
)

data class Weather(
    val id: Int,
    val description: String,
    val icon: String,
    val main: String
)

data class Wind(
    val speed: Double
)

data class Sys(
    val sunrise: Long,
    val sunset: Long
)