package com.example.eton.map

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import com.example.eton.R
import com.example.eton.supabase.Note
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var note: Note? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (intent.hasExtra("note")) {
            note = intent.getSerializableExtra("note") as Note
        }

        if (note != null) {
            val toolbarMap: Toolbar = findViewById(R.id.toolbarMap)
            setSupportActionBar(toolbarMap)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = note!!.note_title

            Log.i("latlng", note!!.lat.toString())

            toolbarMap.setNavigationOnClickListener{
                onBackPressed()
            }
            val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val position = LatLng(note!!.lat, note!!.long)
        googleMap!!.addMarker(MarkerOptions().position(position).title(note!!.note_location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 12f)
        googleMap.animateCamera(newLatLngZoom)
    }
}