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
        sincroFirestore()
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
    fun getNotes() {
        if (UtilidadesRed.estaDisponibleRed((this))) {
            db.collection(CONSTANTES.COLLECTION_NOTES)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val lista : ArrayList<NoteEntity> = ArrayList()
                        appDatabase = (this.application as MyNotesApplication).appDatabase
                        for (document in it.result) {

                            val data = NoteEntity(
                                document.id,
                                document.getString("aplicacion").toString(),
                                document.getTimestamp("fecha")!!.toDate().toString(),
                                document.getString("fecha_registro").toString(),
                                document.getString("nota").toString(),
                                document.getGeoPoint("posicion").toString(),
                                document.getString("propietario").toString(),
                                "SiEnviado"
                            )
                            lista.add(data)
                            GlobalScope.launch(Dispatchers.IO) {
                                (appDatabase).noteDao()
                                    .insert(data)
                            }
                            val adapter = NotesAdapter(lista, this)
                            notesListView.layoutManager = LinearLayoutManager(this)
                            notesListView.adapter = adapter
                            adapter.notifyDataSetChanged()

                        }
                        loadRoomNotes()
                    }
                }.addOnFailureListener {
                    Log.i("ERROR", it.message.toString())
                }
        } else {
//            OFFLINE
            loadRoomNotes()
        }


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

    override fun onResume() {
        super.onResume()
        loadRoomNotes()
    }


    private fun sincroFirestore() {
        GlobalScope.launch(Dispatchers.IO) {
            val localNotes = appDatabase.noteDao().getAllNotes()



            for (localNote in localNotes) {
                when (localNote.estado) {
                    "Borrado" -> {
                        syncNoteDeleteFirebase(localNote,this@MainActivity)
                    }

                    "Editado" -> {
                        syncEditedNoteWithFirebase(localNote)
                    }
                    "NoEnviado"->{
                        syncNoteWithFirebase(localNote)
                    }
                }
            }
        }
    }
    private fun syncEditedNoteWithFirebase(localNote: NoteEntity) {
        val noteData = hashMapOf(
            "aplicacion" to localNote.aplicacion,
            "fecha" to converterStringAtTimestamp(localNote.fecha),
            "fecha_registro" to localNote.fecha_registro,
            "nota" to localNote.nota,
            "posicion" to geoPointConverter(localNote.posicion),
            "propietario" to localNote.propietario,
        )

        db.collection(CONSTANTES.COLLECTION_NOTES)
            .document(localNote.id)
            .set(noteData)
            .addOnSuccessListener { documentReference ->
                GlobalScope.launch(Dispatchers.IO) {
                    localNote.estado = "SiEnviado"
                    appDatabase.noteDao().update(localNote)
                    Log.d("MAR", "Edited note synced to Firebase: $localNote")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MAR", "Error syncing edited note to Firebase: ${e.message}")
            }
    }
    private suspend fun syncNoteDeleteFirebase(localNote: NoteEntity,mainActivity: MainActivity) {
        // Aquí obtienes la referencia del documento en Firestore usando el ID de la nota
        val noteDocumentReference = db.collection(CONSTANTES.COLLECTION_NOTES).document(localNote.id)

        try {
            // Eliminas el documento de Firestore
            noteDocumentReference.delete().addOnCompleteListener{
                if (it.isSuccessful){
                    mainActivity.getNotes()
                }
            }.addOnFailureListener{

            }.await()
            //borra
            appDatabase.noteDao().deleteSiEnviadoNotes()
            Log.d("MAR", "Deleted note locally: $localNote")
        } catch (e: Exception) {
            Log.e("MAR", "Error deleting note in Firestore: ${e.message}")
        }
    }




    private fun syncNoteWithFirebase(localNote: NoteEntity) {
        val noteData = hashMapOf(
            "aplicacion" to localNote.aplicacion,
            "fecha" to converterStringAtTimestamp(localNote.fecha),
            "fecha_registro" to localNote.fecha_registro,
            "nota" to localNote.nota,
            "posicion" to geoPointConverter(localNote.posicion),
            "propietario" to localNote.propietario,
        )

        db.collection(CONSTANTES.COLLECTION_NOTES)
            .document(localNote.id)
            .set(noteData)
            .addOnSuccessListener { documentReference ->
                GlobalScope.launch(Dispatchers.IO) {
                    localNote.estado = "SiEnviado"
                    appDatabase.noteDao().update(localNote)
                    Log.d("MAR", "Note synced to Firebase: $localNote")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MAR", "Error syncing note to Firebase: ${e.message}")
            }
    }


    private fun converterStringAtTimestamp(fecha: String): Timestamp {
        val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'Z yyyy", Locale.US)
        try {
            val fechaHoraDate = dateFormat.parse(fecha)
            return if (fechaHoraDate != null) {
                Timestamp(fechaHoraDate)
            } else {
                Timestamp.now()
            }
        } catch (e: ParseException) {
            return Timestamp.now()
        }
    }

    private fun geoPointConverter(position: String): GeoPoint {
        val regex = Regex("[-+]?[0-9]*\\.?[0-9]+")
        val matches = regex.findAll(position)
        val coordinates = matches.map { it.value.toDouble() }.toList()
        return if (coordinates.size == 2) {
            GeoPoint(coordinates[0], coordinates[1])
        } else {
            GeoPoint(0.0, 0.0)
        }
    }
}