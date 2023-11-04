package com.example.eton.noteList

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.eton.R
import com.example.eton.noteDetail.NoteDetailActivity
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.lang.Exception

class NoteListActivity : AppCompatActivity() {
    private lateinit var userId: String
    private val client = Supabase.getClient()
    private lateinit var data: MutableList<Note>
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var rv: RecyclerView

    private val callback: ItemTouchHelper.SimpleCallback = object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            Log.i("what am i", data.get(viewHolder.adapterPosition).toString())
            val deleted = deleteNote(viewHolder.adapterPosition, viewHolder)
            val msg = if (deleted) "Item deleted" else "Unable to delete note"
            val snackbar = Snackbar.make(rv, msg, Snackbar.LENGTH_LONG)
            snackbar.show()
        }
    }

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
        rv = findViewById(R.id.homeRv)
        manager.orientation = LinearLayoutManager.VERTICAL
        rv.layoutManager = manager

        // add divider for recycler view
        val dividerItemDecoration = DividerItemDecoration(rv.context,
            manager.orientation);
        rv.addItemDecoration(dividerItemDecoration);

        var helper: ItemTouchHelper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(rv)

        // fetch data from supabase
        fetchNotes()
    }

    private fun setAdapterData(){
        // create adapter and bind to recycler view
        val rv = findViewById<RecyclerView>(R.id.homeRv)
        noteAdapter = NoteAdapter(
            data,
        ) { note:Note ->
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("isNew", false)
            intent.putExtra("note", note)
            startActivity(intent)
        }

        val noteCountText: TextView = findViewById(R.id.noteCount)
        noteCountText.text = "${noteAdapter.itemCount} notes"
        rv.adapter = noteAdapter
    }

    private fun fetchNotes (){
        lifecycleScope.launch {
            val client = Supabase.getClient()
            val res = client.postgrest["notes"].select(){
                order(column = "note_title", order = Order.ASCENDING)
            }
            data =  res.decodeList<Note>().toMutableList()
            setAdapterData()
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

    private fun deleteNote(position: Int, viewHolder: RecyclerView.ViewHolder): Boolean {
        var deleted = false
        try {
            val noteId = data[position].id
            lifecycleScope.launch {
                client.postgrest["notes"].delete { eq("id", noteId) }
            }
            deleted = true
        } catch (e: Exception) {
            Log.e("err", "Unable to delete note:: $e")
        }
        return if (deleted) {
            data.removeAt(viewHolder.adapterPosition)
            noteAdapter.notifyDataSetChanged()
            true
        } else {
            false
        }
    }
}


