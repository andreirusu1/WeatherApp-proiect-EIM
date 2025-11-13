package com.example.proiect

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var tvCity: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCity = findViewById(R.id.tvCity)
        tvTemp = findViewById(R.id.tvTemp)
        tvDesc = findViewById(R.id.tvDesc)

        fetchWeatherForBucharest()
    }

    private fun fetchWeatherForBucharest() {
        lifecycleScope.launch {
            val weatherData = withContext(Dispatchers.IO) {
                fetchWeatherData("44.4323", "26.1063")
            }


            if (weatherData != null) {
                tvCity.text = "București"
                tvTemp.text = "${weatherData.temperature}°C"
                tvDesc.text = getWeatherDescription(weatherData.weatherCode)
            } else {
                tvCity.text = "București"
                tvTemp.text = "--°C"
                tvDesc.text = "Eroare la preluarea datelor"
            }
        }
    }

    private fun fetchWeatherData(lat: String, lon: String): WeatherData? {
        val client = OkHttpClient()
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        val request = Request.Builder().url(url).build()

        return try {
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

    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Cer senin"
            1, 2, 3 -> "Parțial noros"
            else -> "Vreme necunoscută"
        }
    }


    data class WeatherData(val temperature: Double, val weatherCode: Int)
}
