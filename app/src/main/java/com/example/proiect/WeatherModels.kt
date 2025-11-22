package com.example.proiect

import androidx.room.*

data class DailyForecast(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int
)

data class HourlyForecast(
    val time: String,
    val temp: Double,
    val weatherCode: Int
)

data class WeatherData(
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val daily: List<DailyForecast>,
    val hourly: List<HourlyForecast>
)

@Entity(tableName = "favorites")
data class City(@PrimaryKey val id: String, val name: String, val lat: Double, val lon: Double)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites") suspend fun getAll(): List<City>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(city: City)
    @Query("DELETE FROM favorites WHERE id = :cityId") suspend fun remove(cityId: String)
}

@Database(entities = [City::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}
