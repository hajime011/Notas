package com.example.mynotes.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.GeoPoint

@Entity(tableName = "mynotes")
data class NoteEntity(

    @PrimaryKey
    val id: String,
    val nota: String,
    val aplicacion: String,
    val propietario: String,
    val fecha_registro: String,
    val ubicacion: GeoPoint
)

