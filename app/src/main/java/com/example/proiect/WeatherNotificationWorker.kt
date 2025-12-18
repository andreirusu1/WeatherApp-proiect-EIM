package com.example.proiect

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class WeatherNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = androidx.room.Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "weather-db"
            ).build()
            
            val repo = WeatherRepo(db.favoritesDao(), db.userDao())

            // Citim ultima locatie selectata din SharedPreferences
            val sharedPref = applicationContext.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            val lat = sharedPref.getString("last_lat", "44.43") ?: "44.43"
            val lon = sharedPref.getString("last_lon", "26.10") ?: "26.10"
            val cityName = sharedPref.getString("last_city_name", "Bucuresti") ?: "Bucuresti"
            
            val weatherData = repo.fetchWeatherData(lat, lon)

            if (weatherData != null) {
                // Obtinem prognoza pentru urmatoarea ora
                val nextHour = (LocalDateTime.now().hour + 1) % 24
                
                // Cautam in lista orara prognoza care corespunde cu nextHour
                // Lista `hourly` din WeatherData contine string-uri de timp (ex: "12:00")
                // Trebuie sa extragem ora din string pentru a face match
                val forecast = weatherData.hourly.find { 
                    try {
                        val h = it.time.substringBefore(":").toIntOrNull() ?: -1
                        h == nextHour
                    } catch (e: Exception) { false }
                }

                if (forecast != null) {
                    showNotification(forecast.temp, forecast.weatherCode, cityName, nextHour)
                } else {
                    // Fallback la temperatura curenta daca nu gasim prognoza orara
                    showNotification(weatherData.temperature, weatherData.weatherCode, cityName, -1)
                }
                
                return@withContext Result.success()
            } else {
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            return@withContext Result.failure()
        }
    }

    private fun showNotification(temp: Double, code: Int, city: String, hour: Int) {
        val channelId = "weather_channel_01"
        val context = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prognoza Meteo"
            val descriptionText = "Prognoza meteo pentru urmatoarea ora"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val timeText = if (hour != -1) "la ora $hour:00" else "acum"
        
        // Apelam functia direct (e top-level in WeatherUtils.kt), nu WeatherUtils.getWeatherDescription
        val desc = getWeatherDescription(code)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_day)
            .setContentTitle("Vremea in $city $timeText")
            .setContentText("$desc, $temp°C")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(1001, builder.build())
        }
    }
}
