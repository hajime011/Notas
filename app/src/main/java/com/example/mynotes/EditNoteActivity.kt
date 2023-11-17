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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditNoteActivity : AppCompatActivity() {

    private lateinit var guardarCambiosButton: Button
    private lateinit var notaEditarEditText: EditText

    private lateinit var noteDao: NoteDao
    private lateinit var noteRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)
        val myNotesApplication = application as MyNotesApplication
        noteDao = myNotesApplication.appDatabase.noteDao()

        setupViews()
        val noteId = intent.getStringExtra("noteId")
        if (noteId != null) {
            noteRef = FirebaseFirestore.getInstance().collection("MyNotes").document(noteId)
            retrieveAndPopulateNoteData(noteRef)
        }
    }

    private fun setupViews() {
        guardarCambiosButton = findViewById(R.id.EditarNotaButton)
        notaEditarEditText = findViewById(R.id.notaEditar)
    }

    private fun retrieveAndPopulateNoteData(noteRef: DocumentReference) {
        noteRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val nota = document.getString("nota")
                notaEditarEditText.setText(nota)
                setupGuardarCambiosButton(noteRef)
            }
        }
    }

    private fun setupGuardarCambiosButton(noteRef: DocumentReference) {
        guardarCambiosButton.setOnClickListener {

            val nuevaNota = notaEditarEditText.text.toString()

            // Verificar la disponibilidad de la red
            if (UtilidadesRed.estaDisponibleRed(this@EditNoteActivity)) {
                // Actualizar la nota localmente
                GlobalScope.launch(Dispatchers.IO) {
                    val localNote = noteDao.getNoteById(noteRef.id)
                    if (localNote != null) {
                        val updatedLocalNote = localNote.copy(nota = nuevaNota)
                        noteDao.update(updatedLocalNote)
                        Log.d("MAR", "Local note updated: $updatedLocalNote")

                        // Actualizar la nota en Firebase Firestore
                        noteRef.update("nota", nuevaNota)
                            .addOnSuccessListener {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@EditNoteActivity,
                                        "La nota se ha editado correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navigateToMainActivity()
                                }
                            }
                            .addOnFailureListener {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@EditNoteActivity,
                                        "Error al editar la nota en Firebase",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
            } else {
                // No hay conexión a Internet
                // Editar la nota localmente
                GlobalScope.launch(Dispatchers.IO) {
                    val localNote = noteDao.getNoteById(noteRef.id)
                    if (localNote != null) {
                        val updatedLocalNote = localNote.copy(nota = nuevaNota)
                        noteDao.update(updatedLocalNote)
                        Log.d("MAR", "Local note updated: $updatedLocalNote")
                        runOnUiThread {
                            Toast.makeText(
                                this@EditNoteActivity,
                                "No hay conexión a Internet. Los cambios se guardarán localmente.",
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMainActivity()
                        }
                    }
                }
            }
        }
    }


    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
