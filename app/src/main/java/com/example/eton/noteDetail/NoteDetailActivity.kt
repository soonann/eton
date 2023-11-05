package com.example.eton.noteDetail

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.eton.ConversationActivity
import com.example.eton.R
import com.example.eton.utils.GetAddressFromLatLng
import com.example.eton.map.MapActivity
import com.example.eton.supabase.Note
import com.example.eton.supabase.Supabase
import com.example.eton.utils.CustomTextWatcher
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.Objects
import java.util.Random
import kotlin.properties.Delegates

class NoteDetailActivity : AppCompatActivity() {
    // for new note
    private var added = false
    private var noteId by Delegates.notNull<Int>()
    private val client = Supabase.getClient()
    private lateinit var note: Note
    private var isImageFullScreen = false

    private var photo: Bitmap? = null
    private var originalLayoutParams: ConstraintLayout.LayoutParams? = null

    private lateinit var etTitle: EditText
    private lateinit var etBody: EditText
    private lateinit var etLocation: AppCompatEditText
    private lateinit var imageView: ImageView
    private lateinit var imageLabelText: TextView

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var imageLabeler: ImageLabeler
    private lateinit var progressDialog: ProgressDialog

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        if (!Places.isInitialized()) {
            Places.initialize(this@NoteDetailActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        etTitle = findViewById(R.id.editTextTitle)
        etBody = findViewById(R.id.editTextBody)
        val textWatcher = CustomTextWatcher()
        etBody.addTextChangedListener(textWatcher)
        etLocation = findViewById(R.id.editTextLocation)
        imageView = findViewById(R.id.imageView)
        imageLabelText = findViewById(R.id.imageLabelText)
        imageLabelText.text = ""

        val it = intent.getSerializableExtra("isNew") as Boolean
        if (!it) {
            added = true
            populateData()
        } else {
            note = Note()
        }

        // For resizing image
        imageView.setOnClickListener {
            isImageFullScreen = !isImageFullScreen
            setImageSize(imageView, isImageFullScreen)
        }
        // get the original layout param of picture incase
        originalLayoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams
    }

    /**
     * Populate existing data, for EDIT note
     */
    private fun populateData() {
        // get the intent seralized data
        val intentNote = intent.getSerializableExtra("note") as Note
        note = intentNote!!
        Log.i("note", note.toString())

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
        }

        if (note.photo_labels.isNotEmpty()) {
            imageLabelText.setText(note.photo_labels)
        }
    }

    /**
     * Save/Add note
     */
    fun onSave(view: View){
        var saved = false
        val title = etTitle.text.toString().trim()
        val body = etBody.text.toString().trim()
        try {
            if (added) {
                lifecycleScope.launch {
                    client.postgrest["notes"].update ({
                        set("note_title", title )
                        set("note_text", body)
                        set("note_location", note.note_location)
                        set("lat", note.lat)
                        set("long", note.long)
                        set("note_photo", note.note_photo)
                        set("photo_labels", note.photo_labels)
                    }){
                        eq("id", note.id)
                    }
                }
                saved = true
            } else {
                if (title.isNotEmpty()) {
                    val id = Random().nextInt(Int.MAX_VALUE)
                    val current = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                    note.id = id
                    note.note_title = title
                    note.note_text = body
                    note.created_at = current
                    lifecycleScope.launch {
                        val response = client.postgrest["notes"].insert(note)
                        val responseStr = response.body.toString()

                        // Get id for the note inserted
                        val responseJson = JSONArray(responseStr).getJSONObject(0)
                        noteId = responseJson.getInt("id")
                        Log.i("response", noteId.toString())
                    }
                    added = true
                    saved = true
                }
            }
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
     * Results for various activities
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            if (requestCode == CAMERA_REQUEST) {
                photo = data?.extras?.get("data") as Bitmap
                imageView.setImageBitmap(photo)
                labelImage(photo!!)
                note.note_photo = photo?.let { bitmapToBase64(it) } ?: ""
            } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                etLocation.setText(place.address)
                note.note_location = place.address!!
                note.lat = place.latLng!!.latitude
                note.long = place.latLng!!.longitude
            } else if (requestCode == SPEECH_INPUT) {
                if (data != null) {
                    val res: ArrayList<String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                    val tempBody = "${etBody.text} ${Objects.requireNonNull(res)[0]}"
                    etBody.setText(tempBody)
                }
            }
        }
    }

    // ---------------- BUTTONS ----------------
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
     * Open camera activity
     */
    fun onCameraOpen(view: View) {
        if (!isCameraEnabled()) {
            Toast.makeText(this, "Your camera permission is turned off. Please turn it on to work!", Toast.LENGTH_SHORT).show()
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
    }

    /**
     * Open Maps Location
     */
    fun onViewMapLocation(view: View) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("note", note)
        startActivity(intent)
    }

    /**
     * Current location btn clicked
     */
    fun onCurrentLocation(view: View) {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Your location provider is turned off. Please turn it on to work!", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this).withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        requestNewLocationData()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
        }
    }

    /**
     * Record button clicked
     */
    fun onVoiceRecord(view: View) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech to text")

        try {
            startActivityForResult(intent, SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this@NoteDetailActivity, "Unable to record:: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun onConversation(view: View) {
        val intent = Intent(this, ConversationActivity::class.java)
        startActivity(intent)
    }

    // ---------------- UTILS ----------------
    /**
     * Convert a Bitmap type into a String
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    /**
     * Convert a String to Bitmap type for displaying upon loading
     */
    private fun decodeBase64StringToBitmap(base64String: String): Bitmap {
        val byteArray = Base64.getDecoder().decode(base64String)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, BitmapFactory.Options())
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

    /**
     * Check if location is enabled
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Check if camera is enabled, else request permissions
     */
    private fun isCameraEnabled(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST
            )
            false
        } else {
            true
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper())
    }

    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    /**
     * For getting current location's name if there is any
     */
    private val mLocationCallBack = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            note.lat = mLastLocation.latitude
            note.long = mLastLocation.longitude

            val addressTask = GetAddressFromLatLng(this@NoteDetailActivity, note.lat, note.long)
            addressTask.setAddressListener(object: GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?) {
                    etLocation.setText(address)
                    note.note_location = address!!
                }

                override fun onError() {
                    Log.e("Get address:: ", "Something went wrong")
                }
            })
            addressTask.getAddress()
        }
    }

    /**
     * Labelling image
     */
    private fun labelImage(photo: Bitmap) {
        progressDialog.setMessage("Preparing image...")
        progressDialog.show()

        val inputImage = InputImage.fromBitmap(photo, 0)

        progressDialog.setMessage("Labelling image...")

        imageLabeler.process(inputImage)
            .addOnSuccessListener {imageLabels ->
                imageLabelText.text = ""
                for (i in 0..2) {
                    val text = imageLabels[i].text
                    val confidence = imageLabels[i].confidence
                    imageLabelText.append("$text : $confidence\n")
                }
                progressDialog.dismiss()
                note.photo_labels = imageLabelText.text.toString()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // ---------------- CONST VAR ----------------
    companion object {
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 1
        private const val CAMERA_REQUEST = 2
        private const val SPEECH_INPUT = 3
    }

    // Image detector
//    fun imageDetector(view: View) {
//        val intent = Intent(this, CameraView::class.java)
//        startActivity(intent)
//    }
}