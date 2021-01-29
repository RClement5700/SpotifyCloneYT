package com.plcoding.spotifycloneyt.data.exoplayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.data.exoplayer.callbacks.MusicPlaybackPreparer
import com.plcoding.spotifycloneyt.data.exoplayer.callbacks.MusicPlayerEventListener
import com.plcoding.spotifycloneyt.data.exoplayer.callbacks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

private const val SERVICE_TAG = "Music Service"

@AndroidEntryPoint
class MusicService: MediaBrowserServiceCompat() {

    @Inject lateinit var dataSourceFactory: DefaultDataSourceFactory
    @Inject lateinit var exoplayer: SimpleExoPlayer
    @Inject lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var currentlyPlayingSong: MediaMetadataCompat? = null

    var isForegroundService = false

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onCreate() {
        super.onCreate()
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
        musicNotificationManager = MusicNotificationManager(this, mediaSession.sessionToken,
        MusicPlayerNotificationListener(this)) {

        } //lamda function will always be called when the current song switches

        val musicPlayerPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            currentlyPlayingSong = it
            preparePlayer(firebaseMusicSource.songs, it, true)
        }
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlayerPreparer)
        mediaSessionConnector.setPlayer(exoplayer)
        exoplayer.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(exoplayer)
    }


    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean)
    {
        val currentSongIndex = if (currentlyPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoplayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoplayer.seekTo(currentSongIndex, 0L)
        exoplayer.playWhenReady = playNow

    }
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?):
            BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>)
    {
        TODO("Not yet implemented")
    }
}