package com.example.eton.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.eton.R
import com.example.eton.noteList.NoteListActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking

class LoginActivity : AppCompatActivity() {

    private lateinit var client: SupabaseClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        client = createSupabaseClient("SUPABASE_URL_HERE", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtZWVjbml4bXlsYXV2ZG9pam95Iiwicm9sZSI6ImFub24iLCJpYXQiOjE2OTc1NTA0NTksImV4cCI6MjAxMzEyNjQ1OX0.BRhn3frE-A_Refo2qmCaHFteXli0SlrL09bSp5dXByI") {
            install(GoTrue)
        }
    }

    // onLogin button clicked
    fun onLogin (view: View){
        val username: TextView = findViewById(R.id.usernameText)
        val pass: TextView = findViewById(R.id.passwordText)

        runBlocking {
            client.gotrue.loginWith(Email) {
                email = username.text.toString()
                password = pass.text.toString()
            }
        }

        val intent = Intent(this, NoteListActivity::class.java)
        startActivity(intent)
    }

    fun onSignUp(view: View) {
        val username: TextView = findViewById(R.id.usernameText)
        val pass: TextView = findViewById(R.id.passwordText)

        runBlocking {
            val user = client.gotrue.signUpWith(Email) {
                email = username.text.toString()
                password = pass.text.toString()
            }
            Log.i("signup", user.toString())
        }
    }

    // onLogin with Google
    fun onLoginWithGoogle(view: View){
       // lifecycleScope.launch {
       //     client.gotrue.loginWith(Google)
       // }
    }
}