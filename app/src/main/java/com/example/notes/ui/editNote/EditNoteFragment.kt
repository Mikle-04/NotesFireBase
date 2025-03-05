package com.example.notes.ui.editNote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notes.R
import com.example.notes.data.db.AppDatabase
import com.example.notes.data.db.dao.NoteDao
import com.example.notes.data.db.models.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class EditNoteFragment : Fragment() {

    private lateinit var noteDao: NoteDao

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
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        // Кнопка сохранения
        view.findViewById<View>(R.id.save_button).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.title_edit).text.toString()
            val content = view.findViewById<EditText>(R.id.content_edit).text.toString()
            val category = spinner.selectedItem.toString()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                val note = Note(title = title, content = content, category = category)
                CoroutineScope(Dispatchers.IO).launch {
                    noteDao.insert(note)
                }
                findNavController().navigate(R.id.action_editNoteFragment_to_mainFragment)
            }
        }
    }
}