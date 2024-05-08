package com.mobile.vms.player.rtsp

import android.media.*
import android.util.Log
import android.view.Surface
import com.mobile.vms.player.helpers.logSdk
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class VideoDecodeCoroutine(
	private var surface: Surface?,
	private val mimeType: String,
	private var width: Int,
	private var height: Int,
	private val videoFrameQueue: FrameQueue,
	private val videoDecoderCallback: VideoDecoderCallback,
	private val actionPlaying: (Long) -> Unit,  // with dif in timestamps between two frames
	private val actionChangeTimeline: (Long) -> Unit,
	private val actionRestartVideo: () -> Unit,
	private val actionChangeWidthHeight: (width: Int, height: Int) -> Unit,
) {

	companion object {
		private val TAG: String = VideoDecodeCoroutine::class.java.simpleName
		private val DEQUEUE_INPUT_TIMEOUT_US = TimeUnit.MILLISECONDS.toMicros(500)
		private val DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US = TimeUnit.MILLISECONDS.toMicros(100)

		/**
		 * Counter equals 10 frames.
		 */
		const val MAX_COUNTER_EMPTY_FRAMES_DECODER = 10
	}

	interface VideoDecoderCallback {
		/**
		 * Finish pop queue, to make request for show live or archive
		 */
		fun finishDecoderDueEmptyFrames()
		fun finishDecoderIncompatibleFormat()
	}

	private var exitFlag: AtomicBoolean = AtomicBoolean(false)
	private var previousTimestamp = 0L
	private var currentTimestamp = 0L
	private var isTimelineChanged = false
	private var counterEmptyFrames = 0
	var decoder: MediaCodec? = null
	var tryCount = 0
	var outputFormatChanged = false
	var delayCheckStarted = false
	var compatibleFormatDetected = false

	val coroutineScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
	val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
		throwable.printStackTrace()
	}

	fun stopAsync() {
		exitFlag.set(true)
	}

	private fun getDecoderSafeWidthHeight(decoder: MediaCodec): Pair<Int, Int> {
		val capabilities = decoder.codecInfo.getCapabilitiesForType(mimeType).videoCapabilities
		return if (capabilities.isSizeSupported(width, height)) {
			Pair(width, height)
		} else {
			val widthAlignment = capabilities.widthAlignment
			val heightAlignment = capabilities.heightAlignment
			Pair(
				ceilDivide(width, widthAlignment) * widthAlignment,
				ceilDivide(height, heightAlignment) * heightAlignment
			)
		}
	}

	fun ceilDivide(numerator: Int, denominator: Int): Int {
		return (numerator + denominator - 1) / denominator
	}

	fun run() {
		coroutineScope.launch {
			try {
				decoder = MediaCodec.createDecoderByType(mimeType)
			} catch (e: Exception) {
				logSdk(TAG, "Error creating decoder: ${e.message}")
				return@launch
			}

//			decoder?.setOnErrorListener { codec, e ->
//				logSdk(TAG, "MediaCodec error received: ${e.errorCode}, ${e.diagnosticInfo}")
//				// Здесь можно реализовать логику в ответ на ошибку
//				videoDecoderCallback.finishDecoderDueEmptyFrames()
//			}

			val widthHeight = getDecoderSafeWidthHeight(decoder!!)
			val format =
				MediaFormat.createVideoFormat(mimeType, widthHeight.first, widthHeight.second)
			logSdk(
				TAG,
				"Configuring surface ${widthHeight.first}x${widthHeight.second} mimeType = w/ '$mimeType', max instances: ${
					decoder?.codecInfo?.getCapabilitiesForType(mimeType)?.maxSupportedInstances
				}"
			)

			if (!exitFlag.get() || surface == null) {
				try {
					decoder?.configure(format, surface, null, 0)
				} catch (e: Exception) {
					logSdk(TAG, "Error configuring decoder: ${e.message}")
					decoder?.reset()
					delay(2000)
					try {
						decoder = MediaCodec.createDecoderByType(mimeType)
						decoder?.configure(format, surface, null, 0)
					} catch (e: Exception) {
						logSdk(TAG, "Error configuring decoder: 2: ${e.message}")
						stopAsync()
						delay(2000)
						counterEmptyFrames = 0
						actionRestartVideo.invoke()
						return@launch
					}
				}
			} else {
				counterEmptyFrames = 0
				logSdk(TAG, "Exiting due to exitFlag being set")
				return@launch
			}

			try {
				decoder?.start()
			} catch (e: Exception) {
				logSdk(TAG, "Error starting decoder: ${e.message}")
				delay(2000)
				counterEmptyFrames = 0
				actionRestartVideo.invoke()
			}
			// handle when start  - java.lang.IllegalStateException: start() is valid only at Configured state; currently at Uninitialized state

			logSdk(TAG, "Started surface decoder, mimeType = $mimeType")

			val bufferInfo = MediaCodec.BufferInfo()
			var currentTimeToStartDecoder = System.currentTimeMillis()

			try {
				// Main loop
				while (!exitFlag.get()) {
					val inIndex: Int = decoder!!.dequeueInputBuffer(DEQUEUE_INPUT_TIMEOUT_US)
					if (inIndex >= 0) {
						// fill inputBuffers[inputBufferIndex] with valid data
						val byteBuffer: ByteBuffer? = decoder!!.getInputBuffer(inIndex)
						byteBuffer?.rewind()

						// Preventing BufferOverflowException
						// if (length > byteBuffer.limit()) throw DecoderFatalException("Error")

						if (exitFlag.get() || !isActive) break // handled interrupted exception

						// videoFrameQueue.queue - check for limit 100
//						if ()

						val frame = videoFrameQueue.pop()

						if (frame == null) {
							logSdk(TAG, "Empty video frame from decoder")
							// Release input buffer
							decoder?.queueInputBuffer(inIndex, 0, 0, 0L, 0)
							counterEmptyFrames++
							logSdk(
								TAG,
								"Cannot get frame, queue is empty counterEmptyFrames = $counterEmptyFrames"
							)
							if (counterEmptyFrames == MAX_COUNTER_EMPTY_FRAMES_DECODER) {
								videoDecoderCallback.finishDecoderDueEmptyFrames()
								counterEmptyFrames = 0
							}
						} else {
							counterEmptyFrames = 0
							byteBuffer?.put(frame.data, frame.offset, frame.length)
							decoder?.queueInputBuffer(
								inIndex,
								frame.offset,
								frame.length,
								frame.timestamp,
								0
							)
							currentTimestamp = frame.timestamp
							if (previousTimestamp == 0L) previousTimestamp = currentTimestamp

							/// todo start audio with currentTimestamp
						}
					}

					if (exitFlag.get() || !isActive) break // handled interrupted exception
					when (val outIndex = decoder!!.dequeueOutputBuffer(
						bufferInfo,
						DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US
					)) {
						MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

							val outputFormat = decoder!!.outputFormat
							var w = outputFormat.getInteger(MediaFormat.KEY_WIDTH)
							var h = outputFormat.getInteger(MediaFormat.KEY_HEIGHT)

							// check for next calculating
							if (w < h) {
								// for chinese devices, extra check
								val temp = w
								w = h
								h = temp

								if (w / h > 2) {
									h = (h * 1.5).toInt()
								}

								val temp2 = w
								w = h
								h = temp2

							} else {
								// for common cases
								val temp = w.toFloat() / h.toFloat()
								if (temp >= 2.0f) {
									h = (h * 1.5).toInt()
								}
							}

							outputFormatChanged = true
							actionChangeWidthHeight.invoke(w, h)
							logSdk(
								TAG,
								"Decoder format changed: ${decoder?.outputFormat}"
							)
						}

						MediaCodec.INFO_TRY_AGAIN_LATER -> {
							val outputFormat = decoder!!.outputFormat
							var w = outputFormat.getInteger(MediaFormat.KEY_WIDTH)
							var h = outputFormat.getInteger(MediaFormat.KEY_HEIGHT)

							delay(100)

							tryCount++

							logSdk(
								TAG, "No output from video decoder available ***, w = $w, h = $h, tryCount = $tryCount"
							)

							if (tryCount == 1 && !outputFormatChanged && w == 1920 && h == 1080 && !delayCheckStarted) {
								delayCheckStarted = true
								outputFormatChanged = false
								logSdk(TAG, "CASE 1")
								coroutineScope.launch {
									logSdk(TAG, "CASE 2")
									delay(5000)  // Отложенная проверка через 4 секунды
									if (tryCount == 1 && !outputFormatChanged && w == 1920 && h == 1080) {
										videoDecoderCallback.finishDecoderIncompatibleFormat()
									}
								}
							}
						}

						else -> {
							outputFormatChanged = true
							tryCount = 0  // reset the attempt counter
							if (outIndex >= 0) {
								decoder!!.releaseOutputBuffer(
									outIndex,
									bufferInfo.size != 0 && !exitFlag.get()
								)
								withContext(Dispatchers.Main) {
									actionPlaying.invoke((currentTimestamp - previousTimestamp) / 1000)
									if (!isTimelineChanged) {
										actionChangeTimeline.invoke(currentTimestamp)   // only once
										isTimelineChanged = true
									}
								}
								previousTimestamp = currentTimestamp
							}
						}
					}

					// All decoded frames have been rendered, we can stop playing now
					if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
						logSdk(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
						break
					}
				}

				// Drain decoder
				val inIndex: Int = decoder!!.dequeueInputBuffer(DEQUEUE_INPUT_TIMEOUT_US)
				if (inIndex >= 0) {
					decoder!!.queueInputBuffer(
						inIndex,
						0,
						0,
						0L,
						MediaCodec.BUFFER_FLAG_END_OF_STREAM
					)
				} else {
					Log.w(TAG, "Not able to signal end of stream")
				}

				decoder!!.stop()
				decoder!!.release()
				decoder = null
				videoFrameQueue.clear()
			} catch (e: Exception) {
				logSdk(TAG, e.message ?: "${e.printStackTrace()}")
			}
		}
	}

}

