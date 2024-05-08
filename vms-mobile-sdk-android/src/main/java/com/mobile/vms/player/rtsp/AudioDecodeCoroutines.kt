package com.mobile.vms.player.rtsp

import android.media.*
import com.mobile.vms.player.helpers.logSdk
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.*

class AudioDecodeCoroutines(
	private val mimeType: String,
	private val sampleRate: Int,
	private val channelCount: Int,
	private val codecConfig: ByteArray?,
	private val audioFrameQueue: FrameQueue
) {

	private var exitFlag: AtomicBoolean = AtomicBoolean(false)
	val coroutineScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }
	val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
		throwable.printStackTrace()
	}

	fun stopAsync() {
		exitFlag.set(true)
	}

	fun run() {
		coroutineScope.launch {
			logSdk(TAG, "Audio decoder was started")

			val decoder = MediaCodec.createDecoderByType(mimeType)
			val format = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount)

			val csd0 = codecConfig ?: getAacDecoderConfigData(
				MediaCodecInfo.CodecProfileLevel.AACObjectLC,
				sampleRate,
				channelCount
			)
			format.setByteBuffer("csd-0", ByteBuffer.wrap(csd0))
			format.setInteger(
				MediaFormat.KEY_AAC_PROFILE,
				MediaCodecInfo.CodecProfileLevel.AACObjectLC
			)

			decoder.configure(format, null, null, 0)
			decoder.start()

			// Creating audio playback device
			val outChannel =
				if (channelCount > 1) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
			val outAudio = AudioFormat.ENCODING_PCM_16BIT
			val bufferSize = AudioTrack.getMinBufferSize(sampleRate, outChannel, outAudio)
			logSdk(
				TAG,
				"sampleRate: $sampleRate, bufferSize: $bufferSize".format(sampleRate, bufferSize)
			)
			if (bufferSize == 0) return@launch
			var audioTrack: AudioTrack
			try {
				audioTrack = AudioTrack(
					AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
						.build(),
					AudioFormat.Builder()
						.setEncoding(outAudio)
						.setChannelMask(outChannel)
						.setSampleRate(sampleRate)
						.build(),
					bufferSize,
					AudioTrack.MODE_STREAM,
					0
				)
				audioTrack.play()
			} catch (e: Exception) {
				e.printStackTrace()
				return@launch
			}

			val timeOut = 10000L

			val bufferInfo = MediaCodec.BufferInfo()
			while (!exitFlag.get()) {
				val inIndex: Int = decoder.dequeueInputBuffer(timeOut)
				if (inIndex >= 0) {
					// fill inputBuffers[inputBufferIndex] with valid data
					var byteBuffer: ByteBuffer?
					try {
						byteBuffer = decoder.getInputBuffer(inIndex)
					} catch (e: Exception) {
						e.printStackTrace()
						break
					}
					byteBuffer?.rewind()

					val audioFrame: FrameQueue.Frame?
					try {
						audioFrame = audioFrameQueue.pop()
						if (audioFrame == null) {
							logSdk(TAG, "Empty audio frame")
							// Release input buffer
							decoder.queueInputBuffer(inIndex, 0, 0, 0L, 0)
						} else {
							byteBuffer?.put(audioFrame.data, audioFrame.offset, audioFrame.length)
							decoder.queueInputBuffer(
								inIndex,
								audioFrame.offset,
								audioFrame.length,
								audioFrame.timestamp,
								0
							)
						}
					} catch (e: Exception) {
						logSdk(TAG, "Exception audio decoder \n ${e.printStackTrace()}")
						e.printStackTrace()
					}
				}
				if (exitFlag.get() || !isActive) break
				try {
					when (val outIndex = decoder.dequeueOutputBuffer(bufferInfo, timeOut)) {
						MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> logSdk(
							TAG,
							"Decoder format changed: ${decoder.outputFormat}"
						)

						MediaCodec.INFO_TRY_AGAIN_LATER -> logSdk(
							TAG,
							"No output from audio decoder available"
						)

						else -> {
							if (outIndex >= 0) {
								val byteBuffer: ByteBuffer? = decoder.getOutputBuffer(outIndex)

								val chunk = ByteArray(bufferInfo.size)
								byteBuffer?.get(chunk)
								byteBuffer?.clear()

								if (chunk.isNotEmpty()) {
									audioTrack.write(chunk, 0, chunk.size)
								}
								decoder.releaseOutputBuffer(outIndex, false)

							}
						}
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}

				// All decoded frames have been rendered, we can stop playing now
				if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
//					logSdk(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
					break
				}
			}

			logSdk(TAG, "*** audioTrack.flush()")
			audioTrack.flush()
			audioTrack.stop()
			audioTrack.release()

			try {
				decoder.stop()
				decoder.release()
			} catch (e: InterruptedException) {
				logSdk(TAG, e.message ?: "${e.printStackTrace()}")
			} catch (e: Exception) {
				logSdk(TAG, e.message ?: "${e.printStackTrace()}")
				e.printStackTrace()
			}
			audioFrameQueue.clear()
			logSdk(TAG, "Audio thread was stopped")
		}
	}

	companion object {
		private val TAG: String = AudioDecodeCoroutines::class.java.simpleName
		private const val DEBUG = false

		fun getAacDecoderConfigData(audioProfile: Int, sampleRate: Int, channels: Int): ByteArray {
			// AOT_LC = 2
			// 0001 0000 0000 0000
			var extraDataAac = audioProfile shl 11
			// Sample rate
			when (sampleRate) {
				7350 -> extraDataAac = extraDataAac or (0xC shl 7)
				8000 -> extraDataAac = extraDataAac or (0xB shl 7)
				11025 -> extraDataAac = extraDataAac or (0xA shl 7)
				12000 -> extraDataAac = extraDataAac or (0x9 shl 7)
				16000 -> extraDataAac = extraDataAac or (0x8 shl 7)
				22050 -> extraDataAac = extraDataAac or (0x7 shl 7)
				24000 -> extraDataAac = extraDataAac or (0x6 shl 7)
				32000 -> extraDataAac = extraDataAac or (0x5 shl 7)
				44100 -> extraDataAac = extraDataAac or (0x4 shl 7)
				48000 -> extraDataAac = extraDataAac or (0x3 shl 7)
				64000 -> extraDataAac = extraDataAac or (0x2 shl 7)
				88200 -> extraDataAac = extraDataAac or (0x1 shl 7)
				96000 -> extraDataAac = extraDataAac or (0x0 shl 7)
			}
			// Channels
			extraDataAac = extraDataAac or (channels shl 3)
			val extraData = ByteArray(2)
			extraData[0] = (extraDataAac and 0xff00 shr 8).toByte() // high byte
			extraData[1] = (extraDataAac and 0xff).toByte()         // low byte
			return extraData
		}
	}

}
