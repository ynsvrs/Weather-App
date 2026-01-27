package com.example.weatherapp.data.model
data class FavoriteCity(
    val id: String = "",
    val cityName: String = "",
    val country: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

fun FavoriteCity.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "cityName" to cityName,
        "country" to country,
        "latitude" to latitude,
        "longitude" to longitude,
        "note" to note,
        "createdAt" to createdAt,
        "createdBy" to createdBy,
        "updatedAt" to updatedAt
    )
}