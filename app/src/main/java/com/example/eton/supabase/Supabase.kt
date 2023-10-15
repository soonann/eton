package com.example.eton.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Supabase {
    fun getClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "SUPABASE_URL_HERE",
            supabaseKey = "SUPABASE_ANON_TOKEN_HERE",
        ) {
            install(Postgrest)
        }
    }
}
