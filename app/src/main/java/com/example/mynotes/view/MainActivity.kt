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

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val email = "pruebasgoo@coordinadora.com"
    private val password = "Coordi2023"
    private lateinit var addNoteButton: Button
    private lateinit var Sincrinizacion: Button
    public lateinit var notesListView: RecyclerView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
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
        Sincrinizacion.setOnClickListener {
            // Agrega la llamada a la función de sincronización
            myNotesPresenter.sincronizarNotasConFirestore()
        }
        obtenerYMostrarUbicacionActual()
    }

    private fun setupView() {
        auth = Firebase.auth
        notesListView = findViewById(R.id.notesListView)
        addNoteButton = findViewById(R.id.addNoteButton)
        Sincrinizacion = findViewById(R.id.Sincrinizacion)
        appDatabase = (application as MyNotesApplication).appDatabase

        // Inicializa myNotesPresenter después de inicializar otras variables
        myNotesPresenter = MyNotesPresenter(this)
    }

    private fun setupListeners() {
        if (::myNotesPresenter.isInitialized) {
            addNoteButton.setOnClickListener {
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
        myNotesPresenter.obtenerYMostrarUbicacionActual()
    }
    private fun getNotes() {
        myNotesPresenter.getNotes()
    }
    fun loadRoomNotes() {
       myNotesPresenter.cargarRoomNotes()
    }

    private fun sincronizarNotas() {
        myNotesPresenter.sincronizarNotasConFirestore()
    }


}