package com.example.mynotes.presenter

import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.R
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.AppDatabase
import com.example.mynotes.database.entity.NoteEntity
import com.example.mynotes.util.CONSTANTES
import com.example.mynotes.util.UtilidadesRed
import com.example.mynotes.view.MainActivity
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

class MyNotesPresenter(private val mainActivity: MainActivity) {
    private val db = com.google.firebase.ktx.Firebase.firestore
    private lateinit var appDatabase: AppDatabase
    fun sincronizarNotasConFirestore() {
        GlobalScope.launch(Dispatchers.IO) {
            val localNotes = (mainActivity.application as MyNotesApplication).appDatabase.noteDao().getAllNotes()

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


    fun mostrarDialogoCrearNotas() {
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
            val posicion = mainActivity.ubicacionActual
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
                            (mainActivity.application as MyNotesApplication).appDatabase.noteDao().insert(noteEntity)
                            mainActivity.getNotes()
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
                    (mainActivity.application as MyNotesApplication).appDatabase.noteDao().insert(noteEntity)
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
                    // Borrar la nota en Firebase
                    val firestore = FirebaseFirestore.getInstance()
                    val noteRef = firestore.collection(CONSTANTES.COLLECTION_NOTES).document(id)

                    noteRef.delete().addOnCompleteListener {
                        if (it.isSuccessful) {
                            // Borrar la nota localmente
                            val myNotesApplication = mainActivity.applicationContext as MyNotesApplication
                            val noteDao = myNotesApplication.appDatabase.noteDao()
                            GlobalScope.launch(Dispatchers.IO) {
                                noteDao.deleteById(id)
                            }

                            // Actualizar la lista de notas en la interfaz de usuario
                            mainActivity.getNotes()
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
            val myNotesApplication = mainActivity.applicationContext as MyNotesApplication
            val noteDao = myNotesApplication.appDatabase.noteDao()
            GlobalScope.launch(Dispatchers.Main) {
                noteDao.actualizarEstadoPorId(id,CONSTANTES.ESTADO_BORRADO)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        mainActivity,
                        "No hay conexión a Internet. La nota se eliminó localmente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    }
    fun mostrarDialogoEditarNotas(notaAEditar: NoteEntity) {
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
                        noteRef.update("nota", nuevaNota, "estado", "Editado").addOnCompleteListener {
                            if (it.isSuccessful) {
                                mainActivity.getNotes()
                            }
                        }.addOnFailureListener {
                            Log.e("Error updating note", it.message.toString())
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
                val myNotesApplication = mainActivity.applicationContext as MyNotesApplication
                val noteDao = myNotesApplication.appDatabase.noteDao()
                GlobalScope.launch(Dispatchers.Main) {
                    noteDao.updateNoteContentAndStateById(notaAEditar.id, nuevaNota, "Editado")

                    withContext(Dispatchers.Main) {
                        mainActivity.loadRoomNotes()
                        Toast.makeText(
                            mainActivity,
                            "No hay conexión a Internet. La nota se actualizó localmente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            dialog.dismiss()
        }

        dialog.show()
    }


}
