package com.example.eton.noteList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eton.R

class NoteAdapter(
    private val dataSet: List<Note>,
    private val onClick: (Note) -> Unit,
    )
    : RecyclerView.Adapter<NoteAdapter.NoteViewHolder> (){

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): NoteViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_view_row_item, viewGroup, false)

        return NoteViewHolder(view, onClick)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: NoteViewHolder, position: Int) {
        val note = dataSet[position]
        viewHolder.bind(note)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    // Holder class
     class NoteViewHolder(view: View, val onClick: (Note) -> Unit) : RecyclerView.ViewHolder(view) {
        private val textView: TextView
        private var currentNote: Note? = null

        init {
            textView = view.findViewById(R.id.textView)
            itemView.setOnClickListener {
                currentNote?.let {
                    onClick(it)
                }
            }
        }

        //  bind the data and onClick on to the item
        fun bind(note: Note){
            currentNote = note
            textView.text = note.title
        }
    }
}