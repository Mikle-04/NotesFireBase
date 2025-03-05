package com.example.notes.data.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id : Int = 0,
    val title: String,
    val content: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false // Флаг синхронизации с Firestore
)
