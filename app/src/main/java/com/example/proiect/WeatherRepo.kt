package com.example.proiect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeatherRepo(
    private val client: OkHttpClient = OkHttpClient(),
    private val weatherBaseUrl: String = "https://api.open-meteo.com/v1/",
    private val geocodingBaseUrl: String = "https://geocoding-api.open-meteo.com/v1/",
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val gson = Gson()

    // modele interne pentru gson (dtos)
    private data class WeatherResponse(
        val current: CurrentWeather,
        val hourly: HourlyWeather,
        val daily: DailyWeather
    )

    private data class CurrentWeather(
        @SerializedName("temperature_2m") val temperature: Double,
        @SerializedName("weather_code") val weatherCode: Int,
        @SerializedName("relative_humidity_2m") val humidity: Int,
        @SerializedName("wind_speed_10m") val windSpeed: Double
    )

    private data class HourlyWeather(
        val time: List<String>,
        @SerializedName("temperature_2m") val temperature: List<Double>,
        @SerializedName("weather_code") val weatherCode: List<Int>
    )

    private data class DailyWeather(
        val time: List<String>,
        @SerializedName("weather_code") val weatherCode: List<Int>,
        @SerializedName("temperature_2m_max") val maxTemp: List<Double>,
        @SerializedName("temperature_2m_min") val minTemp: List<Double>
    )

    private data class GeocodingResponse(
        val results: List<GeocodingResult>?
    )

    private data class GeocodingResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val country: String?
    )

    // cerinta etapa 1: networking (rest api) si procese background
    suspend fun fetchWeatherData(lat: String, lon: String): WeatherData? = withContext(Dispatchers.IO) {
        val url = "${weatherBaseUrl}forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m&hourly=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && body != null) {
                val weatherResponse = gson.fromJson(body, WeatherResponse::class.java)
                
                // procesare daily
                val dailyList = mutableListOf<DailyForecast>()
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                
                weatherResponse.daily.time.forEachIndexed { i, rawDate ->
                    val parsedDate = LocalDate.parse(rawDate, inputFormatter)
                    val formattedDate = parsedDate.format(outputFormatter)
                    dailyList.add(
                        DailyForecast(
                            date = formattedDate,
                            maxTemp = weatherResponse.daily.maxTemp[i],
                            minTemp = weatherResponse.daily.minTemp[i],
                            weatherCode = weatherResponse.daily.weatherCode[i]
                        )
                    )
                }

                // procesare hourly
                val hourlyList = mutableListOf<HourlyForecast>()
                val now = LocalDateTime.now()
                val currentHour = now.hour
                val hTime = weatherResponse.hourly.time
                
                val limit = (currentHour + 24).coerceAtMost(hTime.size)
                for (i in currentHour until limit) {
                     hourlyList.add(HourlyForecast(
                         time = hTime[i].substringAfter("T"),
                         temp = weatherResponse.hourly.temperature[i],
                         weatherCode = weatherResponse.hourly.weatherCode[i]
                     ))
                }

                WeatherData(
                    temperature = weatherResponse.current.temperature,
                    weatherCode = weatherResponse.current.weatherCode,
                    humidity = weatherResponse.current.humidity,
                    windSpeed = weatherResponse.current.windSpeed,
                    daily = dailyList,
                    hourly = hourlyList
                )
            } else { null }
        } catch (e: Exception) { null }
    }

    // cerinta etapa 1: networking si background
    suspend fun searchCities(name: String): List<City> = withContext(Dispatchers.IO) {
        val url = "${geocodingBaseUrl}search?name=$name&count=5&language=ro&format=json"
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            val list = mutableListOf<City>()
            
            if (response.isSuccessful && body != null) {
                val geoResponse = gson.fromJson(body, GeocodingResponse::class.java)
                geoResponse.results?.forEach { obj ->
                    val displayName = if (!obj.country.isNullOrEmpty()) "${obj.name}, ${obj.country}" else obj.name
                    list.add(City(
                        id = "${obj.latitude},${obj.longitude}",
                        userId = 0, // nu mai folosim int in firebase, dar pastram modelul
                        name = displayName,
                        lat = obj.latitude,
                        lon = obj.longitude
                    ))
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    // auth operations cu firebase
    suspend fun login(email: String, pass: String): User? = withContext(Dispatchers.IO) {
        try {
            val fakeEmail = if(email.contains("@")) email else "$email@weatherapp.com"
            
            auth.signInWithEmailAndPassword(fakeEmail, pass).await()
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                User(uid = 1, username = firebaseUser.email?.substringBefore("@") ?: "User", password = "")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun register(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val fakeEmail = if(user.username.contains("@")) user.username else "${user.username}@weatherapp.com"
            auth.createUserWithEmailAndPassword(fakeEmail, user.password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // favorites operations cu firestore
    suspend fun getFavoriteCities(ignoredUserId: Int): List<City> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext emptyList()
        try {
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("favorites")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: ""
                val lat = doc.getDouble("lat") ?: 0.0
                val lon = doc.getDouble("lon") ?: 0.0
                val id = doc.getString("id") ?: "${lat},${lon}"
                City(id, 0, name, lat, lon)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addFavoriteCity(city: City) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        try {
            val cityMap = hashMapOf(
                "id" to city.id,
                "name" to city.name,
                "lat" to city.lat,
                "lon" to city.lon
            )
            db.collection("users")
                .document(user.uid)
                .collection("favorites")
                .document(city.id)
                .set(cityMap)
                .await()
        } catch (e: Exception) { }
    }

    suspend fun removeFavoriteCity(cityId: String, ignoredUserId: Int) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        try {
            db.collection("users")
                .document(user.uid)
                .collection("favorites")
                .document(cityId)
                .delete()
                .await()
        } catch (e: Exception) { }
    }
}
