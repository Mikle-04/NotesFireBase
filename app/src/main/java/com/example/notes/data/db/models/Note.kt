package com.example.notes.data.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    @PropertyName("id") val id: Int = 0, // Локальный ID для Room
    @PropertyName("firestoreId") var firestoreId: String = "", // Уникальный ID для Firestore
    @PropertyName("title") val title: String = "",
    @PropertyName("content") val content: String = "",
    @PropertyName("category") val category: String = "",
    @PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @PropertyName("synced") val synced: Boolean = false
)
