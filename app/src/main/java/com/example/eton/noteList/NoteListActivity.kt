package com.example.eton.noteList

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.eton.R
import com.example.eton.noteDetail.NoteDetailActivity
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class NoteListActivity : AppCompatActivity() {
    private lateinit var userId: String
    private val client = Supabase.getClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        userId = intent.getStringExtra("userId").toString()
        val session = client.gotrue.currentSessionOrNull()

        // refresh
        val pullToRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pullToRefresh.setOnRefreshListener {
            fetchNotes() // your code
            pullToRefresh.isRefreshing = false
        }

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
            intent.putExtra("isNew", false)
            intent.putExtra("note", note)
            startActivity(intent)
        };
        rv.adapter = noteAdapter
    }

    private fun fetchNotes (){
        lifecycleScope.launch {
            val client = Supabase.getClient()
            val res = client.postgrest["notes"].select(){
                order(column = "note_title", order = Order.ASCENDING)
            }
            val data =  res.decodeList<Note>()
            setAdapterData(data)
        }
    }

    // Pull latest changes from the db when the NoteDetailActivity closes.
    override fun onResume() {
        super.onResume()
        fetchNotes()
    }

    fun onAddNote(view: View) {
        val intent = Intent(this, NoteDetailActivity::class.java)
        intent.putExtra("isNew", true)
        startActivity(intent)
    }
}


