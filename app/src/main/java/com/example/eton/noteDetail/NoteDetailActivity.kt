package com.example.eton.noteDetail

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.eton.R
import com.example.eton.camera.CameraView
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Base64

class NoteDetailActivity : AppCompatActivity() {

    private val client = Supabase.getClient()
    private lateinit var note: Note

    private val cameraRequest = 1888
    private lateinit var imageView: ImageView
    private var photo: Bitmap? = null

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

        imageView = findViewById(R.id.imageView)
        // set the title and text
        etTitle.setText(note.note_title)
        etBody.setText(note.note_text)
        if (note.note_photo != null) {
            val base64String = note.note_photo!!

            // Create a Bitmap object from the byte array.
            val bitmap = decodeBase64StringToBitmap(base64String)

            // Display the bitmap in the ImageView.
            imageView.setImageBitmap(bitmap)
        }

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequest)
        val cameraBtn: FloatingActionButton = findViewById(R.id.cameraBtn)
        cameraBtn.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)
        }

    }

    fun onSave(view: View){
        var saved = false
        // populate the data
        val etTitle: EditText = findViewById(R.id.editTextTitle)
        val etBody: EditText = findViewById(R.id.editTextBody)
        val photoStr: String? = photo?.let { bitmapToBase64(it) }
        try {
            lifecycleScope.launch {
                client.postgrest["notes"].update ({
                    set("note_title", etTitle.text.toString() )
                    set("note_text", etBody.text.toString() )
                    set("note_photo", photoStr)
                }){
                    eq("id", note.id)
                }
            }
            saved = true
        } catch (e: Exception) {
            Log.e("Err", e.toString())
        }

        if (saved) {
            Toast.makeText(this, "Successfully saved data!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to save data", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Take picture and return as an image
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            if (requestCode == cameraRequest) {
                photo = data?.extras?.get("data") as Bitmap
                imageView.setImageBitmap(photo)
            }
        }
    }

    /**
     * Convert a Bitmap type into a String
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    private fun decodeBase64StringToBitmap(base64String: String): Bitmap {
        val byteArray = Base64.getDecoder().decode(base64String)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, BitmapFactory.Options())
    }


    // Image detector
//    fun imageDetector(view: View) {
//        val intent = Intent(this, CameraView::class.java)
//        startActivity(intent)
//    }
}