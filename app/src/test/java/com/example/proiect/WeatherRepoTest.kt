package com.example.proiect

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class WeatherRepoTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var weatherRepo: WeatherRepo

    @Mock
    private lateinit var favoritesDao: FavoritesDao

    @Mock
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        // Folosim clientul OkHttp care pointeaza catre serverul local mock
        // Injectam URL-ul mock-ului atat pentru weather cat si pentru geocoding
        val client = OkHttpClient.Builder().build()
        weatherRepo = WeatherRepo(favoritesDao, userDao, client, baseUrl, baseUrl)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchWeatherData returns data on successful response`() = runTest {
        // Simulăm un răspuns JSON valid de la API-ul Open-Meteo
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
    fun `login returns user when credentials are correct`() = runTest {
        val user = User(uid = 1, username = "test", password = "password")
        `when`(userDao.login("test", "password")).thenReturn(user)

        val result = weatherRepo.login("test", "password")
        
        assertNotNull(result)
        assertEquals("test", result?.username)
    }

    @Test
    fun `login returns null when credentials are incorrect`() = runTest {
        `when`(userDao.login("wrong", "pass")).thenReturn(null)

        val result = weatherRepo.login("wrong", "pass")
        
        assertNull(result)
    }
    
    @Test
    fun `register returns true when user is new`() = runTest {
        val newUser = User(username = "newuser", password = "123")
        // Simulam ca userul NU exista
        `when`(userDao.checkUserExists("newuser")).thenReturn(null)
        
        val result = weatherRepo.register(newUser)
        
        assertTrue(result)
    }

    @Test
    fun `register returns false when user already exists`() = runTest {
        val existingUser = User(username = "existing", password = "123")
        // Simulam ca userul exista DEJA
        `when`(userDao.checkUserExists("existing")).thenReturn(existingUser)
        
        val result = weatherRepo.register(existingUser)
        
        assertFalse(result)
    }
}
