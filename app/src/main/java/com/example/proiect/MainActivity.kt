package com.example.proiect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class WeatherData(val temperature: Double, val weatherCode: Int)

@Entity(tableName = "favorites")
data class City(@PrimaryKey val id: String, val name: String, val lat: Double, val lon: Double)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites") suspend fun getAll(): List<City>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(city: City)
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

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, state: Bundle?) = inflater.inflate(R.layout.fragment_favorites, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etCity: EditText = view.findViewById(R.id.et_city_search)
        val btnAdd: Button = view.findViewById(R.id.btn_add_favorite)
        val lvFavs: ListView = view.findViewById(R.id.lv_favorites)
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1)
        lvFavs.adapter = adapter

        fun refreshList() {
            lifecycleScope.launch {
                val cities = repo.getFavoriteCities()
                adapter.clear()
                adapter.addAll(cities.map { it.name })
                adapter.notifyDataSetChanged()
                lvFavs.setOnItemClickListener { _, _, position, _ ->
                    (activity as? MainActivity)?.showWeatherForCity(cities[position])
                }
            }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weather-db").build()
        weatherRepo = WeatherRepo(db.favoritesDao())

        if (savedInstanceState == null) { showWeatherForCity(null) }

        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { 
            when (it.itemId) {
                R.id.nav_home -> showWeatherForCity(null)
                R.id.nav_favorites -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, FavoritesFragment()).commit()
            }
            true
        }
    }

    fun showWeatherForCity(city: City?) {
        val fragment = city?.let { HomeFragment.newInstance(it) } ?: HomeFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
