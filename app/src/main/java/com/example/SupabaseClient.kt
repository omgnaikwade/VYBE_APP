package com.example

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.createSupabaseClient

val supabase = createSupabaseClient(
    supabaseUrl = "https://lmxtndcyhrhxmjtisjgh.supabase.co",
    supabaseKey = "sb_publishable_U9pf8FJk9wIEauC69YuxKw_N-JQoHod"
) {
    install(Auth)
    install(Postgrest)
}
