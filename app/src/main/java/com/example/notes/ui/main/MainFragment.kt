package com.example.notes.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.R
import com.example.notes.data.db.AppDatabase
import com.example.notes.data.db.dao.NoteDao
import com.example.notes.data.db.models.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainFragment : Fragment() {

    private lateinit var noteDao: NoteDao
    private lateinit var adapter: NoteAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Room
        val db = AppDatabase.getDatabase(requireContext())
        noteDao = db.noteDao()

        // Настройка RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.notes_recycler_view)
        adapter = NoteAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Наблюдение за заметками
        noteDao.getAllNotes().observe(viewLifecycleOwner, Observer { notes ->
            adapter.updateNotes(notes ?: emptyList())
        })

        // Кнопка добавления
        view.findViewById<View>(R.id.add_button).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_editNoteFragment)
        }

        syncWithFirestore()
    }
    private fun syncWithFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Загрузка заметок из Firestore
                val firestoreNotes = firestore.collection("users")
                    .document(userId)
                    .collection("notes")
                    .get()
                    .await()
                    .toObjects(Note::class.java)

                Log.d("MainFragment", "Firestore notes received: ${firestoreNotes.size}")

                // Синхронизация: добавление всех заметок из Firestore в Room
                firestoreNotes.forEach { firestoreNote ->
                    val existingNote = noteDao.getAllNotes().value?.find { it.id == firestoreNote.id }
                    if (existingNote == null) {
                        // Добавляем новую заметку
                        noteDao.insert(firestoreNote)
                    } else if (existingNote.timestamp < firestoreNote.timestamp) {
                        // Обновляем, если Firestore-версия новее
                        noteDao.update(firestoreNote)
                    }
                }

                // Отправка несинхронизированных локальных заметок в Firestore
                val unsyncedNotes = noteDao.getUnsyncedNotes()
                unsyncedNotes.forEach { note ->
                    val noteMap = hashMapOf(
                        "id" to note.id,
                        "title" to note.title,
                        "content" to note.content,
                        "category" to note.category,
                        "timestamp" to note.timestamp,
                        "synced" to true
                    )
                    firestore.collection("users")
                        .document(userId)
                        .collection("notes")
                        .document(note.id.toString())
                        .set(noteMap)
                        .await()
                    noteDao.update(note.copy(synced = true))
                }

                // Логирование для отладки
                Log.d("MainFragment", "Sync completed successfully")

            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка синхронизации: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("MainFragment", "Sync failed: ${e.message}")
            }
        }
    }
}