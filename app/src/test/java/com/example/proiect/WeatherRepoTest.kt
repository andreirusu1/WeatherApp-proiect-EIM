package com.example.proiect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

// cerinta etapa 2: teste unitare serioase
@ExperimentalCoroutinesApi
class WeatherRepoTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var weatherRepo: WeatherRepo

    // folosim mockito pentru a simula serviciile firebase in teste
    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()
        val client = OkHttpClient.Builder().build()

        // initializam repo cu clientul de mock si serviciile firebase simulate
        weatherRepo = WeatherRepo(client, baseUrl, baseUrl, firebaseAuth, firestore)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchWeatherData returns data on successful response`() = runTest {
        // simulam un raspuns json valid de la api-ul open-meteo
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "current": {
                    "temperature_2m": 25.5,
                    "weather_code": 1,
                    "relative_humidity_2m": 60,
                    "wind_speed_10m": 5.0
                  },
                  "hourly": {
                    "time": ["2023-10-10T12:00", "2023-10-10T13:00", "2023-10-10T14:00", "2023-10-10T15:00", "2023-10-10T16:00", "2023-10-10T17:00", "2023-10-10T18:00", "2023-10-10T19:00", "2023-10-10T20:00", "2023-10-10T21:00", "2023-10-10T22:00", "2023-10-10T23:00", "2023-10-11T00:00", "2023-10-11T01:00", "2023-10-11T02:00", "2023-10-11T03:00", "2023-10-11T04:00", "2023-10-11T05:00", "2023-10-11T06:00", "2023-10-11T07:00", "2023-10-11T08:00", "2023-10-11T09:00", "2023-10-11T10:00", "2023-10-11T11:00"],
                    "temperature_2m": [25.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0, 26.0],
                    "weather_code": [1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
                  },
                  "daily": {
                    "time": ["2023-10-10"],
                    "weather_code": [1],
                    "temperature_2m_max": [28.0],
                    "temperature_2m_min": [18.0]
                  }
                }
            """.trimIndent())
        
        mockWebServer.enqueue(mockResponse)

        val result = weatherRepo.fetchWeatherData("44.43", "26.10")

        assertNotNull(result)
        assertEquals(25.5, result!!.temperature, 0.01)
        assertEquals(1, result.daily.size)
        assertEquals("10.10.2023", result.daily[0].date)
    }

    @Test
    fun `fetchWeatherData returns null on API error`() = runTest {
        // simulam un raspuns de eroare de la server
        val mockResponse = MockResponse()
            .setResponseCode(404)
        
        mockWebServer.enqueue(mockResponse)

        val result = weatherRepo.fetchWeatherData("44.43", "26.10")

        // verificam ca metoda returneaza null in caz de eroare
        assertNull(result)
    }

}
