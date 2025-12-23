package com.example.proiect

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// Helper functions pentru conversie
fun formatTemp(celsius: Double, isImperial: Boolean): String {
    val value = if (isImperial) (celsius * 9/5) + 32 else celsius
    return String.format(Locale.US, "%.1f%s", value, if(isImperial) "°F" else "°C")
}

fun formatSpeed(kmh: Double, isImperial: Boolean): String {
    val value = if (isImperial) kmh * 0.621371 else kmh
    return String.format(Locale.US, "%.1f %s", value, if(isImperial) "mph" else "km/h")
}

@Composable
fun WeatherCard(title: String, value: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        modifier = Modifier.padding(4.dp).size(width = 110.dp, height = 90.dp), // Marit putin pt text mph
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = title, fontSize = 11.sp)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HourlyForecastItem(forecast: HourlyForecast, isImperialTemp: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = forecast.time, 
            fontSize = 16.sp, 
            color = Color.White, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
        Icon(
            getWeatherIcon(forecast.weatherCode),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = getWeatherDescription(forecast.weatherCode),
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatTemp(forecast.temp, isImperialTemp), 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Bold, 
            color = Color.White
        )
    }
}

// UI Login Screen
@Composable
fun AuthScreen(viewModel: WeatherViewModel, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoginSuccess()
        }
    }

    val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF1976D2), Color(0xFF64B5F6)))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Autentificare", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Utilizator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parolă") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (loginError != null) {
                    Text(loginError!!, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { viewModel.login(username, password) }) {
                        Text("Login")
                    }
                    OutlinedButton(onClick = { viewModel.register(username, password) }) {
                        Text("Register")
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: WeatherViewModel) {
    val city by viewModel.currentCity.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    // Preferinte
    val isImperialTemp by viewModel.isImperialTemp.collectAsStateWithLifecycle()
    val isImperialWind by viewModel.isImperialWind.collectAsStateWithLifecycle()
    
    LaunchedEffect(city) {
        viewModel.fetchWeatherForCity(city)
    }

    val backgroundBrush = weatherData?.let { getWeatherColor(it.weatherCode) } 
        ?: Brush.verticalGradient(listOf(Color.LightGray, Color.Gray))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        AnimatedVisibility(
            visible = weatherData != null,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(1000)),
            exit = fadeOut()
        ) {
            if (weatherData != null) {
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 40.dp, bottom = 16.dp)
                ) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Salut, ${currentUser?.username ?: "Guest"}", fontSize = 20.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(city.name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Icon(
                                getWeatherIcon(weatherData!!.weatherCode),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = Color.White
                            )
                            // Conversie Temperatura
                            Text(formatTemp(weatherData!!.temperature, isImperialTemp), fontSize = 64.sp, color = Color.White)
                            Text(getWeatherDescription(weatherData!!.weatherCode), fontSize = 24.sp, color = Color.White)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                WeatherCard("Umiditate", "${weatherData!!.humidity}%", Icons.Default.WaterDrop)
                                // Conversie Vant
                                WeatherCard("Vânt", formatSpeed(weatherData!!.windSpeed, isImperialWind), Icons.Default.Air)
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Prognoză Orară", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 20.sp,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.Start)
                            )
                        }
                    }
                    
                    items(weatherData!!.hourly) {
                        HourlyForecastItem(it, isImperialTemp)
                        Divider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        
        if (weatherData == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}

@Composable
fun ForecastScreen(viewModel: WeatherViewModel) {
    val city by viewModel.currentCity.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    
    // Preferinte
    val isImperialTemp by viewModel.isImperialTemp.collectAsStateWithLifecycle()

    LaunchedEffect(city) {
         viewModel.fetchWeatherForCity(city)
    }
    
    val backgroundBrush = weatherData?.let { getWeatherColor(it.weatherCode) } 
        ?: Brush.verticalGradient(listOf(Color.LightGray, Color.Gray))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("7 Zile - ${city.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            if (weatherData != null) {
                LazyColumn {
                    items(weatherData!!.daily) { day ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(day.date, fontWeight = FontWeight.Bold)
                                    Text(getWeatherDescription(day.weatherCode), fontSize = 13.sp, color = Color.DarkGray)
                                }
                                Icon(getWeatherIcon(day.weatherCode), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                Text(
                                    "${formatTemp(day.maxTemp, isImperialTemp)} / ${formatTemp(day.minTemp, isImperialTemp)}", 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: WeatherViewModel, onCitySelected: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val suggestions by viewModel.searchResults.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
             delay(500)
             viewModel.searchCities(searchQuery)
        } else {
             viewModel.searchCities("")
        }
    }

    val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF26C6DA)))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Favorite & Căutare", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Caută oraș...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f)
                    )
                )
                
                if (suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.padding(top = 60.dp).fillMaxWidth().heightIn(max = 200.dp),
                        shadowElevation = 4.dp,
                        tonalElevation = 4.dp
                    ) {
                        LazyColumn {
                            items(suggestions) { city ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addToFavorites(city)
                                            searchQuery = ""
                                            Toast.makeText(context, "Adăugat!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(city.name)
                                }
                                Divider()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(favorites, key = { it.id }) { city ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { 
                                viewModel.updateCity(city)
                                onCitySelected() 
                            },
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(city.name, fontSize = 18.sp)
                            IconButton(onClick = {
                                viewModel.removeFromFavorites(city)
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: WeatherViewModel, onLogout: () -> Unit) {
    val isImperialTemp by viewModel.isImperialTemp.collectAsStateWithLifecycle()
    val isImperialWind by viewModel.isImperialWind.collectAsStateWithLifecycle()
    
    val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFAB47BC), Color(0xFF7E57C2)))
    
    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Setări", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Unități de Măsură", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Switch Temp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Temperatură")
                            Text(if(isImperialTemp) "Fahrenheit (°F)" else "Celsius (°C)", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isImperialTemp,
                            onCheckedChange = { viewModel.setTempUnit(it) }
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    // Switch Wind
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Viteza Vântului")
                            Text(if(isImperialWind) "Imperial (mph)" else "Metric (km/h)", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isImperialWind,
                            onCheckedChange = { viewModel.setWindUnit(it) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("Deconectare", fontSize = 18.sp)
            }
        }
    }
}
