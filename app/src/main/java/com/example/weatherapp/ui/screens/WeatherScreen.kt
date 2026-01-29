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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapp.data.model.FavoriteCity
import com.example.weatherapp.data.model.GeoLocation
import com.example.weatherapp.ui.components.*
import com.example.weatherapp.viewmodel.FavoritesViewModel
import com.example.weatherapp.viewmodel.SearchUiState
import com.example.weatherapp.viewmodel.WeatherUiState
import com.example.weatherapp.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val weatherUiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val temperatureUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()

    val favoritesViewModel: FavoritesViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GeoLocation?>(null) }

    val temperatureSymbol = viewModel.getTemperatureSymbol()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather App") },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Star, contentDescription = "Favorites")
                    }
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
                        },
                        onAddToFavorites = {
                            // Store location for adding to favorites
                            scope.launch {
                                val location = GeoLocation(
                                    name = state.weatherData.cityName,
                                    latitude = 0.0, // We don't store this, will search again
                                    longitude = 0.0,
                                    country = state.weatherData.country
                                )
                                currentLocation = location
                                showAddNoteDialog = true
                            }
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
                currentLocation = location
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

    // Add to Favorites Dialog
    if (showAddNoteDialog && currentLocation != null) {
        AddToFavoritesDialog(
            cityName = currentLocation!!.name,
            onDismiss = {
                showAddNoteDialog = false
                currentLocation = null
            },
            onAdd = { note ->
                scope.launch {
                    val favorite = FavoriteCity(
                        cityName = currentLocation!!.name,
                        country = currentLocation!!.country,
                        latitude = currentLocation!!.latitude,
                        longitude = currentLocation!!.longitude,
                        note = note
                    )
                    favoritesViewModel.addFavorite(favorite)
                    showAddNoteDialog = false
                    currentLocation = null
                }
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
    onRefresh: () -> Unit,
    onAddToFavorites: () -> Unit
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

        // Add to Favorites Button
        Button(
            onClick = onAddToFavorites,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to Favorites")
        }

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
            OutlinedButton(
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

@Composable
fun AddToFavoritesDialog(
    cityName: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Favorites") },
        text = {
            Column {
                Text("Add $cityName to your favorites?")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g., Best in summer, Bring umbrella") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(note) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}