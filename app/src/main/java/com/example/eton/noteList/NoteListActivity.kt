package com.example.eton.noteList

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eton.R

class NoteListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // get data
        val data = getMockData()

        // set layout manager of recycler view
        val manager = LinearLayoutManager(this)
        val rv = findViewById<RecyclerView>(R.id.homeRv)
        manager.orientation = LinearLayoutManager.VERTICAL
        rv.layoutManager = manager

        // add divider for recycler view
        val dividerItemDecoration = DividerItemDecoration(rv.context,
            manager.orientation);
        rv.addItemDecoration(dividerItemDecoration);

        // create adapter and bind to recycler view
        var noteAdapter = NoteAdapter(
            data,
        ) { note -> Log.d("",note.title) };
        rv.adapter = noteAdapter
    }

    // TODO: CRUD from supabase
    private fun getMockData(): ArrayList<Note> {
        return arrayListOf<Note>(
            Note("Groceries"),
            Note("Shopping List"),
            Note("School"),
        )
    }
}


