package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.ui.VybeApp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            try {
                // Give Supabase Auth time to restore the saved session.
                delay(500)

                var user = supabase.auth.currentUserOrNull()

                if (user == null) {
                    supabase.auth.signInAnonymously()

                    user = supabase.auth.currentUserOrNull()

                    Log.d(
                        "VYBE_AUTH",
                        "New anonymous user created: ${user?.id}"
                    )
                } else {
                    Log.d(
                        "VYBE_AUTH",
                        "Existing user restored: ${user.id}"
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "VYBE_AUTH",
                    "Anonymous authentication failed",
                    e
                )
            }
        }

        setContent {
            VybeApp()
        }
    }
}
