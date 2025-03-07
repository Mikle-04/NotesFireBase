package com.example.notes.ui.editNote

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
    private val args: EditNoteFragmentArgs by navArgs()

    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private lateinit var spinner: Spinner
    private lateinit var saveProgress: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteDao = AppDatabase.getDatabase(requireContext()).noteDao()

        titleEdit = view.findViewById(R.id.title_edit)
        contentEdit = view.findViewById(R.id.content_edit)
        spinner = view.findViewById(R.id.category_spinner)
        saveProgress = view.findViewById(R.id.save_progress)

        val categories = arrayOf("Работа", "Личное", "Идеи", "Другое")
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        val noteId = args.noteId
        if (noteId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    launch(Dispatchers.Main) {
                        titleEdit.setText(note.title)
                        contentEdit.setText(note.content)
                        spinner.setSelection(categories.indexOf(note.category))
                    }
                }
            }
        }

        view.findViewById<View>(R.id.save_button).setOnClickListener {
            val title = titleEdit.text.toString()
            val content = contentEdit.text.toString()
            val category = spinner.selectedItem.toString()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                if (noteId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val existingNote = noteDao.getNoteById(noteId)
                        if (existingNote != null) {
                            // Проверяем, были ли изменения
                            if (existingNote.title != title || existingNote.content != content || existingNote.category != category) {
                                val updatedNote = existingNote.copy(
                                    title = title,
                                    content = content,
                                    category = category,
                                    timestamp = System.currentTimeMillis(),
                                    synced = false
                                )
                                saveNote(updatedNote)
                            } else {
                                launch(Dispatchers.Main) {
                                    findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                                    Toast.makeText(requireContext(), "Изменений нет", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    val note = Note(title = title, content = content, category = category)
                    saveNote(note)
                }
            } else {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNote(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                launch(Dispatchers.Main) {
                    saveProgress.visibility = View.VISIBLE
                }

                val userId = auth.currentUser?.uid ?: return@launch
                val firestoreId = if (note.firestoreId.isBlank()) {
                    val firestoreRef = firestore.collection("users")
                        .document(userId)
                        .collection("notes")
                        .document()
                    firestoreRef.id
                } else {
                    note.firestoreId
                }

                val noteWithFirestoreId = note.copy(firestoreId = firestoreId)
                noteDao.insert(noteWithFirestoreId)

                val noteMap = hashMapOf(
                    "id" to noteWithFirestoreId.id,
                    "firestoreId" to firestoreId,
                    "title" to note.title,
                    "content" to note.content,
                    "category" to note.category,
                    "timestamp" to note.timestamp,
                    "synced" to true
                )
                firestore.collection("users")
                    .document(userId)
                    .collection("notes")
                    .document(firestoreId)
                    .set(noteMap)
                    .await()

                noteDao.update(noteWithFirestoreId.copy(synced = true))

                launch(Dispatchers.Main) {
                    saveProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Заметка сохранена", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    saveProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}