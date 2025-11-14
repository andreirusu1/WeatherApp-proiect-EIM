package com.example.proiect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// --- Repository for data handling ---
class WeatherRepo {
    private val client = OkHttpClient()

    suspend fun fetchWeatherData(lat: String, lon: String): WeatherData? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val temperature = current.getDouble("temperature_2m")
                val weatherCode = current.getInt("weather_code")
                WeatherData(temperature, weatherCode)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Cer senin"
            1 -> "Parțial noros"
            2 -> "Parțial noros"
            3 -> "Noros"
            45 -> "Ceață"
            else -> "Vreme bună"
        }
    }
}

data class WeatherData(val temperature: Double, val weatherCode: Int)

// Fragments
class HomeFragment : Fragment() {
    private lateinit var weatherRepo: WeatherRepo
    private lateinit var tvCity: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvDesc: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        tvCity = view.findViewById(R.id.tvCity)
        tvTemp = view.findViewById(R.id.tvTemp)
        tvDesc = view.findViewById(R.id.tvDesc)
        weatherRepo = WeatherRepo()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchWeatherForBucharest()
    }

    private fun fetchWeatherForBucharest() {
        lifecycleScope.launch {
            val weatherData = weatherRepo.fetchWeatherData("44.4323", "26.1063")
            if (weatherData != null) {
                tvCity.text = "București"
                tvTemp.text = "${weatherData.temperature}°C"
                tvDesc.text = weatherRepo.getWeatherDescription(weatherData.weatherCode)
            } else {
                tvCity.text = "București"
                tvTemp.text = "--°C"
                tvDesc.text = "Eroare la preluarea datelor"
            }
        }
    }
}

class FavoritesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }
}


//  Main Activity
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // Load the default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_favorites -> FavoritesFragment()
                else -> null
            }

            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
                true
            } else {
                false
            }
        }
    }
}
