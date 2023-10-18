package com.example.eton.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
        val emailText: TextView = findViewById(R.id.emailText)
        val pass: TextView = findViewById(R.id.passwordText)
        var msg: String        // display toast message
        var auth = false    // check if authenticated

        // for async call to authenticate
        runBlocking {
            try {
                client.gotrue.loginWith(Email) {
                    email = emailText.text.toString()
                    password = pass.text.toString()
                }

                msg = "User ${emailText.text} success!"
                auth = true
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                msg = e.localizedMessage.toString().split("\n")[0] // dk if there's a better way to do this LOL
                auth = false
            }
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        // login and start new activity when authenticated
        if (auth) {
            val intent = Intent(this, NoteListActivity::class.java)
            startActivity(intent)
        }
    }

    fun onSignUp(view: View) {
        val emailText: TextView = findViewById(R.id.emailText)
        val pass: TextView = findViewById(R.id.passwordText)
        var msg: String

        runBlocking {
            try {
                val user = client.gotrue.signUpWith(Email) {
                    email = emailText.text.toString()
                    password = pass.text.toString()
                }
                msg = "User ${emailText.text} sign up success!"
            } catch (e: Exception) {
                msg = e.localizedMessage.toString().split("\n")[0] // dk if there's a better way to do this LOL
                Log.e("Error", e.toString())
            }
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}