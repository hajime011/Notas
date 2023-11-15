package com.example.mynotes.database
import androidx.room.TypeConverter
import com.google.firebase.Timestamp

class TimestampConverter {
    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp): Long {
        return timestamp.seconds
    }

    @TypeConverter
    fun toTimestamp(seconds: Long): Timestamp {
        return Timestamp(seconds, 0)
    }
}

