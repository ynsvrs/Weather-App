package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.model.GeoLocation
import com.example.weatherapp.data.model.WeatherData
import com.example.weatherapp.data.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI State
sealed class WeatherUiState {
    object Initial : WeatherUiState()
    object Loading : WeatherUiState()
    data class Success(val weatherData: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

sealed class SearchUiState {
    object Initial : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val locations: List<GeoLocation>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application.applicationContext)

    // State flows
    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _temperatureUnit = MutableStateFlow("celsius")
    val temperatureUnit: StateFlow<String> = _temperatureUnit.asStateFlow()

    init {
        // Load cached data on init
        loadCachedData()
        loadSearchHistory()
        loadTemperatureUnit()
    }

    // Load cached weather data
    private fun loadCachedData() {
        viewModelScope.launch {
            repository.getCachedWeatherData().collect { cachedData ->
                if (cachedData != null && _weatherUiState.value is WeatherUiState.Initial) {
                    _weatherUiState.value = WeatherUiState.Success(cachedData.copy(isOffline = true))
                }
            }
        }
    }

    // Load search history
    private fun loadSearchHistory() {
        viewModelScope.launch {
            repository.getSearchHistory().collect { history ->
                _searchHistory.value = history
            }
        }
    }

    // Load temperature unit preference
    private fun loadTemperatureUnit() {
        viewModelScope.launch {
            repository.getTemperatureUnit().collect { unit ->
                _temperatureUnit.value = unit
            }
        }
    }

    // Search for cities
    fun searchCity(cityName: String) {
        if (cityName.isBlank()) {
            _searchUiState.value = SearchUiState.Error("Please enter a city name")
            return
        }

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading

            val result = repository.searchCities(cityName.trim())
            _searchUiState.value = result.fold(
                onSuccess = { locations ->
                    if (locations.isEmpty()) {
                        SearchUiState.Error("No cities found for '$cityName'")
                    } else {
                        SearchUiState.Success(locations)
                    }
                },
                onFailure = { exception ->
                    SearchUiState.Error(exception.message ?: "Failed to search city")
                }
            )
        }
    }

    // Get weather for selected location
    fun getWeather(location: GeoLocation) {
        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading

            val result = repository.getWeatherData(location, _temperatureUnit.value)
            _weatherUiState.value = result.fold(
                onSuccess = { weatherData ->
                    WeatherUiState.Success(weatherData)
                },
                onFailure = { exception ->
                    WeatherUiState.Error(exception.message ?: "Failed to fetch weather")
                }
            )

            // Clear search results after getting weather
            _searchUiState.value = SearchUiState.Initial
        }
    }

    // Quick search from history
    fun searchFromHistory(cityName: String) {
        searchCity(cityName)
    }

    // Set temperature unit and refresh
    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch {
            repository.saveTemperatureUnit(unit)
            _temperatureUnit.value = unit

            // Refresh weather with new unit
            val currentState = _weatherUiState.value
            if (currentState is WeatherUiState.Success) {
                val cityName = currentState.weatherData.cityName
                searchCity(cityName)
            }
        }
    }

    // Reset search state
    fun resetSearchState() {
        _searchUiState.value = SearchUiState.Initial
    }

    // Get temperature unit symbol
    fun getTemperatureSymbol(): String {
        return if (_temperatureUnit.value == "fahrenheit") "°F" else "°C"
    }
}