package com.example.eton.noteDetail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.eton.R
import com.example.eton.supabase.Note

class NoteDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // get the intent seralized data
        val note = intent.getSerializableExtra("note") as Note?

        // populate the data
        val tv: TextView = findViewById<TextView>(R.id.noteDetailTitle)
        tv.text = note!!.note_title + "\n" + note!!.note_text

    }
}