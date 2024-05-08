package com.mobile.vms.player.strategyplayer

import android.view.Surface
import com.mobile.vms.settings

abstract class PlayerProtocol {

	open var videoUrl: String = ""
	open var speedX: Float = 1f
	open var sound: Float = if (settings.enabledAudio) 1f else 0f
	open var playbackState: PlaybackState = PlaybackState.STATE_NONE
	open var playWhenReady: Boolean = false
	open var isSoundAvailable: Boolean = false
	open var width: Int = 1920
	open var height: Int = 1280
	open var actionStateChanged: (state: Int, data: Any?) -> Unit = { state, data -> }
	open var surface: Surface? = null
	open var isLive = true
	abstract var currentManifest: Any

	abstract fun initPlayer(surface: Surface)
	abstract fun startPlayer()
	abstract fun playPlayer()
	abstract fun pausePlayer()
	abstract fun stopPlayer()
	abstract fun releasePlayer()
//	abstract suspend fun releasePlayerRtsp()
	abstract fun setSpeed()

	enum class PlaybackState(val state: Int) {
		/**
		 * The player has not state
		 */
		STATE_NONE(0),

		/**
		 * The player is idle, meaning it holds only limited resources. The player must be [ ][.prepare] before it will play the media.
		 *
		 * Similar exoplayer
		 */
		STATE_IDLE(1),

		/**
		 * The player is not able to immediately play the media, but is doing work toward being able to do
		 * so. This state typically occurs when the player needs to buffer more data before playback can
		 * start.
		 *
		 * Similar exoplayer
		 */
		STATE_BUFFERING(2),

		/**
		 * The player is able to immediately play from its current position. The player will be playing if
		 * [.getPlayWhenReady] is true, and paused otherwise.
		 *
		 * Similar exoplayer
		 */
		STATE_READY(3),

		/** The player has finished playing the media.
		 *
		 * Similar exoplayer
		 */
		STATE_ENDED(4),

		/** The player was started  */
		STATE_STARTED(11),

		/** The player was stopped  */
		STATE_STOPPED(12),

		/** The player was playing, mean showing video on surface  */
		STATE_PLAYING(13),

		/** The player timeline changed for the first time, triggered only once for every loading */
		STATE_TIMELINE_CHANGED(14),

		/** The player speed changed  */
		STATE_SPEED_CHANGED(15),

		/** The player need replay archive after paused  */
		STATE_RTSP_REPLAY_ARCHIVE(16),

		/** The player was set width and height */
		STATE_WIDTH_HEIGHT_READY(17),

		/** The player error occurred  */
		STATE_ERROR_OCCURRED(600),

		/** The player got a lot of empty frames from video decoder */
		STATE_ERROR_VIDEO_DECODER(601),

		/** The player got a lot of empty frames from queue-frame */
		STATE_ERROR_QUEUE_FRAME(602),

		/** An error occurred due to the performance of the device*/
		STATE_ERROR_DEVICE_PERFORMANCE(603),

		/** The player was released  */
		STATE_RELEASED(666),
	}

}