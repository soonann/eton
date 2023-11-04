package com.example.eton.supabase

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class Note (
    var id: Int,
    var note_title: String,
    var note_text: String,
    var created_at: String,
    var note_location: String,
    var lat: Double,
    var long: Double,
    var note_photo: String,
    var photo_labels: String,
): JavaSerializable{
    constructor(): this(0, "", "", "" , "", 0.0, 0.0, "", "") {}

    constructor(note_title: String): this(0, note_title, "", "" , "", 0.0, 0.0, "", "") {}
}