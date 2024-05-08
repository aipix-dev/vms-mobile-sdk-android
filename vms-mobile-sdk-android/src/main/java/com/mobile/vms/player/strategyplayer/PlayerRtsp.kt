package com.mobile.vms.player.strategyplayer

import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.rtsp.RtspCoroutine
import com.mobile.vms.player.strategyplayer.PlayerProtocol.PlaybackState.*
import com.mobile.vms.settings
import kotlinx.coroutines.isActive

class PlayerRtsp(val activity: AppCompatActivity): PlayerProtocol() {

	private val TAG = PlayerRtsp::class.java.simpleName

	override var videoUrl = ""
		set(value) {
			field = value
			rtspCoroutine?.videoUrl = value
		}
	override var speedX = speed_X_1
		set(value) {
			if (field != value) {
				logSdk(TAG, "field = $field, value = $value")
				field = value
				setSpeed()
				surface?.let { initPlayer(it) }
				actionStateChanged.invoke(STATE_SPEED_CHANGED.state, null)
			}
		}

	override var isSoundAvailable: Boolean = false
	override var playbackState: PlaybackState = STATE_NONE
	override var playWhenReady = false
	override var width: Int = 1920
	override var height: Int = 1280
	override var sound = if (settings.enabledAudio) 1f else 0f
		set(value) {
			field = value
			rtspCoroutine?.isSoundEnabled = value == 1f
			if (value == 1f) {
				rtspCoroutine?.playAudio()
			} else {
				rtspCoroutine?.stopAudio()
			}
		}
	override var actionStateChanged: (state: Int, data: Any?) -> Unit = { _, _ -> }
	override var surface: Surface? = null
	override var isLive: Boolean = true
	override var currentManifest: Any = ""
	private var rtspCoroutine: RtspCoroutine? = null

	override fun initPlayer(surface: Surface) {
		stopPlayer()
		logSdk(TAG, "initPlayer RTPS")
		setSpeed()
		if (this.surface != surface){
			this.surface = surface
		} else {
			logSdk(TAG, "initPlayer - avoid surface reinitialization")
		}
		/**
		 * To waiting for start another coroutine with new socket connection and break after few seconds if couldn't disconnect
		 */
		val startTime = System.currentTimeMillis()
		if (rtspCoroutine != null) {
			while (rtspCoroutine?.rtspClientDisconnected?.get() == false &&
				rtspCoroutine?.videoDecodeCoroutine?.decoder != null
			) {
//				logSdk(TAG, "initPlayer - while ${Thread.currentThread().name}")
				if (System.currentTimeMillis() - startTime > 4000) {
//					logSdk(TAG, "initPlayer - Timeout exceeded")
					break
				}
			}
		}
		rtspCoroutine =
			RtspCoroutine(actionRestartRtsp = { location ->
				logSdk("PlayerRtsp", "RtspThread try to reinitialize, surface = ${this.surface}")
				if (!location.isNullOrEmpty()) videoUrl = location
				this.surface?.let { initPlayer(it) } // think to call request live or archive
			}, actionRtspStateChanged = { state, data ->
				actionStateChanged.invoke(state, data)
			}, isSoundAvailableCallback = { isSoundAvailable ->
				this@PlayerRtsp.isSoundAvailable = isSoundAvailable
			}, actionChangeWidthHeight = { w: Int, h: Int ->
				width = w
				height = h
			})
		startPlayer()
	}

	override fun startPlayer() {
		logSdk(TAG, "startPlayer")
		sound = if (settings.enabledAudio) 1f else 0f
		try {
			if (videoUrl.isEmpty() || surface == null) return
			rtspCoroutine?.surface = surface
			rtspCoroutine?.videoUrl = videoUrl
			rtspCoroutine?.isSoundEnabled = settings.enabledAudio
			rtspCoroutine?.run()
			playWhenReady = true
		} catch (e: Exception) {
			rtspCoroutine?.onRtspClientReleased()
			e.printStackTrace()
		}
	}

	override fun playPlayer() {
		actionStateChanged.invoke(STATE_RTSP_REPLAY_ARCHIVE.state, null)
	}

	override fun pausePlayer() {
		logSdk(TAG, "pausePlayer RTPS")
		stopPlayer()
	}

	override fun stopPlayer() {
		logSdk(TAG, "stopPlayer RTPS")
		if (rtspCoroutine == null || rtspCoroutine?.rtspCoroutineJob?.isActive == false)
			return
		rtspCoroutine?.stopDecoders()
		rtspCoroutine?.rtspClientDisconnected?.set(true)
		actionStateChanged.invoke(STATE_STOPPED.state, null)
		playWhenReady = false
	}

	override fun releasePlayer() {
		logSdk(
			TAG,
			"releasePlayer 2 coroutineScopeIO is isActive = ${rtspCoroutine?.coroutineScopeIO?.isActive}"
		)
		if (rtspCoroutine == null || rtspCoroutine?.rtspCoroutineJob?.isActive == false)
			return
		rtspCoroutine?.onRtspClientReleased()
		actionStateChanged.invoke(STATE_RELEASED.state, null)
	}

	override fun setSpeed() {
		logSdk(TAG, "setSpeed speedX = $speedX")
		if (videoUrl.endsWith("&")) {
			videoUrl = videoUrl.dropLast(1)
		}
		if (videoUrl.contains("&speed=")) {
			videoUrl = videoUrl.split("&speed=")[0]
		}
		videoUrl += if (speedX.toInt() == 1) {
			""
		} else if (speedX == 0.5f) {
			"&speed=${speedX}"
		} else {
			"&speed=${speedX.toInt()}"
		}
		logSdk(TAG, "***setSpeed videoUrl = $videoUrl")
	}
}