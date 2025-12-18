package com.example.proiect

import android.Manifest
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

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) fetchLocation()
    }
    
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Dupa ce primim permisiunea, putem lansa worker-ul imediat (daca suntem logati)
        if (isGranted && viewModel.currentUser.value != null) {
            triggerImmediateNotification()
        }
    }

    // Cerinta Etapa 1: UI complex cu mai multe activitati (aici Fragmente) si animatii
    // Functia principala care initializeaza activitatea, verifica permisiunile si configureaza navigarea
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Verificare permisiune locatie
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Initial ascundem bara de navigare
        bottomNav.visibility = View.GONE

        // Observam starea userului pentru a gestiona notificarile
        // Daca userul e deja logat (persista in repo/ViewModel), pornim notificarile
        // Nota: In acest ViewModel simplu, userul nu persista intre restarturi de aplicatie decat daca il salvam in Prefs
        // Dar daca aplicatia doar a fost in background, el ramane.
        
        // Pentru acest exemplu, presupunem ca login-ul se face manual, deci nu pornim notificarile aici
        // decat daca implementam "Auto Login".

        // Cerinta Etapa 1: Utilizare Fragmente si Animatii intre ele
        bottomNav.setOnItemSelectedListener { item ->
            val newTabId = item.itemId
            val transaction = supportFragmentManager.beginTransaction()

            // Determinam directia animatiei pentru slide
            val oldIndex = getTabIndex(selectedTabId)
            val newIndex = getTabIndex(newTabId)

            if (newIndex > oldIndex) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            } else if (newIndex < oldIndex) {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            
            // Schimbam fragmentul in container in functie de tab-ul selectat
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

        // Incarcam fragmentul initial de Autentificare
        if (savedInstanceState == null) {
            replaceFragment(AuthFragment())
        }
    }

    private fun schedulePeriodicNotifications() {
        // Schimbare la 1 ora (60 minute) conform cerintei
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
        // Lansam un OneTimeWorkRequest pentru a afisa notificarea imediat (t0)
        val oneTimeRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
            .build()
            
        WorkManager.getInstance(this).enqueueUniqueWork(
            "ImmediateWeatherUpdate",
            ExistingWorkPolicy.REPLACE, 
            oneTimeRequest
        )
    }

    // Apelat cand login-ul reuseste
    fun onLoginSuccess() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.visibility = View.VISIBLE
        replaceFragment(HomeFragment())
        selectedTabId = R.id.nav_home
        bottomNav.selectedItemId = R.id.nav_home
        
        // ACUM cerem permisiunea si pornim notificarile, doar dupa login
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

    // Apelat la delogare
    fun onLogout() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.visibility = View.GONE
        replaceFragment(AuthFragment())
        
        // Oprim notificarile la logout
        WorkManager.getInstance(this).cancelUniqueWork("WeatherUpdates")
        WorkManager.getInstance(this).cancelUniqueWork("ImmediateWeatherUpdate")
    }

    // Functie ajutatoare pentru a determina indexul tab-ului (folosita la calculul directiei animatiei)
    private fun getTabIndex(id: Int): Int {
        return when (id) {
            R.id.nav_home -> 0
            R.id.nav_forecast -> 1
            R.id.nav_favorites -> 2
            R.id.nav_settings -> 3
            else -> 0
        }
    }

    // Functie publica pentru a naviga la ecranul de Acasa din alte fragmente
    fun navigateToHome() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
    }

    // Functie simpla de inlocuire a fragmentului fara animatii custom (pentru initializare)
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Cerinta Etapa 1: Alte functionalitati (Localizare)
    // Preia locatia curenta a utilizatorului folosind FusedLocationProviderClient
    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel.fetchUserLocation(fusedLocationClient)
    }
}
