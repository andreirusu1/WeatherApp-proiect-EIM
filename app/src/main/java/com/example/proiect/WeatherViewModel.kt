package com.example.proiect

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    // Cerinta Etapa 1 & 2: Stocare persistenta (Room) cu suport Multi-User
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "weather-db")
        .fallbackToDestructiveMigration() // Reset DB la upgrade de versiune pt simplitate
        .build()
    
    private val repo = WeatherRepo(db.favoritesDao(), db.userDao())

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _currentCity = MutableStateFlow<City>(City("44.43,26.10", 0, "București", 44.43, 26.10))
    val currentCity: StateFlow<City> = _currentCity.asStateFlow()

    private val _favorites = MutableStateFlow<List<City>>(emptyList())
    val favorites: StateFlow<List<City>> = _favorites.asStateFlow()

    private val _searchResults = MutableStateFlow<List<City>>(emptyList())
    val searchResults: StateFlow<List<City>> = _searchResults.asStateFlow()

    // User State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Auth methods
    fun login(username: String, pass: String) {
        viewModelScope.launch {
            val user = repo.login(username, pass)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                refreshFavorites() // Incarca favoritele userului
            } else {
                _loginError.value = "Utilizator sau parolă incorectă"
            }
        }
    }

    fun register(username: String, pass: String) {
        viewModelScope.launch {
            if (username.length < 3 || pass.length < 3) {
                _loginError.value = "Minim 3 caractere!"
                return@launch
            }
            val success = repo.register(User(username = username, password = pass))
            if (success) {
                login(username, pass) // Auto-login dupa register
            } else {
                _loginError.value = "Utilizatorul există deja!"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _favorites.value = emptyList()
    }

    fun updateCity(city: City) {
        _currentCity.value = city
        fetchWeatherForCity(city)
    }

    fun fetchWeatherForCity(city: City) {
        viewModelScope.launch {
            val data = repo.fetchWeatherData(city.lat.toString(), city.lon.toString())
            _weatherData.value = data
        }
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _searchResults.value = repo.searchCities(query)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun addToFavorites(city: City) {
        val user = _currentUser.value ?: return
        // Cream o copie a orasului asociata userului curent
        val userCity = city.copy(userId = user.uid)
        
        viewModelScope.launch {
            repo.addFavoriteCity(userCity)
            refreshFavorites()
        }
    }

    fun removeFromFavorites(city: City) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repo.removeFavoriteCity(city.id, user.uid)
            refreshFavorites()
        }
    }

    private fun refreshFavorites() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _favorites.value = repo.getFavoriteCities(user.uid)
        }
    }
    
    fun fetchUserLocation(fusedLocationClient: FusedLocationProviderClient) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val geocoder = Geocoder(getApplication(), Locale.getDefault())
                        var foundCity: City? = null
                        try {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                foundCity = City(
                                    id = "${location.latitude},${location.longitude}",
                                    userId = 0,
                                    name = address.locality ?: "Locație Curentă",
                                    lat = location.latitude,
                                    lon = location.longitude
                                )
                            }
                        } catch (e: Exception) { }

                        val finalCity = foundCity ?: City("${location.latitude},${location.longitude}", 0, "Locație Curentă", location.latitude, location.longitude)
                        withContext(Dispatchers.Main) {
                            updateCity(finalCity)
                        }
                    }
                }
            }
        } catch (e: SecurityException) { }
    }
}
