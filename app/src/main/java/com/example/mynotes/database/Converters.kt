package com.example.mynotes.database
import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun fromGeoPoint(geoPoint: GeoPoint): String {
        val gson = Gson()
        return gson.toJson(geoPoint)
    }

    @TypeConverter
    fun toGeoPoint(json: String): GeoPoint {
        val gson = Gson()
        return gson.fromJson(json, GeoPoint::class.java)
    }
}

