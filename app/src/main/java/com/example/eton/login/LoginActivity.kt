package com.example.eton.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.eton.R
import com.example.eton.noteList.NoteListActivity
import com.example.eton.supabase.Supabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.SSO
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    //lateinit var client: SupabaseClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //client = Supabase().getClient()
    }

    // onLogin button clicked
    fun onLogin (view: View){
        val user: TextView = findViewById(R.id.usernameText)
        val pass: TextView = findViewById(R.id.passwordText)

        val intent = Intent(this, NoteListActivity::class.java)
        startActivity(intent)
    }

    // onLogin with Google
    fun onLoginWithGoogle(view: View){
       // lifecycleScope.launch {
       //     client.gotrue.loginWith(Google)
       // }
    }
}