package com.example.weatherapp.data.repository

import com.google.firebase.database.*
import com.example.weatherapp.data.model.FavoriteCity
import com.example.weatherapp.data.model.toMap
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FavoritesRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Get current user ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    // Reference to user's favorites
    private fun getFavoritesRef(): DatabaseReference {
        val userId = getCurrentUserId()
        return database.getReference("favorites").child(userId)
    }

    // Sign in anonymously
    suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: throw Exception("Failed to get user ID")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get current user
    fun getCurrentUser() = auth.currentUser

    // Add favorite city
    suspend fun addFavorite(city: FavoriteCity): Result<String> {
        return try {
            val userId = getCurrentUserId()
            val favoriteId = getFavoritesRef().push().key ?: throw Exception("Failed to generate ID")

            val favoriteWithId = city.copy(
                id = favoriteId,
                createdBy = userId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            getFavoritesRef().child(favoriteId).setValue(favoriteWithId.toMap()).await()
            Result.success(favoriteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update favorite note
    suspend fun updateFavorite(favoriteId: String, note: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "note" to note,
                "updatedAt" to System.currentTimeMillis()
            )
            getFavoritesRef().child(favoriteId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete favorite
    suspend fun deleteFavorite(favoriteId: String): Result<Unit> {
        return try {
            getFavoritesRef().child(favoriteId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all favorites with real-time updates
    fun getFavoritesFlow(): Flow<List<FavoriteCity>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favorites = mutableListOf<FavoriteCity>()

                snapshot.children.forEach { childSnapshot ->
                    try {
                        val favorite = FavoriteCity(
                            id = childSnapshot.child("id").getValue(String::class.java) ?: "",
                            cityName = childSnapshot.child("cityName").getValue(String::class.java) ?: "",
                            country = childSnapshot.child("country").getValue(String::class.java) ?: "",
                            latitude = childSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                            longitude = childSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0,
                            note = childSnapshot.child("note").getValue(String::class.java) ?: "",
                            createdAt = childSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                            createdBy = childSnapshot.child("createdBy").getValue(String::class.java) ?: "",
                            updatedAt = childSnapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
                        )
                        favorites.add(favorite)
                    } catch (e: Exception) {
                        // Skip invalid entries
                    }
                }

                // Sort by most recent first
                trySend(favorites.sortedByDescending { it.updatedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = try {
            getFavoritesRef()
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    // Check if city is already in favorites
    suspend fun isCityFavorited(cityName: String): Boolean {
        return try {
            val snapshot = getFavoritesRef()
                .orderByChild("cityName")
                .equalTo(cityName)
                .get()
                .await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }
}