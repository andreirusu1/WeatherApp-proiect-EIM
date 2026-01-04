package com.example.proiect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AirplaneModeReceiver : BroadcastReceiver() {

    // aceasta functie se apeleaza automat cand sistemul trimite evenimentul ascultat
    override fun onReceive(context: Context?, intent: Intent?) {
        // verificam daca intent-ul este cel de schimbare a modului avion
        if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            
            // extragem starea (true = activat, false = dezactivat)
            val isTurnedOn = intent.getBooleanExtra("state", false)

            if (isTurnedOn) {
                Toast.makeText(
                    context, 
                    "Mod Avion activat! Actualizarea datelor meteo este oprita.", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context, 
                    "Mod Avion dezactivat! Conexiunea a revenit.", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
