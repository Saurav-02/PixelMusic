package com.saurav.pixelmusic.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

/**
 * Caps ultra-high-rate PCM to a rate Android devices handle more reliably.
 *
 * Media3/AudioTrack can end up in unstable device-specific paths for sample rates above 192 kHz.
 * Reducing 352.8/384 kHz streams before they reach the sink avoids the "loading audio" hang while
 * preserving normal playback for standard and regular hi-res files.
 */
@UnstableApi
class HiResSampleRateCapAudioProcessor(
    private val maxOutputSampleRateHz: Int = 192_000
) : BaseAudioProcessor() {

    private companion object {
        private val NATIVE_ORDER = ByteOrder.nativeOrder()
        private val NATIVE_ORDER_IS_BIG_ENDIAN = NATIVE_ORDER == ByteOrder.BIG_ENDIAN
    }

    private var downsampleFactor: Int = 1
    private var pendingBytes = ByteArray(0)
    private var pendingSize = 0
    private var shortAccumulators = IntArray(0)
    private var floatAccumulators = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val shouldDownsample = (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) &&
            inputAudioFormat.channelCount > 0 &&
            inputAudioFormat.sampleRate > maxOutputSampleRateHz

        if (!shouldDownsample) {
            return AudioFormat.NOT_SET
        }

        val factor = ceil(
            inputAudioFormat.sampleRate.toDouble() / maxOutputSampleRateHz.toDouble()
        ).toInt().coerceAtLeast(2)

        return AudioFormat(
            inputAudioFormat.sampleRate / factor,
            inputAudioFormat.channelCount,
            inputAudioFormat.encoding
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        if (inputAudioFormat == AudioFormat.NOT_SET || downsampleFactor <= 1) {
            val out = replaceOutputBuffer(inputBuffer.remaining())
            out.put(inputBuffer)
            out.flip()
            return
        }

        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            processFloat(inputBuffer)
        } else {
            process16Bit(inputBuffer)
        }
        inputBuffer.position(inputBuffer.limit())
    }

    private fun process16Bit(inputBuffer: ByteBuffer) {
        val bytesPerFrame = inputAudioFormat.channelCount * Short.SIZE_BYTES
        val processableFrameCount = processableFrameCount(bytesPerFrame, inputBuffer.remaining())
        if (processableFrameCount == 0) {
            stashPendingBytes(inputBuffer)
            return
        }

        val out = replaceOutputBuffer(processableFrameCount * bytesPerFrame)
        val shortInput = inputBuffer.duplicate().order(NATIVE_ORDER)
        var pendingReadOffset = 0

        repeat(processableFrameCount) {
            java.util.Arrays.fill(shortAccumulators, 0)
            repeat(downsampleFactor) {
                if (pendingReadOffset < pendingSize) {
                    for (ch in 0 until inputAudioFormat.channelCount) {
                        shortAccumulators[ch] += readShortFromPending(pendingReadOffset).toInt()
                        pendingReadOffset += Short.SIZE_BYTES
                    }
                } else {
                    for (ch in 0 until inputAudioFormat.channelCount) {
                        shortAccumulators[ch] += shortInput.short.toInt()
                    }
                }
            }
            for (ch in 0 until inputAudioFormat.channelCount) {
                out.putShort(
                    (shortAccumulators[ch] / downsampleFactor)
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                )
            }
        }
        storePendingRemainder(pendingReadOffset, shortInput)
        out.flip()
    }

    private fun processFloat(inputBuffer: ByteBuffer) {
        val bytesPerSample = Float.SIZE_BYTES
        val bytesPerFrame = inputAudioFormat.channelCount * bytesPerSample
        val processableFrameCount = processableFrameCount(bytesPerFrame, inputBuffer.remaining())
        if (processableFrameCount == 0) {
            stashPendingBytes(inputBuffer)
            return
        }

        val out = replaceOutputBuffer(processableFrameCount * bytesPerFrame)
        val floatInput = inputBuffer.duplicate().order(NATIVE_ORDER)
        var pendingReadOffset = 0

        repeat(processableFrameCount) {
            java.util.Arrays.fill(floatAccumulators, 0f)
            repeat(downsampleFactor) {
                if (pendingReadOffset < pendingSize) {
                    for (ch in 0 until inputAudioFormat.channelCount) {
                        floatAccumulators[ch] += readFloatFromPending(pendingReadOffset)
                        pendingReadOffset += Float.SIZE_BYTES
                    }
                } else {
                    for (ch in 0 until inputAudioFormat.channelCount) {
                        floatAccumulators[ch] += floatInput.float
                    }
                }
            }
            for (ch in 0 until inputAudioFormat.channelCount) {
                out.putFloat((floatAccumulators[ch] / downsampleFactor).coerceIn(-1f, 1f))
            }
        }
        storePendingRemainder(pendingReadOffset, floatInput)
        out.flip()
    }

    private fun processableFrameCount(bytesPerFrame: Int, incomingBytes: Int): Int {
        val totalFrames = (pendingSize + incomingBytes) / bytesPerFrame
        return totalFrames / downsampleFactor
    }

    private fun maxPendingBytes(): Int {
        val sampleBytes = if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) Float.SIZE_BYTES else Short.SIZE_BYTES
        return inputAudioFormat.channelCount * sampleBytes * downsampleFactor
    }

    private fun ensurePendingCapacity(requiredCapacity: Int) {
        if (pendingBytes.size >= requiredCapacity) return
        pendingBytes = pendingBytes.copyOf(requiredCapacity.coerceAtLeast(pendingBytes.size * 2).coerceAtLeast(1))
    }

    private fun ensureAccumulatorCapacity(channelCount: Int) {
        if (shortAccumulators.size < channelCount) {
            shortAccumulators = IntArray(channelCount)
        }
        if (floatAccumulators.size < channelCount) {
            floatAccumulators = FloatArray(channelCount)
        }
    }

    private fun stashPendingBytes(inputBuffer: ByteBuffer) {
        ensurePendingCapacity(pendingSize + inputBuffer.remaining())
        val source = inputBuffer.duplicate()
        source.get(pendingBytes, pendingSize, source.remaining())
        pendingSize += inputBuffer.remaining()
    }

    private fun storePendingRemainder(pendingReadOffset: Int, inputBuffer: ByteBuffer) {
        val unreadPendingBytes = pendingSize - pendingReadOffset
        val remainingInputBytes = inputBuffer.remaining()
        ensurePendingCapacity(unreadPendingBytes + remainingInputBytes)
        if (unreadPendingBytes > 0 && pendingReadOffset > 0) {
            pendingBytes.copyInto(
                destination = pendingBytes,
                destinationOffset = 0,
                startIndex = pendingReadOffset,
                endIndex = pendingSize
            )
        }
        if (remainingInputBytes > 0) {
            inputBuffer.get(pendingBytes, unreadPendingBytes, remainingInputBytes)
        }
        pendingSize = unreadPendingBytes + remainingInputBytes
    }

    private fun readShortFromPending(offset: Int): Short {
        val byte0 = pendingBytes[offset].toInt() and 0xFF
        val byte1 = pendingBytes[offset + 1].toInt() and 0xFF
        val bits = if (NATIVE_ORDER_IS_BIG_ENDIAN) {
            (byte0 shl 8) or byte1
        } else {
            (byte1 shl 8) or byte0
        }
        return bits.toShort()
    }

    private fun readFloatFromPending(offset: Int): Float {
        val intBits = readIntFromPending(offset)
        return Float.fromBits(intBits)
    }

    private fun readIntFromPending(offset: Int): Int {
        val byte0 = pendingBytes[offset].toInt() and 0xFF
        val byte1 = pendingBytes[offset + 1].toInt() and 0xFF
        val byte2 = pendingBytes[offset + 2].toInt() and 0xFF
        val byte3 = pendingBytes[offset + 3].toInt() and 0xFF
        return if (NATIVE_ORDER_IS_BIG_ENDIAN) {
            (byte0 shl 24) or (byte1 shl 16) or (byte2 shl 8) or byte3
        } else {
            (byte3 shl 24) or (byte2 shl 16) or (byte1 shl 8) or byte0
        }
    }

    override fun onQueueEndOfStream() {
        pendingSize = 0
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        pendingSize = 0
        if (inputAudioFormat != AudioFormat.NOT_SET && inputAudioFormat.sampleRate > maxOutputSampleRateHz) {
            downsampleFactor = ceil(
                inputAudioFormat.sampleRate.toDouble() / maxOutputSampleRateHz.toDouble()
            ).toInt().coerceAtLeast(2)
            ensurePendingCapacity(maxPendingBytes())
            ensureAccumulatorCapacity(inputAudioFormat.channelCount)
        } else {
            downsampleFactor = 1
        }
    }

    override fun onReset() {
        pendingSize = 0
        downsampleFactor = 1
    }
}
