package com.example.weatherapp.data.local


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.example.weatherapp.data.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_prefs")

class WeatherDataStore(private val context: Context) {

    companion object {
        private val CACHED_WEATHER_KEY = stringPreferencesKey("cached_weather")
        private val CACHE_TIMESTAMP_KEY = longPreferencesKey("cache_timestamp")
        private val TEMPERATURE_UNIT_KEY = stringPreferencesKey("temperature_unit")
        private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")
    }

    private val gson = Gson()
    suspend fun saveWeatherData(weatherData: WeatherData) {
        context.dataStore.edit { preferences ->
            val jsonData = gson.toJson(weatherData)
            preferences[CACHED_WEATHER_KEY] = jsonData
            preferences[CACHE_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }

    fun getCachedWeatherData(): Flow<WeatherData?> {
        return context.dataStore.data.map { preferences ->
            val jsonData = preferences[CACHED_WEATHER_KEY]
            if (jsonData != null) {
                try {
                    gson.fromJson(jsonData, WeatherData::class.java)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    fun getCacheTimestamp(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[CACHE_TIMESTAMP_KEY] ?: 0L
        }
    }

    suspend fun saveTemperatureUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE_UNIT_KEY] = unit
        }
    }

    // Get temperature unit preference
    fun getTemperatureUnit(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[TEMPERATURE_UNIT_KEY] ?: "celsius"
        }
    }

    // Save search history
    suspend fun addToSearchHistory(cityName: String) {
        context.dataStore.edit { preferences ->
            val currentHistory = preferences[SEARCH_HISTORY_KEY] ?: ""
            val historyList = if (currentHistory.isNotEmpty()) {
                currentHistory.split(",").toMutableList()
            } else {
                mutableListOf()
            }

            // Remove if already exists and add to front
            historyList.remove(cityName)
            historyList.add(0, cityName)

            // Keep only last 10 searches
            val limitedHistory = historyList.take(10)
            preferences[SEARCH_HISTORY_KEY] = limitedHistory.joinToString(",")
        }
    }

    // Get search history
    fun getSearchHistory(): Flow<List<String>> {
        return context.dataStore.data.map { preferences ->
            val historyString = preferences[SEARCH_HISTORY_KEY] ?: ""
            if (historyString.isNotEmpty()) {
                historyString.split(",")
            } else {
                emptyList()
            }
        }
    }

    // Clear all cache
    suspend fun clearCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(CACHED_WEATHER_KEY)
            preferences.remove(CACHE_TIMESTAMP_KEY)
        }
    }
}