package com.example.weatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.model.FavoriteCity
import com.example.weatherapp.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class FavoritesUiState {
    object Loading : FavoritesUiState()
    data class Success(val favorites: List<FavoriteCity>) : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val uid: String) : AuthState()
    object NotAuthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class FavoritesViewModel : ViewModel() {

    private val repository = FavoritesRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _favoritesUiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val favoritesUiState: StateFlow<FavoritesUiState> = _favoritesUiState.asStateFlow()

    init {
        checkAuthAndLoadFavorites()
    }

    private fun checkAuthAndLoadFavorites() {
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            if (currentUser != null) {
                _authState.value = AuthState.Authenticated(currentUser.uid)
                observeFavorites()
            } else {
                signInAnonymously()
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = repository.signInAnonymously()
            _authState.value = result.fold(
                onSuccess = { uid ->
                    observeFavorites()
                    AuthState.Authenticated(uid)
                },
                onFailure = { exception ->
                    AuthState.Error(exception.message ?: "Authentication failed")
                }
            )
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoritesFlow()
                .catch { exception ->
                    _favoritesUiState.value = FavoritesUiState.Error(
                        exception.message ?: "Failed to load favorites"
                    )
                }
                .collect { favorites ->
                    _favoritesUiState.value = FavoritesUiState.Success(favorites)
                }
        }
    }

    fun addFavorite(favorite: FavoriteCity) {
        viewModelScope.launch {
            repository.addFavorite(favorite).fold(
                onSuccess = {
                    // Success - real-time listener will update UI
                },
                onFailure = { exception ->
                    _favoritesUiState.value = FavoritesUiState.Error(
                        exception.message ?: "Failed to add favorite"
                    )
                }
            )
        }
    }

    fun updateNote(favoriteId: String, note: String) {
        viewModelScope.launch {
            repository.updateFavorite(favoriteId, note)
        }
    }

    fun deleteFavorite(favoriteId: String) {
        viewModelScope.launch {
            repository.deleteFavorite(favoriteId)
        }
    }

    suspend fun isCityFavorited(cityName: String): Boolean {
        return repository.isCityFavorited(cityName)
    }
}