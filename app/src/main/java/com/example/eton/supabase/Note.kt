package com.example.eton.supabase

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class Note (
    var id: Int,
    var note_title: String,
    var note_text: String,
    var created_at: String,
): JavaSerializable{
    constructor(note_title: String): this(0, note_title, "", "" ) {
    }
}