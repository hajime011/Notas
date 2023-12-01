package com.example.mynotes.presenter

import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.R
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.entity.NoteEntity
import com.example.mynotes.util.CONSTANTES
import com.example.mynotes.util.UtilidadesRed
import com.example.mynotes.view.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class MyNotesPresenter(private val mainActivity: MainActivity) {


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
                    // borrar la nota en Firebase
                    val firestore = FirebaseFirestore.getInstance()
                    val noteRef = firestore.collection(CONSTANTES.COLLECTION_NOTES).document(id)
                    noteRef.delete().addOnCompleteListener{
                        if(it.isSuccessful){
                            mainActivity.getNotes()
                        }
                    }.addOnFailureListener{
                        Log.e("Error deleting note",it.message.toString())
                    }.await()


                } catch (e: Exception) {
                    Log.e("com.example.mynotes.adapter.NotesAdapter", "Error deleting note: ${e.message}")
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
    fun editarNotas(id: String, nuevaNota: String, mainActivity: MainActivity) {
        if (UtilidadesRed.estaDisponibleRed(mainActivity)) {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    // Actualizar la nota en Firebase
                    val firestore = FirebaseFirestore.getInstance()
                    val noteRef = firestore.collection(CONSTANTES.COLLECTION_NOTES).document(id)
                    noteRef.update("nota", nuevaNota, "estado", "Editado").addOnCompleteListener {
                        if (it.isSuccessful) {
                            mainActivity.getNotes()
                        }
                    }.addOnFailureListener {
                        Log.e("Error updating note", it.message.toString())
                    }.await()
                } catch (e: Exception) {
                    Log.e("com.example.mynotes.adapter.NotesAdapter", "Error updating note: ${e.message}")
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
                noteDao.updateNoteContentAndStateById(id, nuevaNota, "Editado")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        mainActivity,
                        "No hay conexión a Internet. La nota se actualizó localmente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


}
