package com.example.mynotes.presenter

import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.util.CONSTANTES
import com.example.mynotes.util.UtilidadesRed
import com.example.mynotes.view.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MyNotesPresenter {

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
}
