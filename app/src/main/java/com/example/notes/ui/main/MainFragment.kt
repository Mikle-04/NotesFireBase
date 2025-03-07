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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainFragment : Fragment() {

    private lateinit var noteDao: NoteDao
    private lateinit var adapter: NoteAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var firestoreListener: ListenerRegistration? = null
    private lateinit var googleSignInClient: GoogleSignInClient

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

        // Настройка Google Sign-In для выхода
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Настройка RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.notes_recycler_view)
        adapter = NoteAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Наблюдение за локальными заметками
        noteDao.getAllNotes().observe(viewLifecycleOwner, Observer { notes ->
            Log.d("MainFragment", "Local notes updated: ${notes?.size ?: "null"}")
            adapter.updateNotes(notes?.distinctBy { it.firestoreId } ?: emptyList()) // Убираем дубли по firestoreId
        })

        view.findViewById<View>(R.id.add_button).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_editNoteFragment)
        }
        // Кнопка выхода
        view.findViewById<View>(R.id.logout_button).setOnClickListener {
            signOut()
        }

        syncWithFirestore()
    }

    private fun syncWithFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Real-time слушатель
        firestoreListener = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainFragment", "Firestore listener error: ${error.message}")
                    Toast.makeText(requireContext(), "Ошибка слушателя: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val firestoreNotes = snapshot.documents.map { doc ->
                            doc.toObject(Note::class.java)!!.copy(firestoreId = doc.id)
                        }
                        Log.d("MainFragment", "Firestore real-time update: ${firestoreNotes.size}")

                        firestoreNotes.forEach { firestoreNote ->
                            val existingNote = noteDao.getNoteByFirestoreId(firestoreNote.firestoreId)
                            if (existingNote == null) {
                                Log.d("MainFragment", "Inserting new note: ${firestoreNote.firestoreId}")
                                noteDao.insert(firestoreNote)
                            } else if (existingNote.timestamp < firestoreNote.timestamp) {
                                Log.d("MainFragment", "Updating note: ${firestoreNote.firestoreId}")
                                noteDao.update(firestoreNote.copy(id = existingNote.id))
                            } else {
                                Log.d("MainFragment", "Skipping unchanged note: ${firestoreNote.firestoreId}")
                            }
                        }

                        syncUnsyncedNotes(userId)
                    }
                }
            }
    }

    private suspend fun syncUnsyncedNotes(userId: String) {
        val unsyncedNotes = noteDao.getUnsyncedNotes()
        unsyncedNotes.forEach { note ->
            if (note.firestoreId.isBlank()) {
                Log.w("MainFragment", "Skipping note with empty firestoreId: $note")
                return@forEach // Пропускаем заметки без firestoreId
            }

            val noteMap = hashMapOf(
                "id" to note.id,
                "firestoreId" to note.firestoreId,
                "title" to note.title,
                "content" to note.content,
                "category" to note.category,
                "timestamp" to note.timestamp,
                "synced" to true
            )
            Log.d("MainFragment", "Syncing note to path: users/$userId/notes/${note.firestoreId}")
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.firestoreId)
                .set(noteMap)
                .await()
            noteDao.update(note.copy(synced = true))
        }
    }
    private fun signOut() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Выход из Firebase
                auth.signOut()
                // Выход из Google
                googleSignInClient.signOut().await()
                // Переход на экран входа
                findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
                Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка выхода: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        firestoreListener = null
    }
}