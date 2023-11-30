package com.example.mynotes.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mynotes.R
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.dao.NoteDao
import com.example.mynotes.database.entity.NoteEntity
import com.example.mynotes.util.UtilidadesRed
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditNoteActivity : AppCompatActivity() {

    private lateinit var guardarCambiosButton: Button
    private lateinit var notaEditarEditText: EditText

    private lateinit var noteDao: NoteDao
    private lateinit var noteId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)
        val myNotesApplication = application as MyNotesApplication
        noteDao = myNotesApplication.appDatabase.noteDao()

        setupViews()
        loadLocalNote()

    }

    private fun setupViews() {
        guardarCambiosButton = findViewById(R.id.EditarNotaButton)
        notaEditarEditText = findViewById(R.id.notaEditar)
        noteId = intent.getStringExtra("noteId").toString()
    }

    private fun loadLocalNote() {
        GlobalScope.launch(Dispatchers.IO) {
            val localNote = noteDao.getNoteById(noteId)
            if (localNote != null) {
                withContext(Dispatchers.Main) {
                    notaEditarEditText.setText(localNote.nota)
                    setupGuardarCambiosButton(localNote)
                }
            }
        }
    }

    private fun setupGuardarCambiosButton(localNote: NoteEntity) {
        guardarCambiosButton.setOnClickListener {

            val nuevaNota = notaEditarEditText.text.toString()

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val updatedLocalNote = localNote.copy(nota = nuevaNota, estado = "Editado")
                    noteDao.update(updatedLocalNote)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EditNoteActivity,
                            "Cambios guardados localmente.",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (UtilidadesRed.estaDisponibleRed(this@EditNoteActivity)) {
                            updateNoteInFirestore(updatedLocalNote)
                        } else {
                            navigateToMainActivity()
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("Maracuya", "Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun updateNoteInFirestore(updatedLocalNote: NoteEntity) {
        val db = FirebaseFirestore.getInstance()
        val noteRef = db.collection("MyNotes").document(updatedLocalNote.id)

        noteRef.update("nota", updatedLocalNote.nota)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(
                        this@EditNoteActivity,
                        "Cambios guardados en Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMainActivity()
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    Toast.makeText(
                        this@EditNoteActivity,
                        "Error al guardar cambios en Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMainActivity()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
