package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.ui.VybeApp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            try {
                // Create an anonymous Supabase user on first launch.
                // If a session already exists, reuse it.
                if (supabase.auth.currentSessionOrNull() == null) {
                    supabase.auth.signInAnonymously()
                    Log.d("VYBE_AUTH", "Anonymous user created")
                } else {
                    Log.d("VYBE_AUTH", "Existing user session restored")
                }

                val user = supabase.auth.currentUserOrNull()
                Log.d("VYBE_AUTH", "User ID: ${user?.id}")

            } catch (e: Exception) {
                Log.e("VYBE_AUTH", "Anonymous authentication failed", e)
            }
        }

        setContent {
            VybeApp()
        }
    }
}
