package com.example.weatherapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.weatherapp.data.local.WeatherDataStore
import com.example.weatherapp.data.model.*
import com.example.weatherapp.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(private val context: Context) {

    private val weatherDataStore = WeatherDataStore(context)
    private val geocodingApi = RetrofitInstance.geocodingApi
    private val weatherApi = RetrofitInstance.weatherApi

    // Check if device has internet connection
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Search for cities by name
    suspend fun searchCities(cityName: String): Result<List<GeoLocation>> {
        return try {
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("No internet connection"))
            }

            val response = geocodingApi.searchCity(cityName)
            if (response.isSuccessful) {
                val locations = response.body()?.results ?: emptyList()
                if (locations.isEmpty()) {
                    Result.failure(Exception("City not found"))
                } else {
                    Result.success(locations)
                }
            } else {
                Result.failure(Exception("Failed to search city: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get weather data for a location
    suspend fun getWeatherData(
        location: GeoLocation,
        temperatureUnit: String = "celsius"
    ): Result<WeatherData> {
        return try {
            if (!isNetworkAvailable()) {
                // Try to load from cache
                val cachedData = weatherDataStore.getCachedWeatherData().first()
                return if (cachedData != null) {
                    Result.success(cachedData.copy(isOffline = true))
                } else {
                    Result.failure(Exception("No internet connection and no cached data"))
                }
            }

            // Map unit to API parameter
            val apiUnit = if (temperatureUnit == "fahrenheit") "fahrenheit" else "celsius"

            val response = weatherApi.getWeather(
                latitude = location.latitude,
                longitude = location.longitude,
                temperatureUnit = apiUnit
            )

            if (response.isSuccessful) {
                val weatherResponse = response.body()
                if (weatherResponse != null) {
                    val weatherData = mapToWeatherData(weatherResponse, location)

                    // Save to cache
                    weatherDataStore.saveWeatherData(weatherData)
                    weatherDataStore.addToSearchHistory(location.name)

                    Result.success(weatherData)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("Failed to fetch weather: ${response.code()}"))
            }
        } catch (e: Exception) {
            // On error, try to load from cache
            val cachedData = weatherDataStore.getCachedWeatherData().first()
            if (cachedData != null) {
                Result.success(cachedData.copy(isOffline = true))
            } else {
                Result.failure(e)
            }
        }
    }

    // Map API response to UI model
    private fun mapToWeatherData(
        response: WeatherResponse,
        location: GeoLocation
    ): WeatherData {
        val current = response.current
        val daily = response.daily
        val hourly = response.hourly

        // Map daily forecast (next 3 days)
        val forecast = daily.time.take(3).mapIndexed { index, date ->
            DailyForecast(
                date = formatDate(date),
                maxTemp = daily.temperature_2m_max[index],
                minTemp = daily.temperature_2m_min[index],
                condition = WeatherCodeMapper.getConditionFromCode(daily.weather_code[index]),
                precipitation = daily.precipitation_sum[index]
            )
        }

        // Map hourly forecast (next 24 hours)
        val hourlyForecastList = hourly?.let {
            it.time.take(24).mapIndexed { index, time ->
                HourlyForecast(
                    time = formatHourTime(time),
                    temperature = it.temperature_2m[index],
                    condition = WeatherCodeMapper.getConditionFromCode(it.weather_code[index]),
                    humidity = it.relative_humidity_2m[index]
                )
            }
        } ?: emptyList()

        return WeatherData(
            cityName = location.name,
            country = location.country,
            temperature = current.temperature_2m,
            feelsLike = current.apparent_temperature,
            condition = WeatherCodeMapper.getConditionFromCode(current.weather_code),
            humidity = current.relative_humidity_2m,
            windSpeed = current.wind_speed_10m,
            minTemp = daily.temperature_2m_min.firstOrNull() ?: current.temperature_2m,
            maxTemp = daily.temperature_2m_max.firstOrNull() ?: current.temperature_2m,
            lastUpdate = formatDateTime(current.time),
            forecast = forecast,
            hourlyForecast = hourlyForecastList,
            isOffline = false
        )
    }

    // Format date string
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    // Format date-time string
    private fun formatDateTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            date?.let { outputFormat.format(it) } ?: dateTimeString
        } catch (e: Exception) {
            dateTimeString
        }
    }

    // Format hour time
    private fun formatHourTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            date?.let { outputFormat.format(it) } ?: dateTimeString
        } catch (e: Exception) {
            dateTimeString
        }
    }

    // Get cached weather data
    fun getCachedWeatherData(): Flow<WeatherData?> {
        return weatherDataStore.getCachedWeatherData()
    }

    // Get search history
    fun getSearchHistory(): Flow<List<String>> {
        return weatherDataStore.getSearchHistory()
    }

    // Save temperature unit
    suspend fun saveTemperatureUnit(unit: String) {
        weatherDataStore.saveTemperatureUnit(unit)
    }

    // Get temperature unit
    fun getTemperatureUnit(): Flow<String> {
        return weatherDataStore.getTemperatureUnit()
    }
}