package com.example.notes.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.R
import com.example.notes.data.db.models.Note
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(private var notes: List<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.note_title)
        val category: TextView = itemView.findViewById(R.id.note_category)
        val content: TextView = itemView.findViewById(R.id.note_content)
        val timestamp: TextView = itemView.findViewById(R.id.note_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title
        holder.category.text = note.category
        holder.content.text = note.content
        holder.timestamp.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(note.timestamp)
        holder.itemView.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToEditNoteFragment(note.id)
            holder.itemView.findNavController().navigate(action)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun getNoteAt(position: Int): Note = notes[position]
}