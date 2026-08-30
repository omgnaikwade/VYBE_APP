package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.Song

object NotificationHelper {

    private const val CHANNEL_ID = "vybe_music"
    const val NOTIFICATION_ID = 1001

    const val ACTION_PLAY_PAUSE = "com.example.vybe.PLAY_PAUSE"
    const val ACTION_NEXT = "com.example.vybe.NEXT"
    const val ACTION_PREVIOUS = "com.example.vybe.PREVIOUS"

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VYBE Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    fun showMusicNotification(
    context: Context,
    song: Song,
    isPlaying: Boolean,
    playPauseIntent: PendingIntent? = null,
    previousIntent: PendingIntent? = null,
    nextIntent: PendingIntent? = null
    ) {
        createChannel(context)

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)

        previousIntent?.let {
            notification.addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                it
            )
        }

        playPauseIntent?.let {
            notification.addAction(
                if (isPlaying)
                    android.R.drawable.ic_media_pause
                else
                    android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                it
            )
        }

        nextIntent?.let {
            notification.addAction(
                android.R.drawable.ic_media_next,
                "Next",
                it
            )
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(NOTIFICATION_ID, notification.build())
    }

    fun cancel(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.cancel(NOTIFICATION_ID)
    }
}
