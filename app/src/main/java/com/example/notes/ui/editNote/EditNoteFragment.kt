package com.example.notes.ui.editNote

import android.app.AlertDialog
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
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
    private lateinit var deleteButton: View

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
        saveProgress = view.findViewById(R.id.save_progress)
        deleteButton = view.findViewById(R.id.delete_button)

        val categories = arrayOf("Работа", "Личное", "Идеи", "Другое")
        val spinner = view.findViewById<MaterialAutoCompleteTextView>(R.id.category_spinner)
        spinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))

        val noteId = args.noteId
        if (noteId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    launch(Dispatchers.Main) {
                        titleEdit.setText(note.title)
                        contentEdit.setText(note.content)
                        spinner.setSelection(categories.indexOf(note.category))
                        deleteButton.visibility = View.VISIBLE
                    }
                }else{
                    deleteButton.visibility = View.GONE
                }
            }
        }

        view.findViewById<View>(R.id.save_button).setOnClickListener {
            val title = titleEdit.text.toString()
            val content = contentEdit.text.toString()
            val category = spinner.text.toString()

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
        deleteButton.setOnClickListener {
            if (noteId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    val note = noteDao.getNoteById(noteId)
                    if (note != null) {
                        deleteNote(note)
                    }
                }
            }
        }
        // Кнопка "Назад"
        view.findViewById<MaterialButton>(R.id.back_button).setOnClickListener {
            val title = titleEdit.text.toString()
            val content = contentEdit.text.toString()
            val category = spinner.text.toString()

            if (noteId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    val existingNote = noteDao.getNoteById(noteId)
                    if (existingNote != null && (existingNote.title != title || existingNote.content != content || existingNote.category != category)) {
                        launch(Dispatchers.Main) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Сохранить изменения?")
                                .setMessage("Вы внесли изменения. Сохранить их перед выходом?")
                                .setPositiveButton("Да") { _, _ ->
                                    val updatedNote = existingNote.copy(
                                        title = title,
                                        content = content,
                                        category = category,
                                        timestamp = System.currentTimeMillis(),
                                        synced = false
                                    )
                                    saveNote(updatedNote)
                                }
                                .setNegativeButton("Нет") { _, _ ->
                                    findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                                }
                                .setNeutralButton("Отмена", null)
                                .show()
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                        }
                    }
                }
            } else if (title.isNotEmpty() || content.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Сохранить новую заметку?")
                    .setMessage("Вы начали создавать заметку. Сохранить её перед выходом?")
                    .setPositiveButton("Да") { _, _ ->
                        val newNote = Note(title = title, content = content, category = category)
                        saveNote(newNote)
                    }
                    .setNegativeButton("Нет") { _, _ ->
                        findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                    }
                    .setNeutralButton("Отмена", null)
                    .show()
            } else {
                findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
            }
        }
        // Анимации появления
        view.findViewById<View>(R.id.logo_image).alpha = 0f
        view.findViewById<View>(R.id.title_text).alpha = 0f
        view.findViewById<View>(R.id.back_button).alpha = 0f
        view.findViewById<View>(R.id.title_input_layout).alpha = 0f
        view.findViewById<View>(R.id.content_input_layout).alpha = 0f
        view.findViewById<View>(R.id.category_spinner).alpha = 0f
        view.findViewById<View>(R.id.save_button).alpha = 0f
        view.findViewById<View>(R.id.delete_button).alpha = 0f

        view.findViewById<View>(R.id.logo_image).animate().alpha(1f).setDuration(500).start()
        view.findViewById<View>(R.id.title_text).animate().alpha(1f).setDuration(500).setStartDelay(200).start()
        view.findViewById<View>(R.id.back_button).animate().alpha(1f).setDuration(500).setStartDelay(400).start()
        view.findViewById<View>(R.id.title_input_layout).animate().alpha(1f).setDuration(500).setStartDelay(600).start()
        view.findViewById<View>(R.id.content_input_layout).animate().alpha(1f).setDuration(500).setStartDelay(800).start()
        view.findViewById<View>(R.id.category_spinner).animate().alpha(1f).setDuration(500).setStartDelay(1000).start()
        view.findViewById<View>(R.id.save_button).animate().alpha(1f).setDuration(500).setStartDelay(1200).start()
        view.findViewById<View>(R.id.delete_button).animate().alpha(1f).setDuration(500).setStartDelay(1400).start()
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
    private fun deleteNote(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                launch(Dispatchers.Main) {
                    saveProgress.visibility = View.VISIBLE
                }

                val userId = auth.currentUser?.uid ?: return@launch
                if (note.firestoreId.isNotBlank()) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("notes")
                        .document(note.firestoreId)
                        .delete()
                        .await()
                }

                noteDao.delete(note)

                launch(Dispatchers.Main) {
                    saveProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Заметка удалена", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    saveProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}