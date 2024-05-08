package com.mobile.vms.player.rtsp

import android.media.MediaCodecList
import android.net.Uri
import android.view.Surface
import com.mobile.vms.VMSMobileSDK
import com.mobile.vms.player.helpers.logSdk
import com.mobile.vms.player.rtsp.FrameQueue.FrameQueueCallback
import com.mobile.vms.player.rtsp.VideoDecodeCoroutine.VideoDecoderCallback
import com.mobile.vms.player.rtsp.utils.*
import com.mobile.vms.player.strategyplayer.PlayerProtocol.PlaybackState.*
import kotlinx.coroutines.*
import java.net.Socket
import java.time.Instant
import java.util.concurrent.atomic.*

const val MAX_COUNTER = 50

class RtspCoroutine(
	val actionRestartRtsp: (String?) -> Unit,
	val actionRtspStateChanged: (Int, Any?) -> Unit? = { _, _ -> },
	val actionShowEmptyScreen: (Boolean) -> Unit? = {},
	val isSoundAvailableCallback: (Boolean) -> Unit = {},
	val actionChangeWidthHeight: (width: Int, height: Int) -> Unit,
): FrameQueueCallback, VideoDecoderCallback {

	private val TAG = RtspCoroutine::class.java.simpleName
	var videoDecodeCoroutine: VideoDecodeCoroutine? = null
	var audioDecodeCoroutines: AudioDecodeCoroutines? = null
	private var videoMimeType: String = "video/avc"
	var surfaceWidth: Int = 1920
	var surfaceHeight: Int = 1080
	private var videoFrameQueue: FrameQueue = FrameQueue(this)
	private var audioFrameQueue: FrameQueue = FrameQueue(this)
	private val DEFAULT_RTSP_PORT = 554
	private var audioMimeType: String = "audio/mp4a-latm"
	private var audioSampleRateHz: Int = 0
	private var audioChannelCount: Int = 0
	private var audioCodecConfig: ByteArray? = null
	var videoUrl = ""
	var isSoundEnabled: Boolean = false
	var surface: Surface? = null
		set(value) {
			logSdk(TAG, "Surface was set in THREAD = ${this}")
			field = value
		}
	var counterError = 0
	var rtspClientDisconnected: AtomicBoolean = AtomicBoolean(false)
	var rtspCoroutineJob: Job? = null
	val coroutineScopeIO: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
	private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
		throwable.printStackTrace()
	}

	fun run() {
		rtspCoroutineJob = coroutineScopeIO.launch {
			logSdk(TAG, "Run coroutine name = ${Thread.currentThread().name}")
			if (videoUrl.endsWith("&")) {
				videoUrl = videoUrl.dropLast(1)
			}
			val uri: Uri = Uri.parse(videoUrl)
			val port = if (uri.port == -1) DEFAULT_RTSP_PORT else uri.port
			var socket: Socket? = null
			try {
				logSdk(
					TAG,
					"Connecting to ${uri.host.toString()}:$port scheme = ${uri.scheme}"
				)
				socket = if (uri.scheme?.lowercase() == "rtsps") {
					NetUtils.createSslSocketAndConnect(uri.host.toString(), port, 10000)
				} else {
					NetUtils.createSocketAndConnect(
						uri.host.toString(),
						port,
						10000
					) // java.net.SocketException: Broken pipe - unable to handle, unknown issue
				}
				// Blocking call until stopped variable is true or connection failed

				val array: Array<String?> =
					videoUrl.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				// rtsp://<username>:<password>@<host address>
				val isBasicAuth = array[2]?.contains("@") == true

				val uriRtsp = if (isBasicAuth) {
					val arrayUrl = videoUrl.split("@")
					"${uri.scheme}://${arrayUrl[1]}"
				} else {
					uri.toString()
				}
				val rtspClient =
					RtspClient.Builder(socket, uriRtsp, rtspClientDisconnected, proxyClientListener)
						.requestVideo(true)
						.requestAudio(true)
						.withDebug(VMSMobileSDK.isDebuggable)

				if (isBasicAuth) {
					val name = array[1]?.substring(2)
					val pwd = array[2]?.split("@")?.get(0)
					logSdk(TAG, "name = $name pwd = $pwd")
					rtspClient.withCredentials(name, pwd)
				}

				// java.net.SocketException: Broken pipe or Connection reset - below
				rtspClient.build().execute()
				NetUtils.closeSocket(socket)
			} catch (e: Throwable) {
				counterError++
				logSdk(TAG, "RTSP Throwable counterError = $counterError")
				if (counterError > MAX_COUNTER) {
					NetUtils.closeSocket(socket)
					rtspClientDisconnected.set(true)
					delay(200) // to wait to release old decoders
					actionRestartRtsp.invoke(null)
				}
				e.printStackTrace()
			}
		}
	}

	private val proxyClientListener = object: RtspClient.RtspClientListener {
		override fun onRtspDisconnected() {
			logSdk(TAG, "onRtspDisconnected() THREAD = ${this@RtspCoroutine}")
			coroutineScopeIO.launchInMain {
				actionRtspStateChanged.invoke(STATE_RELEASED.state, null)
			}
		}

		override fun onRtspFailed(message: String?) {
			logSdk(TAG, "onRtspFailed(message = \"$message\")")
//			showError(needShow = true) // todo - check after mse fixing, was here when error in the end of archive with status code -1
		}

		override fun onRtspFailedStatus204() {
			coroutineScopeIO.launch {
				withContext(Dispatchers.Main) {
					actionRtspStateChanged.invoke(STATE_ERROR_OCCURRED.state, RtspError204())
				}
			}
			logSdk(TAG, "error 204")
		}

		override fun onRtspConnected(sdpInfo: RtspClient.SdpInfo) {
			logSdk(TAG, "THREAD = ${this@RtspCoroutine}")
			showError(needShow = false)
			coroutineScopeIO.launch {
				withContext(Dispatchers.Main) {
					actionRtspStateChanged.invoke(STATE_STARTED.state, sdpInfo.time)
				}
			}
			sdpInfo.videoTrack?.let { videoTrack ->
				videoFrameQueue.clear()
				videoMimeType = when (videoTrack.videoCodec) {
					RtspClient.VIDEO_CODEC_H264 -> "video/avc"
					RtspClient.VIDEO_CODEC_H265 -> "video/hevc"
					else -> return
				}
				// Для H264 используем только SPS и PPS
				val sps: ByteArray? = videoTrack.sps
				val pps: ByteArray? = videoTrack.pps
				// Для H265 добавляем VPS
				val vps: ByteArray? = if (videoTrack.videoCodec == RtspClient.VIDEO_CODEC_H265) videoTrack.vps else null

				// Initialize decoder
				if (sps != null && pps != null) {
					// Если есть VPS (только для H265), объединяем все три NAL единицы вместе
					val data: ByteArray = if (vps != null) {
						ByteArray(sps.size + pps.size + vps.size).apply {
							vps.copyInto(this, 0)
							sps.copyInto(this, vps.size)
							pps.copyInto(this, vps.size + sps.size)
						}
					} else {
						ByteArray(sps.size + pps.size).apply {
							sps.copyInto(this, 0)
							pps.copyInto(this, sps.size)
						}
					}
					videoFrameQueue.push(FrameQueue.Frame(data, 0, data.size, 0))
				} else {
					logSdk(TAG, "RTSP SPS and PPS NAL units missed in SDP")
				}
			}
			val isSoundAvailable =
				sdpInfo.audioTrack != null && sdpInfo.audioTrack!!.audioCodec == RtspClient.AUDIO_CODEC_AAC
			if (isSoundAvailable) {
				audioFrameQueue.clear()
				audioSampleRateHz = sdpInfo.audioTrack!!.sampleRateHz
				audioChannelCount = sdpInfo.audioTrack!!.channels
				audioCodecConfig = sdpInfo.audioTrack!!.config
				logSdk(
					TAG,
					"audioCodecConfig = $audioCodecConfig size = ${audioCodecConfig?.size}, audioChannelCount = $audioChannelCount, audioSampleRate = $audioSampleRateHz"
				)
			}
			isSoundAvailableCallback.invoke(isSoundAvailable)
			onRtspClientConnected()
		}

		override fun onRtspFailedUnauthorized() {
			logSdk(
				TAG,
				"onRtspFailedUnauthorized() - RTSP username or password is incorrect"
			)
			showError(needShow = true)
		}

		override fun onRtspVideoNalUnitReceived(
			data: ByteArray,
			offset: Int,
			length: Int,
			timestamp: Long
		) {
			showError(needShow = false)
			if (length > 0)
				videoFrameQueue.push(FrameQueue.Frame(data, offset, length, timestamp))
		}

		override fun onRtspAudioSampleReceived(
			data: ByteArray,
			offset: Int,
			length: Int,
			timestamp: Long
		) {
			if (length > 0 && isSoundEnabled) {
//				logSdk(
//					"RtspThread",
//					"onRtspAudioSampleReceived length = $length, timestamp=$timestamp)"
//				)
				// example - sampleRate: 48000, bufferSize: 7696
				audioFrameQueue.push(FrameQueue.Frame(data, offset, length, timestamp))
			}
		}

		override fun onRtspConnecting() {
			logSdk(TAG, "RTSP connecting")
		}

		override fun onRtspFailedSocketException(message: String?) {
			logSdk(TAG, "FAILED message=\"$message\")")
			rtspClientDisconnected.set(true)
			actionRestartRtsp.invoke(null)
		}

		override fun onRtspFailedStatus302(location: String) {
			logSdk(TAG, "RTSP onRtspFailedStatus 302")
			rtspClientDisconnected.set(true)
			actionRestartRtsp.invoke(location)
		}
	}

	init {
		logSdk(TAG, "init THREAD RtspCoroutine = $this")
	}

	fun onRtspClientReleased() {
		coroutineScopeIO.launch {
			println("${Instant.now()}: onRtspClientReleased on...: ${Thread.currentThread().name}")
			logSdk(TAG, "onRtspClientReleased on...: ${Thread.currentThread().name}")
			rtspClientDisconnected.set(true)
			stopDecoders()
			delay(200) // to waiting stop video and audio decoders
		}
//			logSdk(TAG, "onRtspClientReleased()")
	}

	fun stopDecoders() {
		stopVideo()
		stopAudio()
		logSdk(TAG, "stopDecoders()")
	}

	fun stopVideo() {
		videoDecodeCoroutine?.stopAsync()
		videoDecodeCoroutine = null
	}

	fun stopAudio() {
		audioDecodeCoroutines?.stopAsync()
		audioDecodeCoroutines = null
	}

	fun playVideo() {
		if (surface != null) {
			videoDecodeCoroutine = VideoDecodeCoroutine(
				surface,
				videoMimeType,
				surfaceWidth,
				surfaceHeight,
				videoFrameQueue,
				videoDecoderCallback = this,
				actionPlaying = {
					coroutineScopeIO.launchInMain {
						actionRtspStateChanged.invoke(STATE_PLAYING.state, it)
					}
				}, actionChangeTimeline = {
					coroutineScopeIO.launchInMain {
						actionRtspStateChanged.invoke(STATE_TIMELINE_CHANGED.state, it)
					}
				}, actionRestartVideo = {
					proxyClientListener.onRtspFailedStatus204()
				}, actionChangeWidthHeight = { width: Int, height: Int ->
					coroutineScopeIO.launchInMain {
						actionRtspStateChanged.invoke(STATE_WIDTH_HEIGHT_READY.state, Pair(width, height))
					}
					logSdk(TAG, "surfaceWidth = $width surfaceHeight = $height")
				})
			videoDecodeCoroutine?.run()
		}
	}

	fun playAudio() {
		logSdk(
			TAG,
			"isSoundEnabled: $isSoundEnabled, audioMimeType: $audioMimeType audioSampleRate: $audioSampleRateHz, audioChannelCount: $audioChannelCount"
		)
		// check has audio stream or not
		if (audioChannelCount > 0 && audioSampleRateHz > 0 && isSoundEnabled) {
			logSdk(TAG, "Starting audio decoder with mime type \"$audioMimeType\"")
			if (isCodecSupported(audioMimeType) && audioDecodeCoroutines == null) {
				audioDecodeCoroutines = AudioDecodeCoroutines(
					audioMimeType,
					audioSampleRateHz,
					audioChannelCount,
					audioCodecConfig,
					audioFrameQueue
				)
				audioDecodeCoroutines?.run()
			} else {
				logSdk(TAG, "Codec $audioMimeType is not supported on this device")
			}
		}
	}

	fun onRtspClientConnected() {
		logSdk(TAG, "onRtspClientConnected, playVideo")
		playVideo()
		playAudio()
		coroutineScopeIO.launchInMain {
			actionRtspStateChanged.invoke(STATE_READY.state, null)
		}
	}

	fun showError(needShow: Boolean = false, time: Long = 0) {
		coroutineScopeIO.launchInMain(time) {
			if (needShow) {
				actionRtspStateChanged.invoke(
					STATE_ERROR_OCCURRED.state,
					RtspErrorEmptyStream(needShow)
				)
				logSdk(TAG, "ERROR_OCCURRED EmptyStream")
			}
		}
	}

	private fun isCodecSupported(mimeType: String): Boolean {
		val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
		return mediaCodecList.codecInfos.any { codecInfo ->
			logSdk(TAG, "codecInfo = $codecInfo")
			codecInfo.supportedTypes.contains(mimeType)
		}
	}

	override fun finishPopDueEmptyFrames() {
		logSdk(TAG, "finishPopDueEmptyFrames()")
		coroutineScopeIO.launchInMain {
			actionRtspStateChanged.invoke(STATE_ERROR_QUEUE_FRAME.state, null)
		}
	}

	override fun finishDecoderDueEmptyFrames() {
		logSdk(TAG, "finishDecoderDueEmptyFrames()")
		rtspClientDisconnected.set(true)
		stopDecoders()
		coroutineScopeIO.launchInMain {
			actionRtspStateChanged.invoke(STATE_ERROR_VIDEO_DECODER.state, null)
		}
	}

	override fun finishDecoderIncompatibleFormat() {
		logSdk(TAG, "finishDecoderDueEmptyFrames()")
		rtspClientDisconnected.set(true)
		stopDecoders()
		coroutineScopeIO.launchInMain {
			actionRtspStateChanged.invoke(STATE_ERROR_DEVICE_PERFORMANCE.state, null)
		}
	}

}


/** Statuses in RtspClient:

//OPTIONS rtsp://xxx.xxx/stream3/live RTSP/1.0
//CSeq: 1
//User-Agent: Lavf58.67.100
//

//OPTIONS rtsp://xxx.xxx/stream3/live RTSP/1.0
//CSeq: 2
//User-Agent: Lavf58.67.100
//Session: 18338180692471
//Authorization: Basic YWRtaW4xOmFkbWlu
//
//
//DESCRIBE rtsp://xxx.xxx/stream3/live RTSP/1.0
//Accept: application/sdp
//CSeq: 3
//User-Agent: Lavf58.67.100
//Session: 18338180692471
//Authorization: Basic YWRtaW4xOmFkbWlu
//
//
//SETUP rtsp://xxx.xxx/stream3/live/track=video RTSP/1.0
//Transport: RTP/AVP/TCP;unicast;interleaved=0-1
//CSeq: 4
//User-Agent: Lavf58.67.100
//Session: 18338180692471
//Authorization: Basic YWRtaW4xOmFkbWlu
//
//
//SETUP rtsp://xxx.xxx/stream3/live/track=audio RTSP/1.0
//Transport: RTP/AVP/TCP;unicast;interleaved=2-3
//CSeq: 5
//User-Agent: Lavf58.67.100
//Session: 18338180692471
//Authorization: Basic YWRtaW4xOmFkbWlu
//
//
//PLAY rtsp://xxx.xxx/stream3/live RTSP/1.0
//Range: npt=0.000-
//CSeq: 6
//User-Agent: Lavf58.67.100
//Session: 18338180692471
//Authorization: Basic YWRtaW4xOmFkbWlu
//
//
//TEARDOWN rtsp://xxx.xxx/stream3/live RTSP/1.0
//CSeq: 7
//User-Agent: Lavf58.67.100
//Session: 18338180692471
//Authorization: Basic YWRtaW4xOmFkbWlu
 */