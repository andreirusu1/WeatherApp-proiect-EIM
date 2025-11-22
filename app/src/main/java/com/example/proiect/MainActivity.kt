package com.example.proiect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private val viewModel: WeatherViewModel by viewModels()
    private var selectedTabId = R.id.nav_home

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) fetchLocation()
    }

    // Cerinta Etapa 1: UI complex cu mai multe activitati (aici Fragmente) si animatii
    // Functia principala care initializeaza activitatea, verifica permisiunile si configureaza navigarea
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }

        // Cerinta Etapa 1: Utilizare Fragmente si Animatii intre ele
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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

        // Incarcam fragmentul initial daca aplicatia porneste prima data
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }
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
