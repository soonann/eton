package com.example.eton.noteDetail

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.eton.R
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {

    val client = Supabase().getClient()
    lateinit var note: Note

    private val cameraRequest = 1888
    lateinit var imageView: ImageView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // get the intent seralized data
        val intentNote = intent.getSerializableExtra("note") as Note?
        note = intentNote!!

        // populate the data
        val etTitle: EditText = findViewById(R.id.editTextTitle)
        val etBody: EditText = findViewById(R.id.editTextBody)

        // set the title and text
        etTitle.setText(note.note_title)
        etBody.setText(note.note_text)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequest)
        imageView = findViewById(R.id.imageView)
        val cameraBtn: FloatingActionButton = findViewById(R.id.cameraBtn)
        cameraBtn.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)
        }

    }

    // TODO: fix the save button as it is quite buggy, doesn't update the db.
    fun onSave(view: View){
        // populate the data
        val etTitle: EditText = findViewById(R.id.editTextTitle)
        val etBody: EditText = findViewById(R.id.editTextBody)
        lifecycleScope.launch {
            client.postgrest["notes"].update ({
                set("note_title", etTitle.text.toString() )
                set("note_text", etBody.text.toString() )
            }){
                eq("id", note.id)
            }
        }
    }

    /**
     * Take video and return as an image
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest) {
            val photo: Bitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(photo)
        }
    }
}