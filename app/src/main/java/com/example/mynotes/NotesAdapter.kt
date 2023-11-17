import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.EditNoteActivity
import com.example.mynotes.R
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.entity.NoteEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotesAdapter(private val notesList: MutableList<NoteEntity>, private val context: Context) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteTitle: TextView = itemView.findViewById(R.id.noteTitle)
        val noteContent: TextView = itemView.findViewById(R.id.noteContent)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        init {
            editButton.setOnClickListener {
                // Acción para editar la nota
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val selectedNoteId = notesList[position].id

                    // Iniciar la actividad de edición
                    val intent = Intent(context, EditNoteActivity::class.java)
                    intent.putExtra("noteId", selectedNoteId)
                    context.startActivity(intent)
                }
            }


            deleteButton.setOnClickListener {
                // Acción para borrar la nota
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val selectedNote = notesList[position]

                    // Borrar la nota en Firebase
                    val firestore = FirebaseFirestore.getInstance()
                    val noteRef = firestore.collection("MyNotes").document(selectedNote.id)

                    GlobalScope.launch(Dispatchers.Main) {
                        try {
                            // Elimina la nota en Firebase
                            noteRef.delete().await()

                            // Elimina la nota local
                            val myNotesApplication = context.applicationContext as MyNotesApplication
                            val noteDao = myNotesApplication.appDatabase.noteDao()
                            noteDao.delete(selectedNote)

                            withContext(Dispatchers.Main) {
                                notesList.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, itemCount)
                            }
                        } catch (e: Exception) {
                            Log.e("NotesAdapter", "Error deleting note: ${e.message}")
                        }
                    }
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notesList[position]
        val positionNotes = position + 1

        Log.d("NotesAdapter", "onBindViewHolder called for position $position")

        val registrationDate = note.fecha_registro
        val content = note.nota

        val formattedNote = "Fecha de Registro: $registrationDate\nNota: $content"

        holder.noteTitle.text = "Nota $positionNotes"
        holder.noteContent.text = formattedNote
    }


    override fun getItemCount(): Int {
        return notesList.size
    }
}
