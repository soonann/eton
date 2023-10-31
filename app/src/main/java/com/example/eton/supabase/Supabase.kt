package com.example.eton.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.GoTrue
import kotlinx.coroutines.launch

class Supabase {
    companion object {
        fun getClient(): SupabaseClient {
            return createSupabaseClient(
                supabaseUrl = "SUPABASE_URL_HERE",
                supabaseKey = "SUPABASE_ANON_TOKEN_HERE",
            ) {
                install(Postgrest)
                install(GoTrue)
            }
        }

    }
}
