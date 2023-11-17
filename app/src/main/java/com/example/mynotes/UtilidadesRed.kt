package com.example.mynotes

import android.content.Context
import android.net.ConnectivityManager

class UtilidadesRed {
    companion object {
        fun estaDisponibleRed(contexto: Context): Boolean {
            val gestorConectividad =
                contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val informacionRed = gestorConectividad.activeNetworkInfo
            return informacionRed != null && informacionRed.isConnected


        }
        
    }
}