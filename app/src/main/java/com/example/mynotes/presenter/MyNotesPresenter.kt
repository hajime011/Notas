    package com.example.mynotes.presenter

    import android.Manifest
    import android.content.Context
    import android.content.pm.PackageManager
    import android.util.Log
    import android.widget.Button
    import android.widget.EditText
    import android.widget.Toast
    import androidx.appcompat.app.AlertDialog
    import androidx.core.app.ActivityCompat
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.mynotes.R
    import com.example.mynotes.adapter.NotesAdapter
    import com.example.mynotes.application.MyNotesApplication
    import com.example.mynotes.database.AppDatabase
    import com.example.mynotes.database.entity.NoteEntity
    import com.example.mynotes.util.CONSTANTES
    import com.example.mynotes.util.UtilidadesRed
    import com.example.mynotes.view.MainActivity
    import com.google.android.gms.location.FusedLocationProviderClient
    import com.google.android.gms.location.LocationServices
    import com.google.firebase.Firebase
    import com.google.firebase.Timestamp
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.GeoPoint
    import com.google.firebase.firestore.firestore
    import com.google.firebase.firestore.ktx.firestore
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.GlobalScope
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await
    import kotlinx.coroutines.withContext
    import java.text.ParseException
    import java.text.SimpleDateFormat
    import java.util.Locale
    import java.util.UUID
    import javax.inject.Inject

    class MyNotesPresenter(val context: Context) {
        private val db = com.google.firebase.ktx.Firebase.firestore
        var ubicacionActual: GeoPoint? = null
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private lateinit var notesListView: RecyclerView
        @Inject
        lateinit var appDatabase: AppDatabase

        init {
            ( context.applicationContext as MyNotesApplication).getMyNotesComponent().inject(this)
        }



        fun cargarRoomNotes(mainActivity: MainActivity) {
            GlobalScope.launch(Dispatchers.IO) {
                val roomNotes: List<NoteEntity> =appDatabase.noteDao().getAllNotes()
                mainActivity.runOnUiThread {
                    listarRoomNotes(roomNotes,mainActivity)
                }
            }
        }

        fun listarRoomNotes(roomNotes: List<NoteEntity>,mainActivity: MainActivity) {
            // Filtrar las notas que no están borradas
            val notasFiltradas = roomNotes.filter { it.estado != CONSTANTES.ESTADO_BORRADO }

            val adapter = NotesAdapter(notasFiltradas.toMutableList(), mainActivity)
            mainActivity.notesListView.layoutManager = LinearLayoutManager(mainActivity)
            mainActivity.notesListView.adapter = adapter
            adapter.notifyDataSetChanged()

        }


        fun obtenerYMostrarUbicacionActual(mainActivity: MainActivity) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity)

            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

        fun getNotes(mainActivity: MainActivity) {
            if (UtilidadesRed.estaDisponibleRed(mainActivity)) {
                db.collection(CONSTANTES.COLLECTION_NOTES)
                    .get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val lista: ArrayList<NoteEntity> = ArrayList()
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
                                    appDatabase.noteDao().insert(data)
                                }
                            }
                            mainActivity.loadRoomNotes() // Actualizar la interfaz de usuario
                        }
                    }.addOnFailureListener {
                        Log.i("ERROR", it.message.toString())
                    }
            } else {
                mainActivity.loadRoomNotes() // Actualizar la interfaz de usuario
            }
        }
        fun sincronizarNotasConFirestore() {
            GlobalScope.launch(Dispatchers.IO) {
                val localNotes = appDatabase.noteDao().getAllNotes()

                for (localNote in localNotes) {
                    when (localNote.estado) {
                        "Borrado" -> {
                            syncNoteDeleteFirebase(localNote)
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
        private suspend fun syncNoteDeleteFirebase(localNote: NoteEntity) {
            // Aquí obtienes la referencia del documento en Firestore usando el ID de la nota
            val noteDocumentReference = db.collection(CONSTANTES.COLLECTION_NOTES).document(localNote.id)

            try {
                // Eliminas el documento de Firestore
                noteDocumentReference.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Borrar la nota localmente después de confirmar el borrado en Firestore
                        GlobalScope.launch(Dispatchers.IO) {
                            appDatabase.noteDao().deleteNoteByIdAndState(localNote.id, CONSTANTES.ESTADO_BORRADO)
                            Log.d("MAR", "Deleted note locally: $localNote")
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("MAR", "Error deleting note in Firestore: ${e.message}")
                }.await()
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


        fun mostrarDialogoCrearNotas(mainActivity: MainActivity) {
            val builder = AlertDialog.Builder(mainActivity)
            val inflater = mainActivity.layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_crear_notas, null)
            builder.setView(dialogView)

            val nuevaNotaEditText = dialogView.findViewById<EditText>(R.id.nuevaNotaEditText)
            val guardarNotaButton = dialogView.findViewById<Button>(R.id.guardarNotaButton)

            val dialog = builder.create()

            guardarNotaButton.setOnClickListener {
                val nuevaNota = nuevaNotaEditText.text.toString()
                val aplicacion = "Mobil"
                val propietario = "Cristian D"
                val fecha = Timestamp.now()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                obtenerYMostrarUbicacionActual(mainActivity)
                val posicion = ubicacionActual ?: GeoPoint(0.0, 0.0)
                val fecha_registro = dateFormat.format(fecha.toDate())

                if (UtilidadesRed.estaDisponibleRed(mainActivity)) {
                    val notas = hashMapOf(
                        "aplicacion" to aplicacion,
                        "fecha" to fecha,
                        "fecha_registro" to fecha_registro,
                        "nota" to nuevaNota,
                        "posicion" to posicion,
                        "propietario" to propietario
                    )

                    Firebase.firestore.collection(CONSTANTES.COLLECTION_NOTES)
                        .add(notas)
                        .addOnSuccessListener { documentReference ->
                            val firestoreId = documentReference.id
                            Log.d("TAG", "Documento agregado con ID: $firestoreId")
                            Toast.makeText(mainActivity, "La nota se ha creado correctamente", Toast.LENGTH_SHORT).show()

                            GlobalScope.launch {
                                val noteEntity = NoteEntity(
                                    id = firestoreId,
                                    nota = nuevaNota,
                                    aplicacion = aplicacion,
                                    propietario = propietario,
                                    fecha_registro = fecha_registro,
                                    fecha = fecha.toString(),
                                    posicion = posicion.toString(),
                                    estado = "SiEnviado"
                                )
                                /*(mainActivity.application as MyNotesApplication).appDatabase.noteDao().insert(noteEntity)*/
                                getNotes(mainActivity)
                            }
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Log.w("TAG", "Error al agregar el documento", e)
                            Toast.makeText(mainActivity, "Error al agregar la nota en Firebase", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Almacenar la nota localmente si no hay conexión
                    val localNoteId = UUID.randomUUID().toString()

                    val noteEntity = NoteEntity(
                        id = localNoteId,
                        nota = nuevaNota,
                        aplicacion = aplicacion,
                        propietario = propietario,
                        fecha_registro = fecha_registro,
                        fecha = fecha.toString(),
                        posicion = posicion.toString(),
                        estado = "NoEnviado"
                    )

                    GlobalScope.launch {
                        appDatabase.noteDao().insert(noteEntity)
                        Log.d("MAR", "Local note inserted: $noteEntity")
                        withContext(Dispatchers.Main) {
                            mainActivity.loadRoomNotes()
                        }
                    }

                    Toast.makeText(mainActivity, "La nota se ha guardado localmente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            dialog.show()
        }

        fun borrarNotas(id:String,mainActivity: MainActivity){

            if (UtilidadesRed.estaDisponibleRed(mainActivity)) {
                GlobalScope.launch(Dispatchers.Main) {
                    try {

                        val firestore = FirebaseFirestore.getInstance()
                        val noteRef = firestore.collection(CONSTANTES.COLLECTION_NOTES).document(id)

                        noteRef.delete().addOnCompleteListener {
                            if (it.isSuccessful) {
                                val noteDao = appDatabase.noteDao()
                                GlobalScope.launch(Dispatchers.IO) {
                                    noteDao.deleteById(id)
                                }
                                    getNotes(mainActivity)
                            }
                        }.addOnFailureListener {
                            Log.e("Error deleting note", it.message.toString())
                        }.await()
                    } catch (e: Exception) {
                        Log.e("Error deleting note", e.message.toString())
                        Toast.makeText(
                            mainActivity,
                            "Error al eliminar la nota. Verifica tu conexión a Internet",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {

                //borrar la nota localmente
                val noteDao = appDatabase.noteDao()
                GlobalScope.launch(Dispatchers.Main) {
                    noteDao.actualizarEstadoPorId(id,CONSTANTES.ESTADO_BORRADO)

                    withContext(Dispatchers.Main) {
                        mainActivity.loadRoomNotes()
                        Toast.makeText(
                            mainActivity,
                            "No hay conexión a Internet. La nota se eliminó localmente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }
        fun mostrarDialogoEditarNotas(notaAEditar: NoteEntity,mainActivity: MainActivity) {

            val builder = AlertDialog.Builder(mainActivity)
            val inflater = mainActivity.layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_editar_notas, null)
            builder.setView(dialogView)

            val nuevaNotaEditText = dialogView.findViewById<EditText>(R.id.nuevaNotaEditText)
            val guardarNotaButton = dialogView.findViewById<Button>(R.id.guardarNotaButton)

            // Establecer el texto del EditText con la nota existente
            nuevaNotaEditText.setText(notaAEditar.nota)

            val dialog = builder.create()

            guardarNotaButton.setOnClickListener {
                val nuevaNota = nuevaNotaEditText.text.toString()

                if (UtilidadesRed.estaDisponibleRed(mainActivity)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        try {
                            // Actualizar la nota en Firebase
                            val firestore = FirebaseFirestore.getInstance()
                            val noteRef = firestore.collection(CONSTANTES.COLLECTION_NOTES).document(notaAEditar.id)
                            noteRef.update("nota", nuevaNota, "estado", "Editado").addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Obtener y mostrar las notas después de editar la nota en Firebase
                                    getNotes(mainActivity)

                                    //local
                                    val noteDao = appDatabase.noteDao()
                                    GlobalScope.launch(Dispatchers.IO) {
                                        noteDao.updateNoteContentAndStateById(notaAEditar.id, nuevaNota, "Editado")
                                    }
                                } else {
                                    Log.e("Error updating note", task.exception?.message.toString())
                                    Toast.makeText(
                                        mainActivity,
                                        "Error al actualizar la nota en Firebase",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }.await()
                        } catch (e: Exception) {
                            Log.e("Error updating note", e.message.toString())
                            Toast.makeText(
                                mainActivity,
                                "Error al actualizar la nota. Verifica tu conexión a Internet",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Actualizar la nota localmente
                    val noteDao =appDatabase.noteDao()
                    GlobalScope.launch(Dispatchers.Main) {
                        noteDao.updateNoteContentAndStateById(notaAEditar.id, nuevaNota, "Editado")

                        // Obtener y mostrar las notas después de editar la nota localmente
                        mainActivity.loadRoomNotes()

                        Toast.makeText(
                            mainActivity,
                            "No hay conexión a Internet. La nota se actualizó localmente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                dialog.dismiss()
            }

            dialog.show()
        }


    }
