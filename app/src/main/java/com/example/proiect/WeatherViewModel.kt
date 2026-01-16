package com.example.proiect

import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    // cerinta etapa 2: networking complex (firebase)
    // cerinta etapa 2: extinderea functionalitatii (multi-user)
    private val repo = WeatherRepo()

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _currentCity = MutableStateFlow<City>(City("44.43,26.10", 0, "Bucuresti", 44.43, 26.10))
    val currentCity: StateFlow<City> = _currentCity.asStateFlow()

    private val _favorites = MutableStateFlow<List<City>>(emptyList())
    val favorites: StateFlow<List<City>> = _favorites.asStateFlow()

    private val _searchResults = MutableStateFlow<List<City>>(emptyList())
    val searchResults: StateFlow<List<City>> = _searchResults.asStateFlow()

    // user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- preferinte unitati ---
    // true = imperial (fahrenheit / mph), false = metric (celsius / km/h)
    private val _isImperialTemp = MutableStateFlow(false)
    val isImperialTemp: StateFlow<Boolean> = _isImperialTemp.asStateFlow()

    private val _isImperialWind = MutableStateFlow(false)
    val isImperialWind: StateFlow<Boolean> = _isImperialWind.asStateFlow()

    init {
        loadPreferences()
        loadLastLocation()
    }

    private fun loadPreferences() {
        val sharedPref = getApplication<Application>().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        _isImperialTemp.value = sharedPref.getBoolean("imperial_temp", false)
        _isImperialWind.value = sharedPref.getBoolean("imperial_wind", false)
    }
    
    // cerinta etapa 2: stocare + serializare (shared preferences)
    private fun loadLastLocation() {
        val sharedPref = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val latStr = sharedPref.getString("last_lat", null)
        val lonStr = sharedPref.getString("last_lon", null)
        val name = sharedPref.getString("last_city_name", null)

        if (latStr != null && lonStr != null && name != null) {
            val lat = latStr.toDoubleOrNull() ?: 44.43
            val lon = lonStr.toDoubleOrNull() ?: 26.10
            val city = City(
                id = "${lat},${lon}",
                userId = 0,
                name = name,
                lat = lat,
                lon = lon
            )
            _currentCity.value = city
        }
    }

    fun setTempUnit(isImperial: Boolean) {
        _isImperialTemp.value = isImperial
        val sharedPref = getApplication<Application>().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("imperial_temp", isImperial)
            apply()
        }
    }

    fun setWindUnit(isImperial: Boolean) {
        _isImperialWind.value = isImperial
        val sharedPref = getApplication<Application>().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("imperial_wind", isImperial)
            apply()
        }
    }
    // -------------------------

    // auth methods
    fun login(username: String, pass: String) {
        viewModelScope.launch {
            val user = repo.login(username, pass)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                refreshFavorites() // incarca favoritele userului
            } else {
                _loginError.value = "Utilizator sau parola incorecta"
            }
        }
    }

    fun register(username: String, pass: String) {
        viewModelScope.launch {
            // firebase impune minim 6 caractere pentru parola
            if (username.length < 3) {
                _loginError.value = "Utilizator: minim 3 caractere!"
                return@launch
            }
            if (pass.length < 6) {
                _loginError.value = "Parola: minim 6 caractere!"
                return@launch
            }
            // in firebase registration nu face login automat decat daca e specificat, dar aici am pus manual
            val success = repo.register(User(username = username, password = pass))
            if (success) {
                login(username, pass) // auto-login dupa register
            } else {
                _loginError.value = "Eroare la inregistrare (email deja folosit?)"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _favorites.value = emptyList()
    }

    fun updateCity(city: City) {
        _currentCity.value = city
        saveLastLocation(city) // salvam locatia pentru worker
        fetchWeatherForCity(city)
    }
    
    // cerinta etapa 2: stocare + serializare (shared preferences)
    private fun saveLastLocation(city: City) {
        val sharedPref = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("last_lat", city.lat.toString())
            putString("last_lon", city.lon.toString())
            putString("last_city_name", city.name)
            apply()
        }
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
        // cream o copie a orasului asociata userului curent
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
            // folosim getcurrentlocation cu prioritate mare in loc de lastlocation
            // pentru ca lastlocation poate fi null pe emulatoare sau la prima rulare
            val cancellationToken = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                .addOnSuccessListener { location ->
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
                                        name = address.locality ?: "Locatie Curenta",
                                        lat = location.latitude,
                                        lon = location.longitude
                                    )
                                }
                            } catch (e: Exception) { }

                            val finalCity = foundCity ?: City("${location.latitude},${location.longitude}", 0, "Locatie Curenta", location.latitude, location.longitude)
                            withContext(Dispatchers.Main) {
                                updateCity(finalCity)
                            }
                        }
                    }
                }
        } catch (e: SecurityException) { }
    }
}
