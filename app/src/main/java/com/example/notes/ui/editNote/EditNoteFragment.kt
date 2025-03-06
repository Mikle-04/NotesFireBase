package com.example.notes.ui.editNote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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


class EditNoteFragment : Fragment() {

    private lateinit var noteDao: NoteDao
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Room
        noteDao = AppDatabase.getDatabase(requireContext()).noteDao()

        // Настройка Spinner с категориями
        val spinner = view.findViewById<Spinner>(R.id.category_spinner)
        val categories = arrayOf("Работа", "Личное", "Идеи", "Другое")
        spinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        // Кнопка сохранения
        view.findViewById<View>(R.id.save_button).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.title_edit).text.toString()
            val content = view.findViewById<EditText>(R.id.content_edit).text.toString()
            val category = spinner.selectedItem.toString()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                val note = Note(title = title, content = content, category = category)
                saveNote(note)
            }
        }
    }

    private fun saveNote(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //Сохранение в room
                noteDao.insert(note)

                //Отправка в Firestore
                val userId = auth.currentUser?.uid ?: return@launch
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

                // Обновление флага synced в Room
                val syncedNote = note.copy(synced = true)
                noteDao.update(syncedNote)

                // Переход на MainFragment
                launch(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка сохранения: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}