package com.example.proiect

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeatherRepo(private val favoritesDao: FavoritesDao, private val userDao: UserDao) {
    private val client = OkHttpClient()

    // Cerinta Etapa 1: Networking (REST API) si Procese background
    suspend fun fetchWeatherData(lat: String, lon: String): WeatherData? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m&hourly=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                
                val dailyJson = json.getJSONObject("daily")
                val dTime = dailyJson.getJSONArray("time")
                val dCode = dailyJson.getJSONArray("weather_code")
                val dMax = dailyJson.getJSONArray("temperature_2m_max")
                val dMin = dailyJson.getJSONArray("temperature_2m_min")
                val dailyList = mutableListOf<DailyForecast>()
                
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                
                for (i in 0 until dTime.length()) {
                    val rawDate = dTime.getString(i)
                    val parsedDate = LocalDate.parse(rawDate, inputFormatter)
                    val formattedDate = parsedDate.format(outputFormatter)
                    
                    dailyList.add(DailyForecast(formattedDate, dMax.getDouble(i), dMin.getDouble(i), dCode.getInt(i)))
                }

                val hourlyJson = json.getJSONObject("hourly")
                val hTime = hourlyJson.getJSONArray("time")
                val hTemp = hourlyJson.getJSONArray("temperature_2m")
                val hCode = hourlyJson.getJSONArray("weather_code")
                val hourlyList = mutableListOf<HourlyForecast>()
                
                val now = LocalDateTime.now()
                val currentHour = now.hour
                
                for (i in currentHour until (currentHour + 24).coerceAtMost(hTime.length())) {
                     hourlyList.add(HourlyForecast(
                         time = hTime.getString(i).substringAfter("T"),
                         temp = hTemp.getDouble(i),
                         weatherCode = hCode.getInt(i)
                     ))
                }

                WeatherData(
                    temperature = current.getDouble("temperature_2m"),
                    weatherCode = current.getInt("weather_code"),
                    humidity = current.getInt("relative_humidity_2m"),
                    windSpeed = current.getDouble("wind_speed_10m"),
                    daily = dailyList,
                    hourly = hourlyList
                )
            } else { null }
        } catch (e: Exception) { null }
    }

    // Cerinta Etapa 1: Networking si Background
    suspend fun searchCities(name: String): List<City> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$name&count=5&language=ro&format=json"
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            val list = mutableListOf<City>()
            if (response.isSuccessful && body != null) {
                val results = JSONObject(body).optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val obj = results.getJSONObject(i)
                        val country = obj.optString("country", "")
                        val cityName = obj.getString("name")
                        val displayName = if (country.isNotEmpty()) "$cityName, $country" else cityName
                        
                        // Nota: La search, City nu are ID de user inca, punem 0 temporar
                        list.add(City(
                            id = "${obj.getDouble("latitude")},${obj.getDouble("longitude")}",
                            userId = 0,
                            name = displayName,
                            lat = obj.getDouble("latitude"),
                            lon = obj.getDouble("longitude")
                        ))
                    }
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    // Auth operations
    suspend fun login(username: String, pass: String): User? = userDao.login(username, pass)
    suspend fun register(user: User): Boolean {
        if (userDao.checkUserExists(user.username) != null) return false
        userDao.register(user)
        return true
    }

    // Favorites operations per user
    suspend fun getFavoriteCities(userId: Int): List<City> = favoritesDao.getAllForUser(userId)
    suspend fun addFavoriteCity(city: City) = favoritesDao.add(city)
    suspend fun removeFavoriteCity(cityId: String, userId: Int) = favoritesDao.remove(cityId, userId)
}
