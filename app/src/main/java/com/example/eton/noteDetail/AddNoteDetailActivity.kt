package com.example.eton.noteDetail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.eton.R
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

/**
 * Unused; prolly can delete sooner or later
 */
class AddNoteDetailActivity: AppCompatActivity() {
    private var added = false
    private lateinit var btn: Button
    private val client = Supabase.getClient()
    private lateinit var note: Note
    private var noteId by Delegates.notNull<Int>()
    private lateinit var etLocation: AppCompatEditText

    private val cameraRequest = 1888
    private lateinit var imageView: ImageView
    private var photo: Bitmap? = null
    private var isImageFullScreen = false
    private var originalLayoutParams: ConstraintLayout.LayoutParams? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        btn = findViewById(R.id.btnSave)
        btn.text = "Add New Note"

        imageView = findViewById(R.id.imageView)
        imageView.setOnClickListener {
            isImageFullScreen = !isImageFullScreen
            setImageSize(imageView, isImageFullScreen)
        }
        originalLayoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams

        if (!Places.isInitialized()) {
            Places.initialize(this@AddNoteDetailActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        etLocation = findViewById(R.id.editTextLocation)

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
     * Resize image onClick
     */
    private fun setImageSize(imageView: ImageView, isImageFullScreen: Boolean) {
        if (isImageFullScreen) {
            imageView.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        } else {
            imageView.layoutParams = originalLayoutParams
        }
    }

    fun onSave(view: View) {
        var success = false

        // populate the data
        val etTitle: EditText = findViewById(R.id.editTextTitle)
        val etBody: EditText = findViewById(R.id.editTextBody)

        val title = etTitle.text.toString().trim()
        val body = etBody.text.toString().trim()
        val location = etLocation.text.toString()
        val photoStr: String = photo?.let { bitmapToBase64(it) } ?: ""

        if (title != "" || body != "") {
            // Check if the button has been pressed, won't create duplicate records
            if (!added) {
                val id = Random().nextInt(Int.MAX_VALUE)
                val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                note = Note(id, title, body, current, location, photoStr, 0.0, 0.0)

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
                            set("note_title", title )
                            set("note_text", body )
                            set("note_photo", photoStr)
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

    fun onAddLocation(view: View) {
        try {
            val fields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            )
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this@AddNoteDetailActivity)
            startActivityForResult(intent, AddNoteDetailActivity.PLACE_AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
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
            } else if (requestCode == AddNoteDetailActivity.PLACE_AUTOCOMPLETE_REQUEST_CODE) {
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

    companion object {
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 1
    }
}