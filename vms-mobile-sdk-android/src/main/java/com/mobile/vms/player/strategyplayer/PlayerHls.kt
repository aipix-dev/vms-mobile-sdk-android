//package com.mobile.vms.player.strategyplayer
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.net.Uri
//import android.view.Surface
//import androidx.media3.common.*
//import androidx.media3.datasource.*
//import androidx.media3.exoplayer.*
//import androidx.media3.exoplayer.hls.HlsMediaSource
//import com.mobile.vms.player.helpers.*
//import com.mobile.vms.player.strategyplayer.PlayerProtocol.PlaybackState.*
//import com.mobile.vms.player.ui.VMSPlayerFragment
//import com.mobile.vms.settings
//
///**
// * It will be call like HlsPlayer in our SDK
// */
//
//@SuppressLint("UnsafeOptInUsageError")
//class PlayerHls(val context: Context): PlayerProtocol(), Player.Listener {
//	private val TAG = "PlayerHls"
//	private var MIN_BUFFER_DURATION = 5000
//	private var MAX_BUFFER_DURATION = 20000
//	private var BUFFER_FOR_PLAYBACK_DURATION = 1000
//	private var BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_DURATION = 2000
//	override var videoUrl = ""
//	override var speedX = speed_X_1
//		set(value) {
//			field = value
//			setSpeed()
//		}
//	override var playWhenReady: Boolean = true
//		set(value) {
//			field = value
//			player?.playWhenReady = value
//		}
//	override var isSoundAvailable: Boolean = true
//	override var playbackState: PlaybackState = STATE_NONE
//	override var width = 1920
//	override var height = 1280
//	override var sound = if (settings.enabledAudio) 1f else 0f
//		set(value) {
//			field = value
//			player?.volume = value
//		}
//	override var actionStateChanged: (state: Int, data: Any?) -> Unit = { state, data -> }
//	override var surface: Surface? = null
//	override var isLive: Boolean = true
//
//	var player: ExoPlayer? = null
//	override var currentManifest: Any = player?.currentManifest ?: Any()
//
//	override fun initPlayer(surface: Surface) {
//		logSdk(TAG, "initPlayer HLS")
//		try {
//			if (player == null) {
//				val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
//					MIN_BUFFER_DURATION,
//					MAX_BUFFER_DURATION,
//					BUFFER_FOR_PLAYBACK_DURATION,
//					BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_DURATION
//				).build()
//				val renderersFactory =
//					DefaultRenderersFactory(context).setExtensionRendererMode(
//						DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
//					)
//				player =
//					ExoPlayer.Builder(context, renderersFactory).setLoadControl(loadControl).build()
//				player?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
//				player?.playbackParameters = PlaybackParameters(speedX, speedX)
//				player?.addListener(this)
//				//            exoPlayer?.addAnalyticsListener(object : AnalyticsListener {
//				//                override fun onLoadCompleted(
//				//                    eventTime: AnalyticsListener.EventTime,
//				//                    loadEventInfo: LoadEventInfo,
//				//                    mediaLoadData: MediaLoadData
//				//                ) {
//				//                    super.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
//				//                    if (isNeedAddTimeToLive && currentLiveCalendar != null) {
//				//                        val additionTime = eventTime.currentPlaybackPositionMs  //get extra time in ms
//				//                        currentLiveCalendar!!.timeInMillis += additionTime  //set correct time for live
//				//                        isNeedAddTimeToLive = false //add this only once for loaded url
//				//                    }
//				//                }
//				//            })
//			}
//			player?.setVideoSurface(surface) // bind player & view
//		} catch (e: Exception) {
//			e.message
//		}
//	}
//
//	override fun startPlayer() {
//		try {
//			val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
//			val hlsMediaSource =
//				HlsMediaSource.Factory(dataSourceFactory).setAllowChunklessPreparation(true)
//					.createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
//			player?.setMediaSource(hlsMediaSource)
//			player?.prepare()
//			sound = if (settings.enabledAudio) 1f else 0f
//			playPlayer() // need call in startPlayer()
//		} catch (e: Exception) {
//			e.message
//		}
//	}
//
//	override fun playPlayer() {
//		playWhenReady = true
//	}
//
//	override fun pausePlayer() {
//		playWhenReady = false
//	}
//
//	override fun stopPlayer() {
//		player?.stop()
//	}
//
//	override suspend fun releasePlayerRtsp() {
//
//	}
//
//	override fun releasePlayer() {
//		player?.removeListener(this)
//		player?.stop()
//		player?.release()
//		player = null
//	}
//
//	override fun setSpeed() {
//		if (!VMSPlayerFragment.isLive) {
//			if (player?.playbackParameters?.speed != speedX) {
//				player?.playbackParameters = PlaybackParameters(speedX, speedX)
//				if (surface != null) player?.setVideoSurface(surface)
//			}
//		} else {
//			player?.playbackParameters = PlaybackParameters(speedX, speedX)
//			if (surface != null) player?.setVideoSurface(surface)
//		}
//	}
//
//	@Deprecated("Deprecated in Java")
//	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//		logSdk("Player_STATE", "playbackState = ${playbackState}")
//		actionStateChanged.invoke(playbackState, playWhenReady)
//	}
//
//	override fun onTimelineChanged(timeline: Timeline, reason: Int) {
//		super.onTimelineChanged(timeline, reason)
//		logSdk("TAG", "onTimelineChanged reason1 = $reason")
//		if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
//			actionStateChanged.invoke(STATE_TIMELINE_CHANGED.state, reason)
//		}
//	}
//
//	override fun onPlayerError(e: PlaybackException) {
//		actionStateChanged.invoke(STATE_ERROR_OCCURRED.state, e)
//	}
//
//}