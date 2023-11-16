package com.example.mynotes

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.dao.NoteDao
import com.example.mynotes.database.entity.NoteEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


class CrearNotas : AppCompatActivity() {
    private lateinit var notaEditText: EditText
    private lateinit var crearNotaButton: Button
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_notas)

        notaEditText = findViewById(R.id.notaEditText)
        crearNotaButton = findViewById(R.id.crearNotaButton)
        noteDao = (application as MyNotesApplication).appDatabase.noteDao()

        val ubicacionString = intent.getStringExtra("posicion")

        val posicion = if (ubicacionString != null && ubicacionString != "null,null") {
            val ubicacionArray = ubicacionString.split(",")
            if (ubicacionArray.size == 2) {
                val latitud = ubicacionArray[0].toDouble()
                val longitud = ubicacionArray[1].toDouble()
                GeoPoint(latitud, longitud)
            } else {
                GeoPoint(0.0, 0.0)
            }
        } else {
            GeoPoint(0.0, 0.0)
        }

        crearNotaButton.setOnClickListener {
            val nota = notaEditText.text.toString()
            val aplicacion = "Mobil"
            val propietario = "Cristian D"
            val fecha = Timestamp.now()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fecha_registro = dateFormat.format(fecha.toDate())

            val notas = hashMapOf(
                "aplicacion" to aplicacion,
                "fecha" to fecha,
                "fecha_registro" to fecha_registro,
                "nota" to nota,
                "posicion" to posicion,
                "propietario" to propietario
            )

            if (isNetworkAvailable()) {
                Firebase.firestore.collection("MyNotes")
                    .add(notas)
                    .addOnSuccessListener { documentReference ->
                        val firestoreId = documentReference.id // id de firestore
                        Log.d("TAG", "Documento agregado con ID: $firestoreId")
                        Toast.makeText(this, "La nota se ha creado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.w("TAG", "Error al agregar el documento", e)
                        Toast.makeText(this, "Error al agregar la nota en Firebase", Toast.LENGTH_SHORT).show()
                    }
            } else {
                GlobalScope.launch {
                    val noteEntity = NoteEntity(
                        id = "1",
                        nota = nota,
                        aplicacion = aplicacion,
                        propietario = propietario,
                        fecha_registro = fecha_registro,
                        fecha = fecha.toString(),
                        posicion = posicion,
                        //estado = "NoEnviado"
                    )
                    noteDao.insert(noteEntity)

                }
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

}

