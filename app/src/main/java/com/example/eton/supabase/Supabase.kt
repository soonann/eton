package com.example.eton.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.storage.Storage

class Supabase {
    companion object {
        fun getClient(): SupabaseClient {
            return createSupabaseClient(
                supabaseUrl = "<YOUR KEY HERE>",
                supabaseKey = "<YOUR KEY HERE>",
            ) {
                install(Postgrest)
                install(GoTrue)
                install(Storage)
            }
        }

    }
}
