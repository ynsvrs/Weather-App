package com.example.weatherapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weatherapp.data.model.GeoLocation
import com.example.weatherapp.ui.components.*
import com.example.weatherapp.viewmodel.SearchUiState
import com.example.weatherapp.viewmodel.WeatherUiState
import com.example.weatherapp.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onNavigateToSettings: () -> Unit
) {
    val weatherUiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val temperatureUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }

    val temperatureSymbol = viewModel.getTemperatureSymbol()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather App") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSearchDialog = true }
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search city")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = weatherUiState) {
                is WeatherUiState.Initial -> {
                    InitialScreen(
                        searchHistory = searchHistory,
                        onSearchHistoryClick = { city ->
                            viewModel.searchFromHistory(city)
                        },
                        onSearchClick = { showSearchDialog = true }
                    )
                }
                is WeatherUiState.Loading -> {
                    LoadingIndicator()
                }
                is WeatherUiState.Success -> {
                    WeatherContent(
                        weatherData = state.weatherData,
                        temperatureSymbol = temperatureSymbol,
                        onRefresh = {
                            viewModel.searchCity(state.weatherData.cityName)
                        }
                    )
                }
                is WeatherUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { showSearchDialog = true }
                    )
                }
            }
        }
    }

    // Search Dialog
    if (showSearchDialog) {
        SearchDialog(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearch = {
                if (searchQuery.isNotBlank()) {
                    viewModel.searchCity(searchQuery)
                }
            },
            onDismiss = {
                showSearchDialog = false
                searchQuery = ""
                viewModel.resetSearchState()
            },
            searchUiState = searchUiState,
            onLocationClick = { location ->
                viewModel.getWeather(location)
                showSearchDialog = false
                searchQuery = ""
            },
            searchHistory = searchHistory,
            onHistoryClick = { city ->
                searchQuery = city
                viewModel.searchCity(city)
            }
        )
    }
}

@Composable
fun InitialScreen(
    searchHistory: List<String>,
    onSearchHistoryClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Weather App",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search for a city to see weather",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search City")
        }

        if (searchHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchHistory.take(5)) { city ->
                    SearchHistoryChip(
                        cityName = city,
                        onClick = { onSearchHistoryClick(city) }
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherContent(
    weatherData: com.example.weatherapp.data.model.WeatherData,
    temperatureSymbol: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main weather card
        WeatherCard(
            cityName = weatherData.cityName,
            country = weatherData.country,
            temperature = weatherData.temperature,
            condition = weatherData.condition,
            isOffline = weatherData.isOffline,
            temperatureSymbol = temperatureSymbol
        )

        // Weather details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeatherDetailItem(
                        icon = Icons.Default.Thermostat,
                        label = "Feels like",
                        value = "${weatherData.feelsLike.toInt()}$temperatureSymbol"
                    )
                    WeatherDetailItem(
                        icon = Icons.Default.WaterDrop,
                        label = "Humidity",
                        value = "${weatherData.humidity}%"
                    )
                    WeatherDetailItem(
                        icon = Icons.Default.Air,
                        label = "Wind",
                        value = "${weatherData.windSpeed.toInt()} km/h"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeatherDetailItem(
                        icon = Icons.Default.ArrowUpward,
                        label = "Max",
                        value = "${weatherData.maxTemp.toInt()}$temperatureSymbol"
                    )
                    WeatherDetailItem(
                        icon = Icons.Default.ArrowDownward,
                        label = "Min",
                        value = "${weatherData.minTemp.toInt()}$temperatureSymbol"
                    )
                    WeatherDetailItem(
                        icon = Icons.Default.Update,
                        label = "Updated",
                        value = weatherData.lastUpdate.split(",").lastOrNull() ?: "N/A"
                    )
                }
            }
        }

        // 3-day forecast
        if (weatherData.forecast.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3-Day Forecast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(weatherData.forecast) { forecast ->
                            DailyForecastCard(
                                forecast = forecast,
                                temperatureSymbol = temperatureSymbol
                            )
                        }
                    }
                }
            }
        }

        // 24-hour forecast
        if (weatherData.hourlyForecast.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "24-Hour Forecast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(weatherData.hourlyForecast) { forecast ->
                            HourlyForecastItem(
                                forecast = forecast,
                                temperatureSymbol = temperatureSymbol
                            )
                        }
                    }
                }
            }
        }

        // Refresh button
        if (!weatherData.isOffline) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Weather")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    searchUiState: SearchUiState,
    onLocationClick: (GeoLocation) -> Unit,
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search City") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearch = onSearch
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = searchUiState) {
                    is SearchUiState.Initial -> {
                        if (searchHistory.isNotEmpty()) {
                            Text(
                                text = "Recent Searches",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                items(searchHistory) { city ->
                                    SearchHistoryChip(
                                        cityName = city,
                                        onClick = { onHistoryClick(city) }
                                    )
                                }
                            }
                        }
                    }
                    is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is SearchUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(state.locations) { location ->
                                LocationItem(
                                    location = location,
                                    onClick = { onLocationClick(location) }
                                )
                            }
                        }
                    }
                    is SearchUiState.Error -> {
                        ErrorMessage(message = state.message)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSearch) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}