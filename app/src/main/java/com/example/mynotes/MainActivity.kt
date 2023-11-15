package com.example.mynotes
import NotesAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.application.MyNotesApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.example.mynotes.database.AppDatabase
import com.example.mynotes.database.entity.NoteEntity
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private val notesMap: MutableMap<String, Any> = mutableMapOf()

    private val db = Firebase.firestore
    private val email = "pruebasgoo@coordinadora.com"
    private val password = "Coordi2023"
    private lateinit var addNoteButton: Button

    private  lateinit var  notesListView: RecyclerView

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var ubicacionActual: GeoPoint? = null


    private lateinit var appDatabase: AppDatabase

    private var selectedNoteId: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        permisos()
        manageButton()
        sessionFirebase()
        getNotes()
        //loadRoomNotes()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        obtenerYMostrarUbicacionActual()
    }

    private fun setupView() {
        auth = Firebase.auth
        notesListView = findViewById(R.id.notesListView)
        addNoteButton = findViewById(R.id.addNoteButton)
        appDatabase = (application as MyNotesApplication).appDatabase
    }

    private fun sessionFirebase() {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d("TAG", "signInWithEmail:success")
                    val user = auth.currentUser

                } else {
                    // If sign in fails, displzay a message to the user.
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun permisos() {
        // Verificar si tienes permiso para acceder a la ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        } else {
            // Si no tienes permiso, solicita permiso al usuario
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun getList(){
        if(isNetworkAvailable()){
            //verificar la conexión a internet
            // obtener las notas firebase
            db.collection(CONSTANTES.COLLECTION_NOTES).get().addOnCompleteListener {
                if(it.isSuccessful){
                    val lista : List<NoteEntity> = emptyList()
                    for(document in it.result){

                    }
                }
            }.addOnFailureListener {
                Log.i("ERROR ",it.message.toString())
            }
        }else{
            // si no hay conexión offline
            // obtener las notas room
        }
    }
    fun getNotes() {
        if (isNetworkAvailable()) {
            db.collection(CONSTANTES.COLLECTION_NOTES)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val lista : ArrayList<NoteEntity> = ArrayList()
                        appDatabase = (this.application as MyNotesApplication).appDatabase
                        for (document in it.result) {

                            val data = NoteEntity(document.id,
                                document.getString("nota").toString(),
                                "2","3",document.getString("fecha_registro").toString(),"5","6","7")
                            lista.add(data)
                            // ALMACENAS EN ROOM LAS  NOTAS CONSULTADAS
//                            GlobalScope.launch(Dispatchers.IO) {
//                                (appDatabase as AppDatabase).notesDao()!!
//                                    .insertAll(document.toNotes())
//                            }
                        }
                        val adapter = NotesAdapter(lista, this)
                        notesListView.layoutManager = LinearLayoutManager(this)
                        notesListView.adapter = adapter
                    }
                }.addOnFailureListener {
                    Log.i("ERROR", it.message.toString())
                }
        } else {
//            OFFLINE
        }
    }
    public fun obtenerYMostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitud = location.latitude
                        val longitud = location.longitude
                        ubicacionActual = GeoPoint(latitud, longitud)
                        Log.d("MAR", "Ubicación actual - Latitud: $latitud, Longitud: $longitud")
                    } else {
                        Log.w("MAR", "No se pudo obtener la ubicación actual.")
                    }
                }
        } else {
            Log.w("TAG", "No tienes permiso para acceder a la ubicación.")
        }
    }
    private fun loadRoomNotes() {
        GlobalScope.launch(Dispatchers.IO) {
            val roomNotes: List<NoteEntity> = appDatabase.noteDao().getAllNotes()

            runOnUiThread {
                listRoomNotes(roomNotes)
            }
        }
    }

    private fun listRoomNotes(roomNotes: List<NoteEntity>) {
        val adapter = NotesAdapter(roomNotes, this)
        notesListView.layoutManager = LinearLayoutManager(this)
        notesListView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadRoomNotes()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun listNotes() {
        // Asegúrate de asignar un valor a selectedNoteId antes de este punto
        if (selectedNoteId != null) {
            Log.d("MainActivity", "Selected Note ID: $selectedNoteId")
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra("noteId", selectedNoteId)
            startActivity(intent)
        }
    }

    public fun geoPointConverter(position : String) : GeoPoint{
        val regex = Regex("[-+]?[0-9]*\\.?[0-9]+")
        val matches = regex.findAll(position)
        val coordinates = matches.map { it.value.toDouble() }.toList()
        return if (coordinates.size == 2) {
            GeoPoint(coordinates[0], coordinates[1])
        } else {
            GeoPoint(0.0, 0.0)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun converterStringAtTimestamp(fecha : String) : Timestamp {
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




    private fun manageButton() {
        addNoteButton.setOnClickListener {
            val intent = Intent(this, CrearNotas::class.java)
            val ubicacionString = ubicacionActual?.latitude.toString() + "," + ubicacionActual?.longitude.toString()
            intent.putExtra("ubicacion", ubicacionString)  // Pasar la ubicación como cadena//@PrimaryKey(autoGenerate = true)
            startActivity(intent)

            // Después de agregar una nueva nota, actualiza la lista y notifica al adaptador
            loadRoomNotes()
        }
    }

}