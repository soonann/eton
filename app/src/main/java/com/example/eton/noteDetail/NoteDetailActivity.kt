package com.example.eton.noteDetail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.eton.R
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {

    val client = Supabase().getClient()
    lateinit var note: Note
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // get the intent seralized data
        val intentNote = intent.getSerializableExtra("note") as Note?
        note = intentNote!!

        // populate the data
        val etTitle: EditText = findViewById<EditText>(R.id.editTextTitle)
        val etBody: EditText = findViewById<EditText>(R.id.editTextBody)

        // set the title and text
        etTitle.setText(note.note_title)
        etBody.setText(note.note_text)

    }

    fun onSave(view: View){
        // populate the data
        val etTitle: EditText = findViewById<EditText>(R.id.editTextTitle)
        val etBody: EditText = findViewById<EditText>(R.id.editTextBody)
        lifecycleScope.launch {
            client.postgrest["notes"].update ({
                set("note_title", etTitle.text.toString() )
                set("note_text", etBody.text.toString() )
            }){
                eq("id", note.id)
            }
        }
    }
}