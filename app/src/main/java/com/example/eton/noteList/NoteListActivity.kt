package com.example.eton.noteList

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eton.R
import com.example.eton.noteDetail.NoteDetailActivity
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class NoteListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // set layout manager of recycler view
        val manager = LinearLayoutManager(this)
        val rv = findViewById<RecyclerView>(R.id.homeRv)
        manager.orientation = LinearLayoutManager.VERTICAL
        rv.layoutManager = manager

        // add divider for recycler view
        val dividerItemDecoration = DividerItemDecoration(rv.context,
            manager.orientation);
        rv.addItemDecoration(dividerItemDecoration);

        // fetch data from supabase
        fetchNotes()

    }

    private fun setAdapterData(data: List<Note>){
        // create adapter and bind to recycler view
        val rv = findViewById<RecyclerView>(R.id.homeRv)
        var noteAdapter = NoteAdapter(
            data,
        ) { note:Note ->
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("note", note)
            startActivity(intent)
        };
        rv.adapter = noteAdapter
    }

    private fun fetchNotes (){
        lifecycleScope.launch {
            val client = Supabase().getClient()
            val res = client.postgrest["notes"].select()
            val data =  res.decodeList<Note>()
            setAdapterData(data)

        }
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


