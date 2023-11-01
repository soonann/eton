package com.example.eton.noteDetail

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.eton.R
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random
import kotlin.properties.Delegates

class AddNoteDetailActivity: AppCompatActivity() {
    private var added = false
    private lateinit var btn: Button
    private val client = Supabase.getClient()
    private lateinit var note: Note
    private var noteId by Delegates.notNull<Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        btn = findViewById(R.id.btnSave)
        btn.text = "Add New Note"
    }

    fun onSave(view: View) {
        // populate the data
        val etTitle: EditText = findViewById(R.id.editTextTitle)
        val etBody: EditText = findViewById(R.id.editTextBody)
        var success = false

        // Check if the button has been pressed, won't create duplicate records
        if (!added) {
            val id = Random().nextInt(Int.MAX_VALUE)
            val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            note = Note(id, etTitle.text.toString(), etBody.text.toString(), current)

            try {
                lifecycleScope.launch {
                    val response = client.postgrest["notes"].insert(note)
                    val responseStr = response.body.toString()
                    Log.i("response", responseStr)

                    // Get id for the note inserted
                    val responseJson = JSONArray(responseStr).getJSONObject(0)
                    noteId = responseJson.getInt("id")
                    Log.i("response", noteId.toString())
                }
                added = true
                success = true
            } catch (e: Exception) {
                Log.e("Err", e.toString())
            }
        } else {
            // Save to db
            try {
                lifecycleScope.launch {
                    client.postgrest["notes"].update ({
                        set("note_title", etTitle.text.toString() )
                        set("note_text", etBody.text.toString() )
                    }){
                        eq("id", noteId)
                    }
                }
                success = true
            } catch (e: Exception) {
                Log.e("Err", e.toString())
            }
        }
        if (success) {
            Toast.makeText(this, "Successfully saved data!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to save data", Toast.LENGTH_SHORT).show()
        }
    }
}