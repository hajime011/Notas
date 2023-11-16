package com.example.mynotes.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

@Entity(tableName = "mynotes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val aplicacion: String,
    val fecha: String,
    val fecha_registro: String,
    val nota: String,
    val posicion: GeoPoint,
    val propietario: String,
    //val estado : String

)

