package com.example.mynotes.view

import com.example.mynotes.adapter.NotesAdapter
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.R
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.AppDatabase
import com.example.mynotes.database.dao.NoteDao
import com.example.mynotes.database.entity.NoteEntity
import com.example.mynotes.presenter.MyNotesPresenter
import com.example.mynotes.util.CONSTANTES
import com.example.mynotes.util.UtilidadesRed
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val email = "pruebasgoo@coordinadora.com"
    private val password = "Coordi2023"
    private lateinit var addNoteButton: Button
    private lateinit var notesListView: RecyclerView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var ubicacionActual: GeoPoint? = null
    private lateinit var appDatabase: AppDatabase
    private lateinit var noteDao: NoteDao

    //presenter
    private lateinit var myNotesPresenter: MyNotesPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        permisos()
        sessionFirebase()
        setupListeners()
        //loadRoomNotes()
        sincronizarNotas()
        getNotes()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        obtenerYMostrarUbicacionActual()
    }

    private fun setupView() {
        auth = Firebase.auth
        notesListView = findViewById(R.id.notesListView)
        addNoteButton = findViewById(R.id.addNoteButton)
        appDatabase = (application as MyNotesApplication).appDatabase

        // Inicializa myNotesPresenter después de inicializar otras variables
        myNotesPresenter = MyNotesPresenter(this)
    }

    private fun setupListeners() {
        // Verifica si myNotesPresenter está inicializado antes de usarlo
        if (::myNotesPresenter.isInitialized) {
            addNoteButton.setOnClickListener {
                // Mostrar el diálogo de creación de notas
                myNotesPresenter.mostrarDialogoCrearNotas()
            }
        }
    }
    private fun sessionFirebase() {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("TAG", "signInWithEmail:success")
                    val user = auth.currentUser
                } else {
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun permisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }


    private fun obtenerYMostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        ubicacionActual = GeoPoint(location.latitude, location.longitude)
                        Log.d("MAR", "Ubicación actual - Latitud: ${location.latitude}, Longitud: ${location.longitude}")
                    } else {
                        Log.w("MAR", "No se pudo obtener la ubicación actual.")
                    }
                }
        } else {
            Log.w("TAG", "No tienes permiso para acceder a la ubicación.")
        }
    }
    public fun getNotes() {
        myNotesPresenter.getNotes()
    }
    fun loadRoomNotes() {
        GlobalScope.launch(Dispatchers.IO) {
            val roomNotes: List<NoteEntity> = appDatabase.noteDao().getAllNotes()
            runOnUiThread {
                listRoomNotes(roomNotes)

            }
        }
    }

    private fun listRoomNotes(roomNotes: List<NoteEntity>) {
        val mutableRoomNotes: MutableList<NoteEntity> = roomNotes.toMutableList()
        val adapter = NotesAdapter(mutableRoomNotes, this)
        notesListView.layoutManager = LinearLayoutManager(this)
        notesListView.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    private fun sincronizarNotas() {
        myNotesPresenter.sincronizarNotasConFirestore()
    }


}