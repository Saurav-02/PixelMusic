package com.saurav.pixelmusic.audiolab.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.audiolab.data.ParametricEQ
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

@UnstableApi
class CustomEqualizerAudioProcessor : BaseAudioProcessor() {

    private var sampleRate = 0
    private var channels = 0

    @Volatile private var design: ParametricEQ? = null
    @Volatile private var designVersion = 0
    private var builtVersion = -1

    private var bank: FilterBank? = null
    private var fadingFrom: FilterBank? = null
    private var fading = false
    private var fadePos = 0
    private var fadeLength = 0
    private var snapNextDesign = true

    private var work = DoubleArray(0)
    private var fadeWork = DoubleArray(0)
    private var pcm = ShortArray(0)
    private val limiter = PeakLimiter()

    private var tailPending = false
    private val tapId = System.identityHashCode(this)

    fun applyProfile(parametricEQ: ParametricEQ) {
        design = parametricEQ
        designVersion++
    }

    fun disable() {
        design = null
        designVersion++
    }

    fun isEnabled(): Boolean = design != null

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..8) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) {
            if (tailPending) drainTail()
            return
        }
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        if (channels <= 0 || sampleRate <= 0) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        syncDesign()
        if (bank == null && !fading && limiter.isResting && !AudioTap.wanted) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        val frames = bytes / (2 * channels)
        if (frames == 0) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        val count = frames * channels
        ensureWork(count)
        val scale = 1.0 / 32768.0
        input.asShortBuffer().get(pcm, 0, count)
        input.position(input.position() + count * 2)
        for (i in 0 until count) work[i] = pcm[i] * scale
        render(frames, replaceOutputBuffer(count * 2))
    }

    private fun render(frames: Int, out: ByteBuffer) {
        syncDesign()
        val count = frames * channels
        if (fading) {
            System.arraycopy(work, 0, fadeWork, 0, count)
            fadingFrom?.process(fadeWork, frames)
        }
        bank?.process(work, frames)
        if (fading) mixFade(frames)
        limiter.process(work, frames)

        if (AudioTap.wanted) AudioTap.write(tapId, work, frames, channels, sampleRate)

        for (i in 0 until count) pcm[i] = toPcm(work[i])
        out.asShortBuffer().put(pcm, 0, count)
        out.position(out.position() + count * 2)
        out.flip()
    }

    private fun drainTail() {
        tailPending = false
        if (channels <= 0 || sampleRate <= 0) return
        val frames = limiter.latencyFrames
        if (frames <= 0) return
        val count = frames * channels
        ensureWork(count)
        work.fill(0.0, 0, count)
        render(frames, replaceOutputBuffer(count * 2))
    }

    private fun ensureWork(count: Int) {
        if (work.size >= count) return
        work = DoubleArray(count)
        fadeWork = DoubleArray(count)
        pcm = ShortArray(count)
    }

    private fun mixFade(frames: Int) {
        var i = 0
        val length = fadeLength.toDouble()
        for (f in 0 until frames) {
            val g = if (fadePos >= fadeLength) 1.0 else fadePos / length
            for (c in 0 until channels) {
                work[i] = fadeWork[i] + (work[i] - fadeWork[i]) * g
                i++
            }
            if (fadePos < fadeLength) fadePos++
        }
        if (fadePos >= fadeLength) {
            fading = false
            fadingFrom = null
        }
    }

    private fun syncDesign() {
        val version = designVersion
        if (version == builtVersion) return
        builtVersion = version
        val next = design?.let { FilterBank.build(it, sampleRate, channels) }
        next?.adoptState(bank)
        if (snapNextDesign) {
            snapNextDesign = false
            bank = next
            fading = false
            fadingFrom = null
            return
        }
        if (bank == null && next == null) return
        fadingFrom = bank
        fading = true
        fadePos = 0
        fadeLength = (sampleRate * FadeSeconds).toInt().coerceAtLeast(1)
        bank = next
    }

    override fun onQueueEndOfStream() {
        if (channels > 0 && (bank != null || fading || !limiter.isResting)) {
            if (!hasPendingOutput()) {
                drainTail()
            } else {
                tailPending = true
            }
        } else {
            tailPending = false
        }
    }

    override fun getOutput(): ByteBuffer {
        if (!hasPendingOutput() && tailPending) {
            drainTail()
        }
        return super.getOutput()
    }

    override fun isEnded(): Boolean = super.isEnded() && !tailPending

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        tailPending = false
        val format = inputAudioFormat
        if (format != AudioProcessor.AudioFormat.NOT_SET &&
            (format.sampleRate != sampleRate || format.channelCount != channels)
        ) {
            sampleRate = format.sampleRate
            channels = format.channelCount
            bank = null
            builtVersion = -1
            limiter.configure(sampleRate, channels)
        }
        bank?.reset()
        limiter.reset()
        fading = false
        fadingFrom = null
        if (bank == null) snapNextDesign = true
    }

    override fun onReset() {
        tailPending = false
        sampleRate = 0
        channels = 0
        bank = null
        builtVersion = -1
        fading = false
        fadingFrom = null
        snapNextDesign = true
    }

    private class FilterBank(
        private val count: Int,
        private val channels: Int,
        private val coefficients: DoubleArray,
        private val gain: Double,
    ) {
        private val state = DoubleArray(count * channels * 4)

        fun process(buffer: DoubleArray, frames: Int) {
            for (k in 0 until count) {
                val o = k * Biquad.Stride
                val b0 = coefficients[o]
                val b1 = coefficients[o + 1]
                val b2 = coefficients[o + 2]
                val a1 = coefficients[o + 3]
                val a2 = coefficients[o + 4]
                for (c in 0 until channels) {
                    val s = (k * channels + c) * 4
                    var x1 = state[s]
                    var x2 = state[s + 1]
                    var y1 = state[s + 2]
                    var y2 = state[s + 3]
                    var i = c
                    for (f in 0 until frames) {
                        val x = buffer[i]
                        var y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
                        if (!y.isFinite()) {
                            x1 = 0.0
                            x2 = 0.0
                            y1 = 0.0
                            y2 = 0.0
                            y = 0.0
                        } else {
                            x2 = x1
                            x1 = x
                            y2 = y1
                            y1 = y
                        }
                        buffer[i] = y
                        i += channels
                    }
                    state[s] = x1
                    state[s + 1] = x2
                    state[s + 2] = y1
                    state[s + 3] = y2
                }
            }
            if (gain != 1.0) {
                for (i in 0 until frames * channels) buffer[i] *= gain
            }
        }

        fun adoptState(previous: FilterBank?) {
            if (previous == null || previous.channels != channels) return
            val shared = minOf(count, previous.count) * channels * 4
            System.arraycopy(previous.state, 0, state, 0, shared)
        }

        fun reset() = state.fill(0.0)

        companion object {
            fun build(eq: ParametricEQ, sampleRate: Int, channels: Int): FilterBank? {
                if (sampleRate <= 0 || channels <= 0) return null
                val bands = eq.bands.filter { Biquad.isAudible(it) && it.frequency < sampleRate / 2.0 }
                val gain = 10.0.pow(eq.preamp / 20.0)
                if (bands.isEmpty() && gain == 1.0) return null
                val coefficients = DoubleArray(bands.size * Biquad.Stride)
                bands.forEachIndexed { index, band ->
                    Biquad.coefficients(band, sampleRate, coefficients, index * Biquad.Stride)
                }
                return FilterBank(bands.size, channels, coefficients, gain)
            }
        }
    }

    companion object {
        private const val FadeSeconds = 0.03
    }
}

internal fun toPcm(sample: Double): Short {
    if (!sample.isFinite()) return 0
    return Math.round(sample * 32768.0).coerceIn(-32768L, 32767L).toShort()
}
