package com.example.eton.noteList

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.text.toLowerCase
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
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.Locale


class NoteListActivity : AppCompatActivity() {
    private lateinit var userId: String
    private val client = Supabase.getClient()
    private lateinit var data: MutableList<Note>
    private lateinit var tmpData: MutableList<Note>
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var rv: RecyclerView
    private lateinit var alertDialog: AlertDialog.Builder
    private lateinit var geofencingClient: GeofencingClient

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
            deleteNote(viewHolder.adapterPosition, viewHolder)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        geofencingClient = LocationServices.getGeofencingClient(this)

        // handle intent
        userId = intent.getStringExtra("userId").toString()
        val session = client.gotrue.currentSessionOrNull()

        // onRefresh
        val pullToRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pullToRefresh.setOnRefreshListener {
            fetchNotes() // on refresh, re-fetch the notes
            pullToRefresh.isRefreshing = false
        }

        // alert dialog
        alertDialog = AlertDialog.Builder(this)

        // set layout manager of recycler view
        val manager = LinearLayoutManager(this)
        rv = findViewById(R.id.homeRv)
        manager.orientation = LinearLayoutManager.VERTICAL
        rv.layoutManager = manager

        // add divider for recycler view
        val dividerItemDecoration = DividerItemDecoration(rv.context,
            manager.orientation);
        rv.addItemDecoration(dividerItemDecoration);

        // onItemTouch
        var helper: ItemTouchHelper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(rv)

        // init data
        fetchNotes()

        // handle search
        val noteCountText: TextView = findViewById(R.id.noteCount)
        val sv = findViewById<SearchView>(R.id.searchView)
        sv.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("Not yet implemented")
            }

            // on search query change
            override fun onQueryTextChange(newText: String?): Boolean {
                data.clear()
                val searchText = newText!!.toLowerCase(Locale.getDefault())
                if (searchText.isNotEmpty()) {

                    tmpData.forEach {
                        // if the filter matches, add it into the filtered data list
                        if (it.note_title.toLowerCase().contains(searchText) ||
                            it.note_location.toLowerCase().contains(searchText) ||
                            it.note_text.toLowerCase().contains(searchText) ||
                            it.photo_labels.toLowerCase().contains(searchText)
                            ) {
                            data.add(it)
                        }
                    }

                    rv.adapter!!.notifyDataSetChanged() // notify the changes
                    noteCountText.text = "${rv.adapter!!.itemCount} notes"

                } else {
                    // if the text field is empty, fill with the proper data
                    data.clear()
                    data.addAll(tmpData)
                    rv.adapter!!.notifyDataSetChanged() // notify the changes
                    noteCountText.text = "${rv.adapter!!.itemCount} notes"
                }

                return false
            }

        })

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
            tmpData =  res.decodeList<Note>().toMutableList()
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

    private fun deleteNote(position: Int, viewHolder: RecyclerView.ViewHolder) {
        var deleted = false
        alertDialog.setTitle("Warning!")
            .setMessage("Do you want to delete this note?")
            .setCancelable(true)
            .setNegativeButton("Yes") { dialog, which ->
                //remove geofence if there is one
                if (data[position].lat != null) {
                    removeGeofence(data[position].note_title)
                }

                try {
                    val noteId = data[position].id
                    lifecycleScope.launch {
                        client.postgrest["notes"].delete { eq("id", noteId) }
                    }
                    deleted = true
                } catch (e: Exception) {
                    Log.e("err", "Unable to delete note:: $e")
                }
                if (deleted) {
                    data.removeAt(viewHolder.adapterPosition)
                    noteAdapter.notifyDataSetChanged()
                }
                val msg = if (deleted) "Item deleted" else "Unable to delete note"
                val snackbar = Snackbar.make(rv, msg, Snackbar.LENGTH_SHORT)
                snackbar.show()
            }.setNeutralButton("Cancel") { dialog, which ->
                fetchNotes()
            }.create()
        alertDialog.show()
    }

    private fun removeGeofence(id: String) {
        val geofenceId = listOf(id)
        geofencingClient.removeGeofences(geofenceId).run {
            addOnSuccessListener {
                Log.v("geofence remove", "success")
            }
            addOnFailureListener {
                Log.e("geofence remove", "failure")
            }
        }

    }

}


