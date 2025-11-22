package com.example.proiect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Cerinta Etapa 1: Utilizare Fragmente
// Acest fisier contine clasele Fragment care servesc drept containere pentru ecranele Jetpack Compose
class HomeFragment : Fragment() {
    // Folosim activityViewModels pentru a partaja acelasi ViewModel intre fragmente (si implicit datele)
    private val viewModel: WeatherViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Cerinta Etapa 1: Utilizare Jetpack Compose
        // Inlocuim layout-ul XML clasic cu un ComposeView
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1976D2))) {
                    HomeScreen(viewModel)
                }
            }
        }
    }
}

class ForecastFragment : Fragment() {
    private val viewModel: WeatherViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1976D2))) {
                    ForecastScreen(viewModel)
                }
            }
        }
    }
}

class FavoritesFragment : Fragment() {
    private val viewModel: WeatherViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1976D2))) {
                    FavoritesScreen(viewModel, onCitySelected = {
                        // Navigare inapoi la Home prin Activity
                        (activity as? MainActivity)?.navigateToHome()
                    })
                }
            }
        }
    }
}

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1976D2))) {
                    SettingsScreen()
                }
            }
        }
    }
}
