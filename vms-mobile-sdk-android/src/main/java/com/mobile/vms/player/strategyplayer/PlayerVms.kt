package com.mobile.vms.player.strategyplayer

import android.view.Surface
import com.mobile.vms.player.helpers.logSdk
import com.mobile.vms.settings

class PlayerVms(private var playerProtocol: PlayerProtocol) {

	private val TAG = PlayerVms::class.java.simpleName

//	fun isRtsp() = playerProtocol.videoUrl.startsWith(VIDEO_TYPE_RTSP)
	fun isRtsp() = true // now only RTSP

	init {
		setPlayerProtocol(playerProtocol)
	}

	/**
	 * This method need to change player on the fly - rtsp/hls
	 */
	fun setPlayerProtocol(playerProtocol: PlayerProtocol) {
		logSdk(TAG, "setPlayerProtocol")
		this.playerProtocol = playerProtocol
	}

	var playWhenReady: Boolean = false
		get() = playerProtocol.playWhenReady
		set(value) {
			field = value
			playerProtocol.playWhenReady = value
		}
	var speedX: Float = playerProtocol.speedX
		set(value) {
			field = value
			playerProtocol.speedX = value
		}
	var width: Int = playerProtocol.width
		set(value) {
			field = value
			playerProtocol.width = value
		}
	var height: Int = playerProtocol.height
		set(value) {
			field = value
			playerProtocol.height = value
		}
	var playbackState: Int = playerProtocol.playbackState.state
	var sound: Float = if (settings.enabledAudio) 1f else 0f
		set(value) {
			field = value
			playerProtocol.sound = value
		}
	var videoUrl: String = playerProtocol.videoUrl
		set(value) {
			field = value
			playerProtocol.videoUrl = value
		}
	val isSoundAvailable: Boolean
		get() {
			return playerProtocol.isSoundAvailable
		}
	var actionStateChanged: (state: Int, any: Any?) -> Unit = playerProtocol.actionStateChanged
		set(value) {
			field = value
			playerProtocol.actionStateChanged = value
		}

	fun initPlayer(surface: Surface) {
		logSdk(TAG, "initPlayer")
		playerProtocol.initPlayer(surface)
	}

	fun startPlayer() {
		logSdk(TAG, "startPlayer")
		playerProtocol.playWhenReady = true
		playerProtocol.startPlayer()
	}

	fun play() {
		logSdk(TAG, "play")
		playerProtocol.playWhenReady = true
		playerProtocol.playPlayer()
	}

	fun pause() {
		logSdk(TAG, "pause")
		playerProtocol.playWhenReady = false
		playerProtocol.pausePlayer()
	}

	fun stop() {
		logSdk(TAG, "stop")
		playerProtocol.playWhenReady = false
		playerProtocol.stopPlayer()
	}

	fun release() {
		logSdk(TAG, "release")
		playerProtocol.releasePlayer()
	}

}