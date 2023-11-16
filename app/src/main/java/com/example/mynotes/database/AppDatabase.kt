package com.example.mynotes.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mynotes.database.dao.NoteDao
import com.example.mynotes.database.entity.NoteEntity

@Database(entities = [NoteEntity::class], version = 6)
//@TypeConverters(Converters::class, TimestampConverter::class)
@TypeConverters(Converters::class, TimestampConverter::class)

abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
