package com.example.eton.noteDetail

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.eton.R
import com.example.eton.noteList.Note

class NoteDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        val title = intent.getStringExtra("title")
        val tv: TextView = findViewById<TextView>(R.id.noteDetailTitle)
        tv.text = title.toString()

    }
}