package com.example.mynotes.database.dao


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mynotes.database.entity.NoteEntity

@Dao
interface NoteDao {
    @Query("SELECT * FROM mynotes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM mynotes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("UPDATE mynotes SET estado = :estadoNuevo WHERE id = :id")
    suspend fun actualizarEstadoPorId(id: String, estadoNuevo: String)


    @Delete
    suspend fun delete(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(noteEntity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(noteEntity: NoteEntity)
    @Query("DELETE FROM mynotes WHERE estado = 'Borrado'")
    suspend fun deleteSiEnviadoNotes()




}