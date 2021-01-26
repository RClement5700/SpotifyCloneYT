package com.plcoding.spotifycloneyt.data.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.session.MediaController
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.plcoding.spotifycloneyt.Constants.NOTIFICATION_CHANNEL_ID
import com.plcoding.spotifycloneyt.Constants.NOTIFICATION_ID
import com.plcoding.spotifycloneyt.R
import java.util.*

class MusicNotificationManager(
    private val context: Context,
    private val sessionToken: MediaSessionCompat.Token,
    private val notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit
) {

    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager = PlayerNotificationManager.createWithNotificationChannel (
            context,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_channel_name,
            R.string.notification_channel_description,
            NOTIFICATION_ID,
            DescriptionAdapter(mediaController),
            notificationListener
        )
            .apply{
                setSmallIcon(R.drawable.ic_music)
                setMediaSessionToken(sessionToken)
        }
    }

    fun showNotification(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(val mediaController: MediaControllerCompat):
        PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return mediaController.metadata.description.toString()
        }

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback):
                Bitmap? {
            Glide.with(context).asBitmap().load(mediaController.metadata.description.iconUri)
                .into(object: CustomTarget<Bitmap>() {

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        callback.onBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                })
            return null
        }
    }
}