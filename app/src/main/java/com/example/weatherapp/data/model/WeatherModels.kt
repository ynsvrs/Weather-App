package com.example.weatherapp.data.model

import com.google.gson.annotations.SerializedName

// Response from Open-Meteo geocoding API
data class GeocodingResponse(
    val results: List<GeoLocation>?
)

data class GeoLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val admin1: String? = null // State/Province
)

// Response from Open-Meteo weather API
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerializedName("current")
    val current: CurrentWeather,
    @SerializedName("daily")
    val daily: DailyWeather,
    @SerializedName("hourly")
    val hourly: HourlyWeather? = null
)

data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val apparent_temperature: Double,
    val precipitation: Double,
    val weather_code: Int,
    val wind_speed_10m: Double
)

data class DailyWeather(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weather_code: List<Int>,
    val precipitation_sum: List<Double>
)

data class HourlyWeather(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weather_code: List<Int>,
    val relative_humidity_2m: List<Int>
)

// UI Model (what we show in the app)
data class WeatherData(
    val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val lastUpdate: String,
    val forecast: List<DailyForecast>,
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val isOffline: Boolean = false
)

data class DailyForecast(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val condition: String,
    val precipitation: Double
)

data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val condition: String,
    val humidity: Int
)

// Cached data for offline mode
data class CachedWeatherData(
    val weatherData: WeatherData,
    val timestamp: Long
)

// Weather condition mapping from WMO codes
object WeatherCodeMapper {
    fun getConditionFromCode(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }

    fun getWeatherEmoji(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2, 3 -> "⛅"
            45, 48 -> "🌫️"
            51, 53, 55 -> "🌦️"
            61, 63, 65 -> "🌧️"
            66, 67 -> "🌧️"
            71, 73, 75 -> "❄️"
            77 -> "❄️"
            80, 81, 82 -> "🌧️"
            85, 86 -> "🌨️"
            95 -> "⛈️"
            96, 99 -> "⛈️"
            else -> "🌡️"
        }
    }
}