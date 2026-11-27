package com.saurav.pixelmusic.audiolab.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.audiolab.AudioFxService
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class EightDParams(
    val enabled: Boolean = false,
    val rotationSpeedHz: Float = 0.15f,
    val depth: Float = 0.85f,
    val reverbLevel: Float = 0.4f,
)

// Pans the song slowly around the head; the 8D card in Audio FX sets speed, depth, and room reverb
@UnstableApi
class EightDAudioProcessor(private val audioFx: AudioFxService) : BaseAudioProcessor() {
    private var phase = 0.0
    private var smoothedEnabled = 0f
    private var smoothedDepth = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        val byteCount = input.remaining()
        if (byteCount == 0) return
        val rate = inputAudioFormat.sampleRate
        if (rate <= 0) {
            val output = replaceOutputBuffer(byteCount)
            output.put(input)
            output.flip()
            return
        }

        val settings = audioFx.eightD
        val targetEnabled = if (settings.enabled) 1f else 0f
        if (targetEnabled == 0f && smoothedEnabled == 0f) {
            val output = replaceOutputBuffer(byteCount)
            output.put(input)
            output.flip()
            return
        }

        val targetDepth = settings.depth.coerceIn(0f, 1f)
        val speed = settings.rotationSpeedHz.coerceIn(0.05f, 0.5f).toDouble()
        val smoothingStep = 1f / (rate * SmoothingSeconds).coerceAtLeast(1f)
        val output = replaceOutputBuffer(byteCount).order(ByteOrder.nativeOrder())
        val frameCount = byteCount / StereoFrameBytes
        val phaseStep = TwoPi * speed / rate

        repeat(frameCount) {
            val left = input.short.toInt()
            val right = input.short.toInt()
            smoothedEnabled = approach(smoothedEnabled, targetEnabled, smoothingStep)
            smoothedDepth = approach(smoothedDepth, targetDepth, smoothingStep)

            val pan = sin(phase) * smoothedDepth
            val theta = (pan + 1.0) * PI / 4.0
            val gainLeft = cos(theta)
            val gainRight = sin(theta)
            val processedLeft = left + (left * gainLeft - left) * smoothedEnabled
            val processedRight = right + (right * gainRight - right) * smoothedEnabled

            output.putShort(
                processedLeft.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort(),
            )
            output.putShort(
                processedRight.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort(),
            )
            phase = (phase + phaseStep) % TwoPi
        }
        output.put(input)
        output.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        phase = 0.0
        val settings = audioFx.eightD
        smoothedEnabled = if (settings.enabled) 1f else 0f
        smoothedDepth = settings.depth.coerceIn(0f, 1f)
    }

    override fun onReset() {
        phase = 0.0
        smoothedEnabled = 0f
        smoothedDepth = 0f
    }

    private fun approach(current: Float, target: Float, step: Float): Float = when {
        current < target -> min(target, current + step)
        current > target -> max(target, current - step)
        else -> current
    }

    private companion object {
        const val StereoFrameBytes = 4
        const val SmoothingSeconds = 0.01f
        const val TwoPi = 2.0 * PI
    }
}
