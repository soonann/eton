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
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.eton.R
import com.example.eton.camera.CameraView
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Random
import kotlin.properties.Delegates

class NoteDetailActivity : AppCompatActivity() {
    // for new note
    private var added = false
    private var noteId by Delegates.notNull<Int>()

    private val client = Supabase.getClient()
    private lateinit var note: Note
    private var isImageFullScreen = false

    private val cameraRequest = 1888
    private lateinit var imageView: ImageView
    private var photo: Bitmap? = null
    private var photoStr = ""
    private var originalLayoutParams: ConstraintLayout.LayoutParams? = null
    private lateinit var etTitle: EditText
    private lateinit var etBody: EditText
    private lateinit var etLocation: AppCompatEditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        if (!Places.isInitialized()) {
            Places.initialize(this@NoteDetailActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        // populate the data
        etTitle = findViewById(R.id.editTextTitle)
        etBody = findViewById(R.id.editTextBody)
        etLocation = findViewById(R.id.editTextLocation)
        imageView = findViewById(R.id.imageView)

        val it = intent.getSerializableExtra("isNew") as Boolean
        if (!it) {
            added = true
            populateData()
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

    /**
     * Populate existing data, for EDIT note
     */
    private fun populateData() {
        // get the intent seralized data
        val intentNote = intent.getSerializableExtra("note") as Note?
        note = intentNote!!

        // set the title and text
        etTitle.setText(note.note_title)
        etBody.setText(note.note_text)

        if (note.note_location.isNotEmpty()) {
            etLocation.setText(note.note_location)
        }

        if (note.note_photo.isNotEmpty()) {
            val base64String = note.note_photo!!

            // Create a Bitmap object from the byte array.
            val bitmap = decodeBase64StringToBitmap(base64String)

            // Display the bitmap in the ImageView.
            imageView.setImageBitmap(bitmap)

            imageView.setOnClickListener {
                isImageFullScreen = !isImageFullScreen
                setImageSize(imageView, isImageFullScreen)
            }
            // get the original layout param of picture incase
            originalLayoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams
        } else if (photoStr.isNotEmpty()) {
            imageView.setImageBitmap(photo)

            imageView.setOnClickListener {
                isImageFullScreen = !isImageFullScreen
                setImageSize(imageView, isImageFullScreen)
            }
            // get the original layout param of picture incase
            originalLayoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams
        }
    }

    /**
     * Trigger add location via Places API
     */
    fun onAddLocation(view: View) {
        try {
            val fields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            )
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this@NoteDetailActivity)
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Save/Add note
     */
    fun onSave(view: View){
        var saved = false
        val title = etTitle.text.toString().trim()
        val body = etBody.text.toString().trim()
        val location = etLocation.text.toString()
        try {
            if (added) {
                lifecycleScope.launch {
                    client.postgrest["notes"].update ({
                        set("note_title", title )
                        set("note_text", body)
                        set("note_location", location)
                        set("note_photo", photoStr)
                    }){
                        eq("id", note.id)
                    }
                }
            } else {
                val id = Random().nextInt(Int.MAX_VALUE)
                val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                note = Note(id, title, body, current, location, photoStr)
                lifecycleScope.launch {
                    val response = client.postgrest["notes"].insert(note)
                    val responseStr = response.body.toString()

                    // Get id for the note inserted
                    val responseJson = JSONArray(responseStr).getJSONObject(0)
                    noteId = responseJson.getInt("id")
                    Log.i("response", noteId.toString())
                }
                added = true
            }
            saved = true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("longest error to debug", e.toString())
        }

        if (saved) {
            Toast.makeText(this, "Successfully saved data!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to save data", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Resize image onClick
     */
    fun setImageSize(imageView: ImageView, isImageFullScreen: Boolean) {
        if (isImageFullScreen) {
            imageView.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        } else {
            imageView.layoutParams = originalLayoutParams
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
                photoStr = photo?.let { bitmapToBase64(it) } ?: ""
            } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                etLocation.setText(place.address)
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

    companion object {
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 1
    }


    // Image detector
//    fun imageDetector(view: View) {
//        val intent = Intent(this, CameraView::class.java)
//        startActivity(intent)
//    }
}