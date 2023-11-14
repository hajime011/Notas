package com.example.mynotes.application

import android.app.Application
import androidx.room.Room
import com.example.mynotes.database.AppDatabase

class MyNotesApplication : Application() {
    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "notes-database"
        ).fallbackToDestructiveMigration().build()
    }
}

