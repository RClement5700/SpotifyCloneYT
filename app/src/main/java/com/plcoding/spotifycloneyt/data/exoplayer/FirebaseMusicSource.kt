package com.plcoding.spotifycloneyt.data.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.data.remote.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseMusicSource @Inject constructor (
    private val musicDatabase: MusicDatabase) {

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()
    var songs = emptyList<MediaMetadataCompat>()

    @InternalCoroutinesApi
    //Dispatchers.IO = optimize coroutines for IO operations
    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state = State.STATE_INITIALIZING
        val allSongs = musicDatabase.retrieveAllSongs()
        songs = allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.artist)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .build()
        }
        state = State.STATE_INITIALIZED
    }

    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource: ConcatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach{ song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() = songs.map { song ->
        val description = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setDescription(song.description.description)
            .setMediaId(song.description.mediaId)
            .setTitle(song.description.title)
            .setIconUri(song.description.iconUri)
            .setSubtitle(song.description.subtitle)
            .build()
        MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)
    }

    @InternalCoroutinesApi
    private var state: State = State.STATE_CREATED
        //when you pass parameters into this function later you will be able to see if the state is in
        //error or not
        set(value) {
            if (value == State.STATE_INITIALIZED || value == State.STATED_ERROR) {
                // synchronized = during this block, no other threads can access the given parameter
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach{ listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }
            } else {
                field = value

            }
        }

    @InternalCoroutinesApi
    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if (state == State.STATE_CREATED || state == State.STATE_INITIALIZED) {
            onReadyListeners += action
            return false
        } else {
            action(state == State.STATE_INITIALIZED)
            return true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATED_ERROR
}