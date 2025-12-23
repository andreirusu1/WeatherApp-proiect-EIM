package com.example.proiect

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Rounded.WbSunny
        1, 2 -> Icons.Rounded.WbCloudy
        3 -> Icons.Rounded.Cloud
        45, 48 -> Icons.Rounded.Dehaze
        51, 53, 55 -> Icons.Rounded.Grain
        61, 63, 65 -> Icons.Rounded.Umbrella
        71, 73, 75 -> Icons.Rounded.AcUnit
        80, 81, 82 -> Icons.Rounded.WaterDrop
        95, 96, 99 -> Icons.Rounded.Thunderstorm
        else -> Icons.Rounded.QuestionMark
    }
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Cer senin"
        1, 2 -> "Partial noros"
        3 -> "Innorat"
        45, 48 -> "Ceata"
        51, 53, 55 -> "Burnita"
        61, 63, 65 -> "Ploaie"
        71, 73, 75 -> "Ninsoare"
        80, 81, 82 -> "Averse"
        95, 96, 99 -> "Furtuna"
        else -> "Necunoscut"
    }
}

fun getWeatherColor(code: Int): Brush {
    return when (code) {
        0 -> Brush.verticalGradient(listOf(Color(0xFFFFA726), Color(0xFFFFD54F)))
        1, 2, 3 -> Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF90CAF9)))
        45, 48 -> Brush.verticalGradient(listOf(Color(0xFF607D8B), Color(0xFF90A4AE)))
        in 51..67 -> Brush.verticalGradient(listOf(Color(0xFF3949AB), Color(0xFF7986CB)))
        in 71..77 -> Brush.verticalGradient(listOf(Color(0xFF0288D1), Color(0xFF81D4FA)))
        95, 96, 99 -> Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF546E7A)))
        else -> Brush.verticalGradient(listOf(Color(0xFF66BB6A), Color(0xFFA5D6A7)))
    }
}
