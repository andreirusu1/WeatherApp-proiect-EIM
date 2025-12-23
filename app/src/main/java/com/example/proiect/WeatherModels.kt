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

// --- DATABASE ENTITIES ---

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val username: String,
    val password: String
)

@Entity(tableName = "favorites", primaryKeys = ["id", "userId"])
data class City(
    val id: String, // lat,lon
    val userId: Int, // Foreign Key catre User
    val name: String, 
    val lat: Double, 
    val lon: Double
)

// --- DAOs ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :user AND password = :pass LIMIT 1")
    suspend fun login(user: String, pass: String): User?

    @Query("SELECT * FROM users WHERE username = :user LIMIT 1")
    suspend fun checkUserExists(user: String): User?

    @Insert
    suspend fun register(user: User): Long
}

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites WHERE userId = :uid") 
    suspend fun getAllForUser(uid: Int): List<City>

    @Insert(onConflict = OnConflictStrategy.REPLACE) 
    suspend fun add(city: City)

    @Query("DELETE FROM favorites WHERE id = :cityId AND userId = :uid") 
    suspend fun remove(cityId: String, uid: Int)
}

@Database(entities = [City::class, User::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun userDao(): UserDao
}
