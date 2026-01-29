package com.example.weatherapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapp.data.model.FavoriteCity
import com.example.weatherapp.ui.components.EditNoteDialog
import com.example.weatherapp.ui.components.FavoriteItem
import com.example.weatherapp.ui.components.LoadingIndicator
import com.example.weatherapp.viewmodel.AuthState
import com.example.weatherapp.viewmodel.FavoritesUiState
import com.example.weatherapp.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onCityClick: (String) -> Unit,
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val authState by favoritesViewModel.authState.collectAsStateWithLifecycle()
    val favoritesState by favoritesViewModel.favoritesUiState.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingFavorite by remember { mutableStateOf<FavoriteCity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingFavorite by remember { mutableStateOf<FavoriteCity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Cities") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = authState) {
                is AuthState.Loading -> {
                    LoadingIndicator()
                }
                is AuthState.NotAuthenticated -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Please sign in to view favorites")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { favoritesViewModel.signInAnonymously() }) {
                            Text("Sign In Anonymously")
                        }
                    }
                }
                is AuthState.Authenticated -> {
                    when (val favState = favoritesState) {
                        is FavoritesUiState.Loading -> {
                            LoadingIndicator()
                        }
                        is FavoritesUiState.Success -> {
                            if (favState.favorites.isEmpty()) {
                                EmptyFavoritesView()
                            } else {
                                FavoritesList(
                                    favorites = favState.favorites,
                                    onEdit = { favorite ->
                                        editingFavorite = favorite
                                        showEditDialog = true
                                    },
                                    onDelete = { favorite ->
                                        deletingFavorite = favorite
                                        showDeleteDialog = true
                                    },
                                    onCityClick = onCityClick
                                )
                            }
                        }
                        is FavoritesUiState.Error -> {
                            ErrorView(message = favState.message)
                        }
                    }
                }
                is AuthState.Error -> {
                    ErrorView(message = state.message)
                }
            }
        }
    }

    // Edit note dialog
    if (showEditDialog && editingFavorite != null) {
        EditNoteDialog(
            currentNote = editingFavorite!!.note,
            onDismiss = {
                showEditDialog = false
                editingFavorite = null
            },
            onSave = { newNote ->
                favoritesViewModel.updateNote(editingFavorite!!.id, newNote)
                showEditDialog = false
                editingFavorite = null
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && deletingFavorite != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletingFavorite = null
            },
            title = { Text("Delete Favorite") },
            text = { Text("Remove ${deletingFavorite!!.cityName} from favorites?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        favoritesViewModel.deleteFavorite(deletingFavorite!!.id)
                        showDeleteDialog = false
                        deletingFavorite = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletingFavorite = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FavoritesList(
    favorites: List<FavoriteCity>,
    onEdit: (FavoriteCity) -> Unit,
    onDelete: (FavoriteCity) -> Unit,
    onCityClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favorites, key = { it.id }) { favorite ->
            FavoriteItem(
                favorite = favorite,
                onEdit = { onEdit(favorite) },
                onDelete = { onDelete(favorite) },
                onClick = { onCityClick(favorite.cityName) }
            )
        }
    }
}

@Composable
fun EmptyFavoritesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⭐",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Favorites Yet",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add cities to favorites from the weather screen",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
    }
}