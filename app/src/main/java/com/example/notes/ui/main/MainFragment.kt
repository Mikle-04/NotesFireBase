package com.example.notes.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.R
import com.example.notes.data.db.AppDatabase
import com.example.notes.data.db.dao.NoteDao


class MainFragment : Fragment() {

    private lateinit var noteDao: NoteDao
    private lateinit var adapter: NoteAdapter

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
            adapter.updateNotes(notes)
        })

        // Кнопка добавления
        view.findViewById<View>(R.id.add_button).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_editNoteFragment)
        }
    }
}