# Weather App - Android

## Project Overview

A modern Android weather application built with **Kotlin** and **Jetpack Compose** that provides real-time weather information, forecasts, and offline support. The app demonstrates professional Android development practices including MVVM architecture, repository pattern, and modern Compose UI. Extended with Firebase Realtime Database integration, adding a Favorites and Notes feature with real-time synchronization across devices.

## 🌐 API Information

### Weather Data Provider: **Open-Meteo**

- **Website**: https://open-meteo.com/
- **API Key Required**: No (Free, unlimited access)
- **Documentation**: https://open-meteo.com/en/docs

## 🔌 API Endpoints & Parameters

### 1. Geocoding API (City Search)

**Endpoint**: `https://geocoding-api.open-meteo.com/v1/search`

**Purpose**: Convert city names to geographic coordinates

**HTTP Method**: `GET`

**Example Request**:
```
GET https://geocoding-api.open-meteo.com/v1/search?name=London&count=5&language=en&format=json
```

**Example Response**:
```json
{
  "results": [
    {
      "name": "London",
      "latitude": 51.5074,
      "longitude": -0.1278,
      "country": "United Kingdom",
      "admin1": "England"
    }
  ]
}
```
### 2. Weather Forecast API

**Endpoint**: `https://api.open-meteo.com/v1/forecast`

**Purpose**: Retrieve current weather and forecast data

**HTTP Method**: `GET`

**Current Weather Variables**:
- `temperature_2m` - Temperature at 2 meters above ground
- `relative_humidity_2m` - Relative humidity
- `apparent_temperature` - Feels-like temperature
- `precipitation` - Precipitation amount
- `weather_code` - WMO weather code
- `wind_speed_10m` - Wind speed at 10 meters

**Daily Variables**:
- `temperature_2m_max` - Maximum daily temperature
- `temperature_2m_min` - Minimum daily temperature
- `weather_code` - Daily weather condition
- `precipitation_sum` - Total daily precipitation

**Hourly Variables**:
- `temperature_2m` - Hourly temperature
- `weather_code` - Hourly weather condition
- `relative_humidity_2m` - Hourly humidity

**Example Request**:
```
GET https://api.open-meteo.com/v1/forecast?latitude=51.5074&longitude=-0.1278
&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m
&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum
&hourly=temperature_2m,weather_code,relative_humidity_2m
&temperature_unit=celsius&timezone=auto
```

**Example Response**:
```json
{
  "latitude": 51.5,
  "longitude": -0.12,
  "timezone": "Europe/London",
  "current": {
    "time": "2026-01-25T15:00",
    "temperature_2m": 12.5,
    "relative_humidity_2m": 75,
    "apparent_temperature": 10.2,
    "precipitation": 0.0,
    "weather_code": 3,
    "wind_speed_10m": 15.5
  },
  "daily": {
    "time": ["2026-01-25", "2026-01-26", "2026-01-27"],
    "temperature_2m_max": [14.5, 13.2, 15.8],
    "temperature_2m_min": [8.3, 7.9, 9.1],
    "weather_code": [3, 61, 2],
    "precipitation_sum": [0.0, 5.2, 0.5]
  },
  "hourly": {
    "time": ["2026-01-25T00:00", "2026-01-25T01:00"],
    "temperature_2m": [10.5, 10.2],
    "weather_code": [3, 3],
    "relative_humidity_2m": [78, 79]
  }
}
```
---
# Firebase Setup Steps

## Step 1: Create Firebase Project
- Created new project: WeatherApp
- Added Android app with package: com.yourname.weatherapp

## Step 2: Download Configuration

- Downloaded google-services.json
- Placed in app/ directory
- Added to .gitignore to prevent committing secrets

## Step 3: Update Gradle Files
- Project-level build.gradle.kts
- Module-level app/build.gradle.kts

## Step 4: Enable Firebase Services

- Authentication: Enabled Anonymous Auth
- Realtime Database: Created database in us-central1
- Security Rules: Configured 
---

# Authentification
## User Flow:

- App launches
- Checks if user is authenticated
- If not, automatically signs in anonymously
- UID is used for data scoping

# Real-Time Synchronization

### How It Works
- Technology: Firebase Realtime Database ValueEventListener

### Behavior:

- Initial Load: Listener fires immediately with current data
- On Change: Fires whenever data at that path changes
- Child Events: Detects add, modify, remove operations
- Automatic: No manual refresh needed

## Real-Time Features
### Multi-Device Sync:

- Changes on Device A appear on Device B instantly
- Works across emulators/physical devices
- No polling required

### Instant UI Updates:

- StateFlow emits new list
- Compose recomposes automatically
- Smooth animations

### Offline Support:

- Firebase caches data locally
- Works offline (reads cached data)
- Syncs changes when back online

# Security Rules
## Configured Rules

```bash
json{
"rules": {
"favorites": {
"$uid": {
".read": "$uid === auth.uid",
".write": "$uid === auth.uid",
"$favoriteId": {
".validate": "newData.hasChildren(['id', 'cityName', 'country', 'createdAt', 'createdBy'])",
"id": {
".validate": "newData.isString()"
},
"cityName": {
".validate": "newData.isString() && newData.val().length > 0 && newData.val().length <= 100"
},
"country": {
".validate": "newData.isString()"
},
"latitude": {
".validate": "newData.isNumber()"
},
"longitude": {
".validate": "newData.isNumber()"
},
"note": {
".validate": "newData.isString() && newData.val().length <= 500"
},
"createdAt": {
".validate": "newData.isNumber()"
},
"createdBy": {
".validate": "newData.val() === auth.uid"
},
"updatedAt": {
".validate": "newData.isNumber()"
}
}
}
}
}
}
```

## Rule Explanation
### 1. Read Access
```bash
   json".read": "$uid === auth.uid"
```
- Purpose: Users can only read their own favorites
- Security: Prevents users from seeing other users' data
- Test: Try accessing /favorites/other_uid → Denied
### 2. Write Access
```bash
json".write": "$uid === auth.uid"
```
- Purpose: Users can only write to their own node
- Security: Prevents malicious users from modifying others' data
- Test: Try writing to /favorites/other_uid → Denied
### 3. Required Fields Validation
```bash
   json".validate": "newData.hasChildren(['id', 'cityName', 'country', 'createdAt', 'createdBy'])"
```
- Purpose: Ensures complete data structure
- Security: Prevents incomplete/malformed records
- Test: Try adding favorite without cityName → Rejected
### 4. Field-Level Constraints
   City Name Length:
```bash
   json"cityName": {
   ".validate": "newData.isString() && newData.val().length > 0 && newData.val().length <= 100"
   }
```
- Prevents empty names
- Limits to 100 characters
- Prevents database bloat

Note Length:
```bash
json"note": {
".validate": "newData.isString() && newData.val().length <= 500"
}
```
- Prevents excessively long notes
- 500 char limit is reasonable for notes

CreatedBy Validation:
```bash
json"createdBy": {
".validate": "newData.val() === auth.uid"
}
```
- Must match authenticated user
- Prevents impersonation
- Ensures data ownership
### Each File Responsibilities

#### **1. Presentation (UI)**
- **Technology**: Jetpack Compose with Material 3
- **Components**:
    - `WeatherScreen.kt` - Main weather display
    - `SettingsScreen.kt` - User preferences
    - `WeatherComponents.kt` - Reusable UI elements
- **Responsibilities**:
    - Display data from ViewModel
    - Handle user interactions
    - Show loading/error states
    - Navigation between screens

#### **2. ViewModel**
- **File**: `WeatherViewModel.kt`
- **Pattern**: StateFlow for reactive state
- **Responsibilities**:
    - Manage UI state
    - Handle user actions
    - Coordinate data fetching
    - Expose data streams to UI
    - Survive configuration changes

#### **3. Repository**
- **File**: `WeatherRepository.kt`
- **Pattern**: Single source of truth
- **Responsibilities**:
    - Coordinate between network and cache
    - Implement offline-first strategy
    - Handle network availability
    - Transform API data to UI models
    - Manage data freshness

#### **4. Data Source**
- **Network**:
    - `RetrofitInstance.kt` - HTTP client configuration
    - `WeatherApiService.kt` - API interface definitions
- **Local Storage**:
    - `WeatherDataStore.kt` - Persistent storage
- **Models**:
    - `WeatherModels.kt` - Data classes

---
## Caching Approach

### Technology: **Jetpack DataStore (Preferences)**

### Why DataStore?
- Modern replacement for SharedPreferences
-  Type-safe with Kotlin Coroutines
-  Asynchronous by default
-  Better performance
-  Data consistency guarantees

### Caching Strategy: **Write-Through Cache**


### What Gets Cached

1. **Weather Data** (Complete object as JSON):
    - Current conditions
    - 3-day daily forecast
    - 24-hour hourly forecast
    - All metadata (city, country, update time)

2. **Search History**:
    - Last 10 searched cities
    - Most recent searches first
    - Used for quick re-search

3. **User Preferences**:
    - Temperature unit (Celsius/Fahrenheit)
    - Persists across app restarts

4. **Cache Metadata**:
    - Timestamp of last successful fetch
    - Used for cache age determination


## Error Handling Decisions

### 1. **Network Errors**

| Error Type | Detection | User Message | Recovery |
|------------|-----------|--------------|----------|
| **No Internet** | `NetworkCapabilities` check | "No internet connection" | Load cache if available |
| **Timeout** | OkHttp timeout (30s) | "Request timed out. Please try again." | Retry button |
| **Server Error** | HTTP 5xx codes | "Server error. Try again later." | Retry button |
| **API Failure** | HTTP 4xx codes | "Failed to fetch weather" | Retry button |

### 2. **User Input Errors**

| Input | Validation | Message |
|-------|------------|---------|
| **Empty city** | `cityName.isBlank()` | "Please enter a city name" |
| **City not found** | Empty API results | "No cities found for '[city]'" |
| **Invalid characters** | Trim whitespace | Auto-sanitize input |

### 3. **Data Parsing Errors**

- **Gson Exceptions**: Caught and logged, fallback to cache
- **Null Safety**: Kotlin null-safety prevents crashes
- **Default Values**: Safe defaults for missing API fields

## How to Run the App

### Prerequisites

**Required Software**:
-  **Android Studio** Ladybug (2024.1.1) or newer
-  **JDK** 17 or higher
-  **Android SDK** API 24+ (Android 7.0+)
-  **Gradle** 8.2+ (auto-installed by Android Studio)

**System Requirements**:
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: 8GB free space
- **Internet**: Required for initial setup and API calls

### Step-by-Step Instructions

#### **1. Clone the Repository**
```bash
git clone https://github.com/yourusername/WeatherApp.git
cd WeatherApp
```

#### **2. Open in Android Studio**

1. Launch Android Studio
2. Click **File** then **Open**
3. Navigate to the cloned `WeatherApp` folder
4. Click **OK**
5. Wait for Gradle sync to complete (may take 2-5 minutes)

#### **3. Configure Package Name**

#### **4. Sync Gradle**

1. Click **File** → **Sync Project with Gradle Files**
2. Or click the elephant icon 🐘 in the toolbar
3. Wait for sync to complete successfully

#### **5. Run on Emulator**

**Create Emulator** (if you don't have one):
1. Click **Device Manager** 
2. Click **Create Device** 
3. Select **Pixel 5** or **Pixel 6**
4. Click **Next**
5. Select **API 34** (Android 14) - Download if needed
6. Click **Next** then **Finish**

**Run the App**:
1. Select your emulator from the device dropdown (top toolbar)
2. Click the green **Run** button
3. Wait for emulator to start (1-2 minutes first time)
4. App will automatically install and launch

##  Known Limitations

### 1. **Cache Management**
-  **No automatic cache expiration**
    - Cache persists indefinitely until manually refreshed
    - Could show outdated weather if not refreshed
    - **Future**: Add cache timestamp and 1-hour auto-expiration
### 2. **Multiple Cities**
-  **Single city view only**
    - Can only view one city at a time
    - No favorites or saved cities list
    - **Future**: Add favorites with swipe-to-switch

### 3. **Search Limitations**
-  **Offline search disabled**
    - Cannot search for new cities without internet
    - Only cached city is viewable offline
    - **Workaround**: Search cities while online for offline access later
