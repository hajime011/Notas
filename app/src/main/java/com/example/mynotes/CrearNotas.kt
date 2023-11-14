package com.example.mynotes

import android.content.Intent
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
    private lateinit var noteDao: NoteDao // Asegúrate de obtener el DAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_notas)

        val db = Firebase.firestore

        notaEditText = findViewById(R.id.notaEditText)
        crearNotaButton = findViewById(R.id.crearNotaButton)
        noteDao = (application as MyNotesApplication).appDatabase.noteDao() // Obtiene el DAO

        val ubicacionString = intent.getStringExtra("ubicacion")

        val ubicacion = if (ubicacionString != null) {
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
            val fechaActual = Timestamp.now()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fecha = dateFormat.format(fechaActual.toDate())

            val notas = hashMapOf(
                "aplicacion" to aplicacion,
                "fecha" to fechaActual,
                "fecha_registro" to fecha,
                "nota" to nota,
                "posicion" to ubicacion,
                "propietario" to propietario
            )

            // Agregar un nuevo documento con un ID generado automáticamente en Firestore
            db.collection("MyNotes")
                .add(notas)
                .addOnSuccessListener { documentReference ->
                    val firestoreId = documentReference.id // id de firestore
                    Log.d("TAG", "Documento agregado con ID: $firestoreId")

                    GlobalScope.launch {
                        // Insertar la nota en la base de datos local con el ID de Firestore
                        val noteEntity = NoteEntity(
                            id = firestoreId,
                            nota = nota,
                            aplicacion = aplicacion,
                            propietario = propietario,
                            fecha_registro = fecha,
                            ubicacion = ubicacion
                        )
                        noteDao.insert(noteEntity)
                    }
                    Toast.makeText(this, "La nota se ha creado correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w("TAG", "Error al agregar el documento", e)
                }


            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}

