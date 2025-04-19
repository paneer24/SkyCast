package com.example.skycast.ui.Viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skycast.data.entity.WeatherResponse
import com.example.skycast.data.repository.WeatherRepo
import com.example.skycast.utils.Resource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepo) : ViewModel() {
    private val _weatherState = MutableStateFlow<Resource<WeatherResponse>>(Resource.Loading)
    val weatherState: StateFlow<Resource<WeatherResponse>> = _weatherState

    private val _cityName = MutableStateFlow("Delhi")
    val cityName: StateFlow<String> = _cityName

    init {
        fetchWeather()
    }

    fun fetchWeather() {
        viewModelScope.launch {
            repository.getWeather(_cityName.value).collect { result ->
                _weatherState.value = result
            }
        }
    }

    fun updateCity(newCity: String) {
        _cityName.value = newCity
        fetchWeather()
    }
}
