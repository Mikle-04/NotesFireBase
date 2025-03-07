package com.example.notes.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.R
import com.example.notes.data.db.models.Note
class NoteAdapter(private var notes: List<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.note_title)
        val contentText: TextView = itemView.findViewById(R.id.note_content)
        val categoryText: TextView = itemView.findViewById(R.id.note_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleText.text = note.title
        holder.contentText.text = note.content
        holder.categoryText.text = note.category
        holder.itemView.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToEditNoteFragment(note.id)
            holder.itemView.findNavController().navigate(action)
        }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}