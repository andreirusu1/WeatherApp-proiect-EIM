package com.example.proiect

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

    // Cerinta Etapa 1: Stocare persistenta (Baza de Date) folosind Room
    // Initializam baza de date pe contextul aplicatiei
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "weather-db").build()
    private val repo = WeatherRepo(db.favoritesDao())

    // StateFlow pentru a retine datele meteo curente si a le expune catre UI (Compose)
    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    // Orasul selectat curent (default Bucuresti)
    private val _currentCity = MutableStateFlow<City>(City("44.43,26.10", "București", 44.43, 26.10))
    val currentCity: StateFlow<City> = _currentCity.asStateFlow()

    // Lista de favorite incarcata din DB
    private val _favorites = MutableStateFlow<List<City>>(emptyList())
    val favorites: StateFlow<List<City>> = _favorites.asStateFlow()

    // Rezultatele cautarii (pentru search bar)
    private val _searchResults = MutableStateFlow<List<City>>(emptyList())
    val searchResults: StateFlow<List<City>> = _searchResults.asStateFlow()

    init {
        refreshFavorites()
    }

    // Actualizeaza orasul curent si declanseaza descarcarea datelor meteo
    fun updateCity(city: City) {
        _currentCity.value = city
        fetchWeatherForCity(city)
    }

    // Cerinta Etapa 1: Networking si Background Processing
    // Apeleaza repo-ul pentru a lua date de la API pe thread-ul IO
    fun fetchWeatherForCity(city: City) {
        viewModelScope.launch {
            val data = repo.fetchWeatherData(city.lat.toString(), city.lon.toString())
            _weatherData.value = data
        }
    }

    // Cauta orase folosind API-ul, tot asincron
    fun searchCities(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _searchResults.value = repo.searchCities(query)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    // Operatii pe baza de date (Favorite)
    fun addToFavorites(city: City) {
        viewModelScope.launch {
            repo.addFavoriteCity(city)
            refreshFavorites()
        }
    }

    fun removeFromFavorites(city: City) {
        viewModelScope.launch {
            repo.removeFavoriteCity(city)
            refreshFavorites()
        }
    }

    private fun refreshFavorites() {
        viewModelScope.launch {
            _favorites.value = repo.getFavoriteCities()
        }
    }
    
    // Cerinta Etapa 1: Localizare
    // Obtine locatia curenta a telefonului si o transforma in Oras (Geocoding invers)
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
                                    name = address.locality ?: "Locație Curentă",
                                    lat = location.latitude,
                                    lon = location.longitude
                                )
                            }
                        } catch (e: Exception) { }

                        val finalCity = foundCity ?: City("${location.latitude},${location.longitude}", "Locație Curentă", location.latitude, location.longitude)
                        withContext(Dispatchers.Main) {
                            updateCity(finalCity)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted logic handled in Activity
        }
    }
}
