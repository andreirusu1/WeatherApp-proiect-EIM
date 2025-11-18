package com.example.proiect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

data class WeatherData(val temperature: Double, val weatherCode: Int)

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


class WeatherRepo(private val favoritesDao: FavoritesDao) {
    private val client = OkHttpClient()

    suspend fun fetchWeatherData(lat: String, lon: String): WeatherData? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val current = JSONObject(body).getJSONObject("current")
                WeatherData(current.getDouble("temperature_2m"), current.getInt("weather_code"))
            } else { null }
        } catch (e: Exception) { null }
    }

    suspend fun searchCity(name: String): City? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$name&count=1&language=ro&format=json"
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val results = JSONObject(body).optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val cityJson = results.getJSONObject(0)
                    City(
                        id = "${cityJson.getDouble("latitude")},${cityJson.getDouble("longitude")}",
                        name = cityJson.getString("name"),
                        lat = cityJson.getDouble("latitude"),
                        lon = cityJson.getDouble("longitude")
                    )
                } else { null }
            } else { null }
        } catch (e: Exception) { null }
    }

    fun getWeatherDescription(weatherCode: Int) = when (weatherCode) {
        0 -> "Cer senin"; 1, 2, 3 -> "Parțial noros"; 45, 48 -> "Ceață";
        51, 53, 55 -> "Burniță"; 61, 63, 65 -> "Ploaie"; 71, 73, 75 -> "Ninsoare";
        80, 81, 82 -> "Averse"; 95 -> "Furtună"; else -> "Vreme bună"
    }

    suspend fun getFavoriteCities(): List<City> = favoritesDao.getAll()
    suspend fun addFavoriteCity(city: City) = favoritesDao.add(city)
    suspend fun removeFavoriteCity(city: City) = favoritesDao.remove(city.id)
}

class HomeFragment : Fragment() {
    private val repo: WeatherRepo by lazy { (activity as MainActivity).weatherRepo }
    private var city: City? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            city = City(it.getString("id")!!, it.getString("name")!!, it.getDouble("lat"), it.getDouble("lon"))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, state: Bundle?) = inflater.inflate(R.layout.fragment_home, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvCity: TextView = view.findViewById(R.id.tvCity)
        val tvTemp: TextView = view.findViewById(R.id.tvTemp)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val cityToFetch = city ?: City("44.43,26.10", "București", 44.43, 26.10)

        lifecycleScope.launch {
            val data = repo.fetchWeatherData(cityToFetch.lat.toString(), cityToFetch.lon.toString())
            tvCity.text = cityToFetch.name
            tvTemp.text = data?.temperature?.let { "${it}°C" } ?: "--°C"
            tvDesc.text = data?.weatherCode?.let { repo.getWeatherDescription(it) } ?: "Eroare"
        }
    }

    companion object {
        fun newInstance(city: City) = HomeFragment().apply {
            arguments = Bundle().apply {
                putString("id", city.id); putString("name", city.name)
                putDouble("lat", city.lat); putDouble("lon", city.lon)
            }
        }
    }
}

class FavoritesFragment : Fragment() {
    private val repo: WeatherRepo by lazy { (activity as MainActivity).weatherRepo }
    private lateinit var adapter: ArrayAdapter<String>
    private var favoriteCities: List<City> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, state: Bundle?) = inflater.inflate(R.layout.fragment_favorites, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etCity: EditText = view.findViewById(R.id.et_city_search)
        val btnAdd: Button = view.findViewById(R.id.btn_add_favorite)
        val lvFavs: ListView = view.findViewById(R.id.lv_favorites)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        lvFavs.adapter = adapter

        fun refreshList() {
            lifecycleScope.launch {
                favoriteCities = repo.getFavoriteCities()
                adapter.clear()
                adapter.addAll(favoriteCities.map { it.name })
            }
        }

        lvFavs.setOnItemClickListener { _, _, position, _ ->
            (activity as? MainActivity)?.showWeatherForCity(favoriteCities[position])
        }

        lvFavs.setOnItemLongClickListener { _, _, position, _ ->
            val cityToRemove = favoriteCities[position]
            lifecycleScope.launch {
                repo.removeFavoriteCity(cityToRemove)
                Toast.makeText(context, "${cityToRemove.name} șters!", Toast.LENGTH_SHORT).show()
                refreshList()
            }
            true
        }

        btnAdd.setOnClickListener {
            val name = etCity.text.toString()
            if (name.isNotBlank()) {
                lifecycleScope.launch {
                    repo.searchCity(name)?.let { city ->
                        repo.addFavoriteCity(city)
                        etCity.text.clear()
                        Toast.makeText(context, "${city.name} adăugat!", Toast.LENGTH_SHORT).show()
                        refreshList()
                    } ?: Toast.makeText(context, "Oraș negăsit!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        refreshList()
    }
}


class MainActivity : AppCompatActivity() {

    lateinit var weatherRepo: WeatherRepo
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchAndShowCurrentLocationWeather()
        } else {
            showWeatherForCity(null) // Show default Bucharest
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weather-db").build()
        weatherRepo = WeatherRepo(db.favoritesDao())

        if (savedInstanceState == null) {
            requestLocationPermission()
        }

        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { 
            when (it.itemId) {
                R.id.nav_home -> requestLocationPermission() // Re-fetch location or show default
                R.id.nav_favorites -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, FavoritesFragment()).commit()
            }
            true
        }
    }

    private fun requestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                fetchAndShowCurrentLocationWeather()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun fetchAndShowCurrentLocationWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lifecycleScope.launch {
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                    var city: City? = null
                    try {
                        val addresses = withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        }
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            city = City(
                                id = "${location.latitude},${location.longitude}",
                                name = address.locality ?: "Locație Curentă",
                                lat = location.latitude,
                                lon = location.longitude
                            )
                        }
                    } catch (e: IOException) {
                       // Could not get city name
                    }
                    showWeatherForCity(city ?: City("${location.latitude},${location.longitude}","Locație Curentă", location.latitude, location.longitude))
                }
            } else {
                showWeatherForCity(null) // Fallback to default if location is null
            }
        }
    }

    fun showWeatherForCity(city: City?) {
        val fragment = city?.let { HomeFragment.newInstance(it) } ?: HomeFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
