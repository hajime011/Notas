import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.EditNoteActivity
import com.example.mynotes.R
import com.example.mynotes.database.entity.NoteEntity

class NotesAdapter(private val notesList: List<NoteEntity>, private val context: Context) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteTitle: TextView = itemView.findViewById(R.id.noteTitle)
        val noteContent: TextView = itemView.findViewById(R.id.noteContent)

        init {
            // Agregar un OnClickListener a la vista de CardView
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Obtener la ID de la nota seleccionada según la posición
                    val selectedNoteId = notesList[position].id
                    Log.d("NotesAdapter", "Selected Note ID: $selectedNoteId")

                    val intent = Intent(context, EditNoteActivity::class.java)
                    intent.putExtra("noteId", selectedNoteId)
                    context.startActivity(intent)
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
