package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.player.AudioPlayerManager

class MusicNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        when (intent?.action) {

            NotificationHelper.ACTION_PLAY_PAUSE -> {
                PlayerHolder.manager?.togglePlayPause()
            }

            NotificationHelper.ACTION_NEXT -> {
                PlayerHolder.manager?.next()
            }

            NotificationHelper.ACTION_PREVIOUS -> {
                PlayerHolder.manager?.previous()
            }
        }
    }
}

object PlayerHolder {
    var manager: AudioPlayerManager? = null
}
