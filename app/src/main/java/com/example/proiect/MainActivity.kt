package com.example.proiect

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    
    private val viewModel: WeatherViewModel by viewModels()
    private var selectedTabId = R.id.nav_home
    private val airplaneModeReceiver = AirplaneModeReceiver()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) fetchLocation()
    }
    
    // cerinta etapa 2: notificari (push)
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // dupa ce primim permisiunea, putem lansa worker-ul imediat (daca suntem logati)
        if (isGranted && viewModel.currentUser.value != null) {
            triggerImmediateNotification()
        }
    }

    // cerinta etapa 1: ui complex cu mai multe activitati (aici fragmente) si animatii
    // functia principala care initializeaza activitatea, verifica permisiunile si configureaza navigarea
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // verificare permisiune locatie
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // initial ascundem bara de navigare
        bottomNav.visibility = View.GONE
        

        // cerinta etapa 1: utilizare fragmente si animatii intre ele
        bottomNav.setOnItemSelectedListener { item ->
            val newTabId = item.itemId
            val transaction = supportFragmentManager.beginTransaction()

            // determinam directia animatiei pentru slide
            val oldIndex = getTabIndex(selectedTabId)
            val newIndex = getTabIndex(newTabId)

            if (newIndex > oldIndex) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            } else if (newIndex < oldIndex) {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            
            // schimbam fragmentul in container in functie de tab-ul selectat
            when (newTabId) {
                R.id.nav_home -> transaction.replace(R.id.fragment_container, HomeFragment())
                R.id.nav_forecast -> transaction.replace(R.id.fragment_container, ForecastFragment())
                R.id.nav_favorites -> transaction.replace(R.id.fragment_container, FavoritesFragment())
                R.id.nav_settings -> transaction.replace(R.id.fragment_container, SettingsFragment())
            }
            
            transaction.commit()
            selectedTabId = newTabId
            true
        }

        // incarcam fragmentul initial de autentificare
        if (savedInstanceState == null) {
            replaceFragment(AuthFragment())
        }
    }

    // cerinta etapa 2: broadcast receivers
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        registerReceiver(airplaneModeReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(airplaneModeReceiver)
    }

    // cerinta etapa 2: notificari (push)
    private fun schedulePeriodicNotifications() {
        // schimbare la 1 ora (60 minute) conform cerintei
        val workRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
            1, TimeUnit.HOURS 
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherUpdates",
            ExistingPeriodicWorkPolicy.UPDATE, 
            workRequest
        )
    }
    
    private fun triggerImmediateNotification() {
        // lansam un onetimeworkrequest pentru a afisa notificarea imediat (t0)
        val oneTimeRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
            .build()
            
        WorkManager.getInstance(this).enqueueUniqueWork(
            "ImmediateWeatherUpdate",
            ExistingWorkPolicy.REPLACE, 
            oneTimeRequest
        )
    }

    // apelat cand login-ul reuseste
    fun onLoginSuccess() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.visibility = View.VISIBLE
        replaceFragment(HomeFragment())
        selectedTabId = R.id.nav_home
        bottomNav.selectedItemId = R.id.nav_home
        
        // acum cerem permisiunea si pornim notificarile, doar dupa login
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                triggerImmediateNotification()
                schedulePeriodicNotifications()
            }
        } else {
            triggerImmediateNotification()
            schedulePeriodicNotifications()
        }
    }

    // apelat la delogare
    fun onLogout() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.visibility = View.GONE
        replaceFragment(AuthFragment())
        
        // oprim notificarile la logout
        WorkManager.getInstance(this).cancelUniqueWork("WeatherUpdates")
        WorkManager.getInstance(this).cancelUniqueWork("ImmediateWeatherUpdate")
    }

    // functie ajutatoare pentru a determina indexul tab-ului (folosita la calculul directiei animatiei)
    private fun getTabIndex(id: Int): Int {
        return when (id) {
            R.id.nav_home -> 0
            R.id.nav_forecast -> 1
            R.id.nav_favorites -> 2
            R.id.nav_settings -> 3
            else -> 0
        }
    }

    // functie publica pentru a naviga la ecranul de acasa din alte fragmente
    fun navigateToHome() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
    }

    // functie simpla de inlocuire a fragmentului fara animatii custom (pentru initializare)
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // cerinta etapa 1: alte functionalitati (localizare)
    // preia locatia curenta a utilizatorului folosind fusedlocationproviderclient
    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel.fetchUserLocation(fusedLocationClient)
    }
}
