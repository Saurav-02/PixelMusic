package com.saurav.pixelmusic.audiolab.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.audiolab.AudioFxService
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

data class LoudnessParams(
    val enabled: Boolean = false,
    val boostDb: Float = 4f,
    val compression: Float = 0.3f,
)

@UnstableApi
class LoudnessAudioProcessor(private val service: AudioFxService) : BaseAudioProcessor() {

    private var sampleRate = 0
    private var channels = 0

    private var amount = 0f
    private var envelope = 0f
    private var gain = 1f
    private var limiterGain = 1f
    private var attack = 0f
    private var release = 0f
    private var limiterRelease = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        if (channels <= 0 || sampleRate <= 0) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        val params = service.loudness
        val target = if (params.enabled) 1f else 0f
        val out = replaceOutputBuffer(bytes)
        if (target == 0f && amount == 0f) {
            service.loudnessReductionDb = 0f
            out.put(input)
            out.flip()
            return
        }
        val frames = bytes / (2 * channels)
        if (frames == 0) {
            out.put(input)
            out.flip()
            return
        }

        val compression = params.compression.coerceIn(0f, 1f)
        val threshold = -10f - 24f * compression
        val slope = 1f - 1f / (1f + 3f * compression)
        val makeup = -threshold * slope * 0.5f * (if (compression > 0f) 1f else 0f)
        val boost = params.boostDb.coerceIn(0f, 12f)
        val toggleStep = 1f / (sampleRate * ToggleSeconds)

        val scale = 1f / 32768f
        var deepest = 1f
        var mostReduction = 0f
        var f = 0
        while (f < frames) {
            val block = minOf(Block, frames - f)
            if (!envelope.isFinite() || envelope < 0f) envelope = 0f
            if (!gain.isFinite() || gain <= 0f) gain = 1f
            val levelDb = 20f * log10(max(envelope, 1e-6f))
            val reduction = if (compression > 0f && levelDb > threshold) (threshold - levelDb) * slope else 0f
            if (reduction < mostReduction) mostReduction = reduction
            val targetGain = 10f.pow((boost + makeup + reduction) / 20f)
            val gainStep = (targetGain - gain) / block
            for (b in 0 until block) {
                val inL = input.getShort() * scale
                val inR = if (channels == 2) input.getShort() * scale else inL
                val peakIn = max(abs(inL), abs(inR))
                envelope = if (peakIn > envelope) envelope + (peakIn - envelope) * attack
                else envelope + (peakIn - envelope) * release
                gain += gainStep
                amount = if (target > amount) minOf(target, amount + toggleStep) else maxOf(target, amount - toggleStep)

                var l = inL * gain
                var r = inR * gain
                val peakOut = max(abs(l), abs(r)) * limiterGain
                limiterGain = if (peakOut > Ceiling) limiterGain * Ceiling / peakOut
                else limiterGain + (1f - limiterGain) * limiterRelease
                if (!limiterGain.isFinite() || limiterGain <= 0f) limiterGain = 1f
                if (limiterGain < deepest) deepest = limiterGain
                l *= limiterGain
                r *= limiterGain

                out.putShort(toPcm(inL + (l - inL) * amount))
                if (channels == 2) out.putShort(toPcm(inR + (r - inR) * amount))
            }
            f += block
        }
        service.loudnessReductionDb = (mostReduction + 20f * log10(max(deepest, 1e-6f))) * amount
        out.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        val format = inputAudioFormat
        if (format != AudioProcessor.AudioFormat.NOT_SET &&
            (format.sampleRate != sampleRate || format.channelCount != channels)
        ) {
            sampleRate = format.sampleRate
            channels = format.channelCount
            attack = 1f - exp(-1f / (0.008f * sampleRate))
            release = 1f - exp(-1f / (0.18f * sampleRate))
            limiterRelease = 1f - exp(-1f / (0.08f * sampleRate))
        }
        val params = service.loudness
        amount = if (params.enabled) 1f else 0f
        envelope = 0f
        limiterGain = 1f
        gain = 10f.pow(params.boostDb.coerceIn(0f, 12f) / 20f)
    }

    override fun onReset() {
        sampleRate = 0
        channels = 0
        amount = 0f
        envelope = 0f
        limiterGain = 1f
        gain = 1f
    }

    companion object {
        private const val Block = 32
        private const val ToggleSeconds = 0.15f
        private const val Ceiling = 0.944f
        private const val Knee = 0.95f

        private fun toPcm(sample: Float): Short {
            if (!sample.isFinite()) return 0
            val a = abs(sample)
            val shaped = if (a <= Knee) sample else {
                (Knee + (1f - Knee) * tanh((a - Knee) / (1f - Knee))) * if (sample < 0) -1f else 1f
            }
            return (shaped * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
