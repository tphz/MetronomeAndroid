package com.tangpenghui.metronome.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.session.*
import com.tangpenghui.metronome.controller.MetronomeController
import com.tangpenghui.metronome.controller.MetronomeState
import com.tangpenghui.metronome.controller.RunState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(androidx.media3.common.util.UnstableApi::class)
class MetronomeMediaSession(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var controller: MetronomeController? = null
    private var stateCollector: kotlinx.coroutines.Job? = null
    private val lastState = MutableStateFlow<MetronomeState?>(null)

    private var mediaSession: MediaSession? = null

    private val player = object : Player {
        override fun play() { controller?.start() }
        override fun pause() { controller?.pause() }
        override fun stop() { controller?.stop() }
        override fun isPlaying(): Boolean = controller?.state?.value?.runState == RunState.RUNNING
        override fun getPlayWhenReady(): Boolean = isPlaying
        override fun setPlayWhenReady(p: Boolean) {}
        override fun getPlaybackState(): Int = if (isPlaying) Player.STATE_READY else Player.STATE_IDLE
        override fun getCurrentMediaItem(): MediaItem? = null
        override fun setMediaItem(item: MediaItem) {}
        override fun setMediaItem(item: MediaItem, startPositionMs: Long) {}
        override fun setMediaItem(item: MediaItem, resetPosition: Boolean) {}
        override fun setMediaItems(items: List<MediaItem>) {}
        override fun setMediaItems(items: List<MediaItem>, resetPosition: Boolean) { setMediaItems(items) }
        override fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPositionMs: Long) { setMediaItems(items) }
        override fun addMediaItem(item: MediaItem) {}
        override fun addMediaItem(index: Int, item: MediaItem) {}
        override fun addMediaItems(items: List<MediaItem>) {}
        override fun addMediaItems(index: Int, items: List<MediaItem>) {}
        override fun replaceMediaItem(index: Int, item: MediaItem) {}
        override fun replaceMediaItems(fromIndex: Int, toIndex: Int, items: List<MediaItem>) {}
        override fun removeMediaItem(index: Int) {}
        override fun removeMediaItems(fromIndex: Int, toIndex: Int) {}
        override fun moveMediaItem(fromIndex: Int, toIndex: Int) {}
        override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}
        override fun clearMediaItems() {}
        override fun prepare() {}
        override fun seekToDefaultPosition() {}
        override fun seekToDefaultPosition(mediaItemIndex: Int) {}
        override fun seekTo(positionMs: Long) {}
        override fun seekTo(mediaItemIndex: Int, positionMs: Long) {}
        override fun getCurrentPosition(): Long = 0L
        override fun getDuration(): Long = 0L
        override fun getBufferedPosition(): Long = 0L
        override fun getBufferedPercentage(): Int = 0
        override fun getTotalBufferedDuration(): Long = 0L
        override fun isLoading(): Boolean = false
        override fun getPlayerError(): PlaybackException? = null
        override fun getPlaybackSuppressionReason(): Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE
        override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {}
        override fun getPlaybackParameters(): PlaybackParameters = PlaybackParameters.DEFAULT
        override fun seekBack() {}
        override fun seekForward() {}
        override fun hasNext(): Boolean = false
        override fun hasNextWindow(): Boolean = false
        override fun hasNextMediaItem(): Boolean = false
        override fun next() {}
        override fun seekToNextWindow() {}
        override fun seekToNextMediaItem() {}
        override fun seekToNext() {}
        override fun hasPrevious(): Boolean = false
        override fun hasPreviousWindow(): Boolean = false
        override fun hasPreviousMediaItem(): Boolean = false
        override fun previous() {}
        override fun seekToPreviousWindow() {}
        override fun seekToPreviousMediaItem() {}
        override fun seekToPrevious() {}
        override fun setVolume(v: Float) { controller?.setVolume(v) }
        override fun getVolume(): Float = controller?.state?.value?.volume ?: 0.3f
        override fun setDeviceVolume(v: Int) {}
        override fun setDeviceVolume(v: Int, flags: Int) {}
        override fun increaseDeviceVolume() {}
        override fun increaseDeviceVolume(flags: Int) {}
        override fun decreaseDeviceVolume() {}
        override fun decreaseDeviceVolume(flags: Int) {}
        override fun getDeviceVolume(): Int = 0
        override fun getAvailableCommands(): Player.Commands = Player.Commands.EMPTY
        override fun isCommandAvailable(command: @Player.Command Int): Boolean = false
        override fun release() {}
        override fun setRepeatMode(repeatMode: @Player.RepeatMode Int) {}
        override fun getRepeatMode(): Int = Player.REPEAT_MODE_OFF
        override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {}
        override fun getShuffleModeEnabled(): Boolean = false
        override fun getCurrentTimeline(): Timeline = Timeline.EMPTY
        override fun getCurrentPeriodIndex(): Int = 0
        override fun getCurrentMediaItemIndex(): Int = 0
        override fun getPreviousMediaItemIndex(): Int = -1
        override fun getNextMediaItemIndex(): Int = -1
        override fun setPlaylistMetadata(metadata: MediaMetadata) {}
        override fun getPlaylistMetadata(): MediaMetadata = MediaMetadata.EMPTY
        override fun getAudioAttributes(): AudioAttributes = AudioAttributes.DEFAULT
        override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {}
        override fun getApplicationLooper(): Looper = Looper.getMainLooper()
        override fun addListener(listener: Player.Listener) {}
        override fun removeListener(listener: Player.Listener) {}
        override fun getCurrentTracks(): Tracks = Tracks.EMPTY
        override fun getTrackSelectionParameters(): TrackSelectionParameters = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
        override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {}
        override fun getMediaMetadata(): MediaMetadata = MediaMetadata.EMPTY
        override fun getCurrentManifest(): Any? = null
        override fun getCurrentWindowIndex(): Int = 0
        override fun getNextWindowIndex(): Int = -1
        override fun getPreviousWindowIndex(): Int = -1
        override fun getMediaItemCount(): Int = 0
        override fun getMediaItemAt(index: Int): MediaItem = throw IndexOutOfBoundsException()
        override fun isCurrentWindowDynamic(): Boolean = false
        override fun isCurrentMediaItemDynamic(): Boolean = false
        override fun isCurrentWindowLive(): Boolean = false
        override fun isCurrentMediaItemLive(): Boolean = false
        override fun getCurrentLiveOffset(): Long = C.TIME_UNSET
        override fun isCurrentWindowSeekable(): Boolean = false
        override fun isCurrentMediaItemSeekable(): Boolean = false
        override fun isPlayingAd(): Boolean = false
        override fun getCurrentAdGroupIndex(): Int = C.INDEX_UNSET
        override fun getCurrentAdIndexInAdGroup(): Int = C.INDEX_UNSET
        override fun getContentDuration(): Long = C.TIME_UNSET
        override fun getContentPosition(): Long = 0L
        override fun getContentBufferedPosition(): Long = 0L
        override fun getVideoSize(): VideoSize = VideoSize.UNKNOWN
        override fun getSurfaceSize(): Size = Size.ZERO
        override fun getCurrentCues(): CueGroup = CueGroup.EMPTY_TIME_ZERO
        override fun getDeviceInfo(): DeviceInfo = DeviceInfo.UNKNOWN
        override fun isDeviceMuted(): Boolean = false
        override fun setDeviceMuted(muted: Boolean) {}
        override fun setDeviceMuted(muted: Boolean, flags: Int) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun getSeekBackIncrement(): Long = C.DEFAULT_SEEK_BACK_INCREMENT_MS
        override fun getSeekForwardIncrement(): Long = C.DEFAULT_SEEK_FORWARD_INCREMENT_MS
        override fun getMaxSeekToPreviousPosition(): Long = 3000L
        override fun canAdvertiseSession(): Boolean = true
        override fun clearVideoSurface() {}
        override fun clearVideoSurface(surface: Surface?) {}
        override fun setVideoSurface(surface: Surface?) {}
        override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
        override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
        override fun setVideoSurfaceView(surfaceView: SurfaceView?) {}
        override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {}
        override fun setVideoTextureView(textureView: TextureView?) {}
        override fun clearVideoTextureView(textureView: TextureView?) {}
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession, controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailablePlayerCommands(
                Player.Commands.Builder()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SET_VOLUME)
                    .build()
            )
            .build()
    }

    fun initialize(controller: MetronomeController) {
        this.controller = controller
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(context, player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()
            .also { session ->
                session.player.volume = controller.state.value.volume
            }
        stateCollector = scope.launch {
            controller.state.collect { st ->
                lastState.value = st
                mediaSession?.player?.playWhenReady = st.runState == RunState.RUNNING
            }
        }
    }

    fun release() {
        stateCollector?.cancel()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
    }
}
