package com.example.mynotes.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

@Entity(tableName = "mynotes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    val nota: String,
    val aplicacion: String,
    val propietario: String,
    val fecha_registro: String,
    val fechaActual: Timestamp,
    val ubicacion: GeoPoint,
    val estado:String

)

