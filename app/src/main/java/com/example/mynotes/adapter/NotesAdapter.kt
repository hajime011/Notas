package com.example.mynotes.adapter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.R
import com.example.mynotes.database.entity.NoteEntity
import com.example.mynotes.presenter.MyNotesPresenter
import com.example.mynotes.view.MainActivity

class NotesAdapter(private val notesList: MutableList<NoteEntity>, private val mainActivity: MainActivity) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {
    private var myNotesPresenter = MyNotesPresenter(mainActivity)
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteTitle: TextView = itemView.findViewById(R.id.noteTitle)
        val noteContent: TextView = itemView.findViewById(R.id.noteContent)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)


        init {
            editButton.setOnClickListener {
                val editedNoteContent = noteContent.text.toString()
                myNotesPresenter.editarNotas(notesList[adapterPosition].id, editedNoteContent, mainActivity)
            }




            deleteButton.setOnClickListener {


                myNotesPresenter.borrarNotas(notesList[position].id,mainActivity)
            }


        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(mainActivity).inflate(R.layout.item_note, parent, false)
        return ViewHolder(itemView)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notesList[position]
        val positionNotes = position + 1


        Log.d("Hola", "onBindViewHolder called for position $position")


        holder.noteTitle.text = "Nota $positionNotes"
        holder.noteContent.text = note.nota
    }




    override fun getItemCount(): Int {
        return notesList.size
    }
}
