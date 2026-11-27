package com.saurav.pixelmusic.audiolab.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.audiolab.AudioFxService
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

enum class SpatialMode { Crossfeed, Speakers }

data class SpatialParams(
    val enabled: Boolean = false,
    val mode: SpatialMode = SpatialMode.Crossfeed,
    val cutoffHz: Float = 700f,
    val feedDb: Float = 4.5f,
    val speakerAngle: Float = 30f,
    val amount: Float = 1f,
    val surround: Boolean = false,
    val mono: Boolean = false,
)

@UnstableApi
class SpatialAudioProcessor(private val service: AudioFxService) : BaseAudioProcessor() {

    private var sampleRate = 0
    private var inChannels = 0
    private var surround = false

    private var amount = 0f
    private var monoAmount = 0f
    private var frame = FloatArray(0)
    private var crossfeed: Crossfeed? = null
    private var head: HeadRenderer? = null
    private var headAngle = Float.NaN

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val params = service.spatial
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return when (inputAudioFormat.channelCount) {
            2 -> inputAudioFormat
            in 3..8 -> if (params.surround) {
                AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
            } else {
                AudioProcessor.AudioFormat.NOT_SET
            }
            else -> AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        if (inChannels <= 0 || sampleRate <= 0) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        val frames = bytes / (2 * inChannels)
        if (frames == 0) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        val params = service.spatial
        val scale = 1f / 32768f

        if (surround) {
            val renderer = head ?: HeadRenderer(sampleRate, surroundAzimuths(inChannels)).also { head = it }
            val gain = surroundGain(inChannels)
            val mono = params.mono
            val out = replaceOutputBuffer(frames * 4)
            if (frame.size < inChannels) frame = FloatArray(inChannels)
            for (f in 0 until frames) {
                for (c in 0 until inChannels) frame[c] = input.getShort() * scale
                renderer.render(frame)
                var l = renderer.left * gain
                var r = renderer.right * gain
                if (mono) {
                    val m = (l + r) * 0.5f
                    l = m
                    r = m
                }
                out.putShort(toPcm(l))
                out.putShort(toPcm(r))
            }
            out.flip()
            return
        }

        val target = if (params.enabled && !service.stage.enabled) params.amount.coerceIn(0f, 1f) else 0f
        val monoTarget = if (params.mono) 1f else 0f
        val out = replaceOutputBuffer(bytes)
        if (target == 0f && amount == 0f && monoTarget == 0f && monoAmount == 0f) {
            out.put(input)
            out.flip()
            return
        }
        val step = 1f / (sampleRate * ToggleSeconds)
        when (params.mode) {
            SpatialMode.Crossfeed -> {
                val cf = crossfeed?.takeIf { it.matches(params.cutoffHz, params.feedDb) }
                    ?: Crossfeed(sampleRate, params.cutoffHz, params.feedDb, crossfeed).also { crossfeed = it }
                for (f in 0 until frames) {
                    var l = input.getShort() * scale
                    var r = input.getShort() * scale
                    monoAmount = glide(monoAmount, monoTarget, step)
                    if (monoAmount > 0f) {
                        val m = (l + r) * 0.5f
                        l += (m - l) * monoAmount
                        r += (m - r) * monoAmount
                    }
                    amount = glide(amount, target, step)
                    cf.process(l, r)
                    out.putShort(toPcm(l + (cf.left - l) * amount))
                    out.putShort(toPcm(r + (cf.right - r) * amount))
                }
            }
            SpatialMode.Speakers -> {
                val angle = params.speakerAngle.coerceIn(5f, 90f)
                val renderer = head?.takeIf { headAngle == angle }
                    ?: HeadRenderer(sampleRate, floatArrayOf(-angle, angle)).also { head = it; headAngle = angle }
                if (frame.size < 2) frame = FloatArray(2)
                for (f in 0 until frames) {
                    var l = input.getShort() * scale
                    var r = input.getShort() * scale
                    monoAmount = glide(monoAmount, monoTarget, step)
                    if (monoAmount > 0f) {
                        val m = (l + r) * 0.5f
                        l += (m - l) * monoAmount
                        r += (m - r) * monoAmount
                    }
                    amount = glide(amount, target, step)
                    frame[0] = l
                    frame[1] = r
                    renderer.render(frame)
                    out.putShort(toPcm(l + (renderer.left * SpeakerGain - l) * amount))
                    out.putShort(toPcm(r + (renderer.right * SpeakerGain - r) * amount))
                }
            }
        }
        out.flip()
    }

    private fun glide(value: Float, target: Float, step: Float): Float =
        if (target > value) minOf(target, value + step) else maxOf(target, value - step)

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        val format = inputAudioFormat
        val nextSurround = format.channelCount in 3..8 && outputAudioFormat.channelCount == 2
        if (format != AudioProcessor.AudioFormat.NOT_SET &&
            (format.sampleRate != sampleRate || format.channelCount != inChannels || nextSurround != surround)
        ) {
            sampleRate = format.sampleRate
            inChannels = format.channelCount
            surround = nextSurround
            frame = FloatArray(inChannels)
            crossfeed = null
            head = null
            headAngle = Float.NaN
        }
        crossfeed?.clear()
        head?.clear()
        val params = service.spatial
        amount = if (params.enabled && !service.stage.enabled) params.amount.coerceIn(0f, 1f) else 0f
        monoAmount = if (params.mono) 1f else 0f
    }

    override fun onReset() {
        sampleRate = 0
        inChannels = 0
        surround = false
        crossfeed = null
        head = null
        headAngle = Float.NaN
    }

    private class Crossfeed(sampleRate: Int, private val cutoff: Float, private val feed: Float, previous: Crossfeed?) {
        private val a0Lo: Float
        private val b1Lo: Float
        private val a0Hi: Float
        private val a1Hi: Float
        private val b1Hi: Float
        private val gain: Float
        private var loL: Float = previous?.loL ?: 0f
        private var loR: Float = previous?.loR ?: 0f
        private var hiL: Float = previous?.hiL ?: 0f
        private var hiR: Float = previous?.hiR ?: 0f
        private var asisL: Float = previous?.asisL ?: 0f
        private var asisR: Float = previous?.asisR ?: 0f
        var left = 0f
            private set
        var right = 0f
            private set

        init {
            val fc = cutoff.coerceIn(300f, 2000f).toDouble()
            val fd = feed.coerceIn(1f, 15f).toDouble()
            val gbLo = fd * -5.0 / 6.0 - 3.0
            val gbHi = fd / 6.0 - 3.0
            val gLo = 10.0.pow(gbLo / 20.0)
            val gHi = 1.0 - 10.0.pow(gbHi / 20.0)
            val fcHi = fc * 2.0.pow((gbLo - 20.0 * log10(gHi)) / 12.0)
            val xLo = exp(-2.0 * PI * fc / sampleRate)
            b1Lo = xLo.toFloat()
            a0Lo = (gLo * (1.0 - xLo)).toFloat()
            val xHi = exp(-2.0 * PI * fcHi / sampleRate)
            b1Hi = xHi.toFloat()
            a0Hi = (1.0 - gHi * (1.0 - xHi)).toFloat()
            a1Hi = (-xHi).toFloat()
            gain = (1.0 / (1.0 - gHi + gLo)).toFloat()
        }

        fun matches(cutoff: Float, feed: Float) = this.cutoff == cutoff && this.feed == feed

        fun process(l: Float, r: Float) {
            loL = a0Lo * l + b1Lo * loL
            loR = a0Lo * r + b1Lo * loR
            hiL = a0Hi * l + a1Hi * asisL + b1Hi * hiL
            hiR = a0Hi * r + a1Hi * asisR + b1Hi * hiR
            asisL = l
            asisR = r
            left = (hiL + loR) * gain
            right = (hiR + loL) * gain
        }

        fun clear() {
            loL = 0f; loR = 0f; hiL = 0f; hiR = 0f; asisL = 0f; asisR = 0f
        }
    }

    private class HeadRenderer(sampleRate: Int, private val azimuths: FloatArray) {
        private val n = azimuths.size
        private val line = Array(n) { FloatArray(DelaySize) }
        private var write = 0
        private val delay = FloatArray(n * 2)
        private val b0 = FloatArray(n * 2)
        private val b1 = FloatArray(n * 2)
        private val a1 = FloatArray(n * 2)
        private val x1 = FloatArray(n * 2)
        private val y1 = FloatArray(n * 2)
        private val weight = FloatArray(n)
        var left = 0f
            private set
        var right = 0f
            private set

        init {
            val w0 = SpeedOfSound / HeadRadius
            val k = 2.0 * sampleRate
            val raw = DoubleArray(n * 2)
            for (s in 0 until n) {
                val az = azimuths[s]
                weight[s] = when {
                    az.isNaN() -> 0.5f
                    abs(az) < 1f -> 0.71f
                    else -> 1f
                }
                for (ear in 0..1) {
                    val i = s * 2 + ear
                    if (az.isNaN()) {
                        val wc = 2.0 * PI * 120.0
                        val normLfe = wc + k
                        b0[i] = (wc / normLfe).toFloat()
                        b1[i] = (wc / normLfe).toFloat()
                        a1[i] = ((wc - k) / normLfe).toFloat()
                        continue
                    }
                    val earAz = if (ear == 0) -90.0 else 90.0
                    val theta = Math.toRadians(abs(wrap(az - earAz)))
                    val alpha = (1 + AlphaMin / 2) + (1 - AlphaMin / 2) * cos(theta / ThetaMin * PI)
                    val norm = 2 * w0 + k
                    b0[i] = ((2 * w0 + alpha * k) / norm).toFloat()
                    b1[i] = ((2 * w0 - alpha * k) / norm).toFloat()
                    a1[i] = ((2 * w0 - k) / norm).toFloat()
                    raw[i] = if (theta < PI / 2) -HeadRadius / SpeedOfSound * cos(theta)
                    else HeadRadius / SpeedOfSound * (theta - PI / 2)
                }
            }
            val earliest = raw.minOrNull() ?: 0.0
            for (i in 0 until n * 2) {
                // Align LFE with the centre speaker instead of zero delay so deep bass stays in phase with front channels
                val target = if (azimuths[i / 2].isNaN()) 0.0 else raw[i]
                delay[i] = ((target - earliest) * sampleRate).toFloat().coerceIn(0f, (DelaySize - 2).toFloat())
            }
        }

        fun render(frame: FloatArray) {
            var l = 0f
            var r = 0f
            for (s in 0 until n) {
                line[s][write] = frame[s]
                for (ear in 0..1) {
                    val i = s * 2 + ear
                    val d = delay[i]
                    val whole = d.toInt()
                    val frac = d - whole
                    var p0 = write - whole
                    if (p0 < 0) p0 += DelaySize
                    val p1 = if (p0 == 0) DelaySize - 1 else p0 - 1
                    val x = line[s][p0] + (line[s][p1] - line[s][p0]) * frac
                    val y = b0[i] * x + b1[i] * x1[i] - a1[i] * y1[i]
                    x1[i] = x
                    y1[i] = y
                    if (ear == 0) l += y * weight[s] else r += y * weight[s]
                }
            }
            write = if (write + 1 == DelaySize) 0 else write + 1
            left = l
            right = r
        }

        fun clear() {
            line.forEach { it.fill(0f) }
            x1.fill(0f)
            y1.fill(0f)
        }

        private fun wrap(deg: Double): Double {
            var d = deg % 360.0
            if (d > 180) d -= 360
            if (d < -180) d += 360
            return d
        }

        companion object {
            private const val DelaySize = 256
            private const val HeadRadius = 0.0875
            private const val SpeedOfSound = 343.0
            private const val AlphaMin = 0.1
            private const val ThetaMin = 150.0 * PI / 180.0
        }
    }

    companion object {
        private const val ToggleSeconds = 0.15f
        private const val SpeakerGain = 0.6f
        private const val Knee = 0.9f

        private fun surroundGain(channels: Int): Float = when {
            channels <= 4 -> 0.70f
            channels <= 6 -> 0.55f
            else -> 0.48f
        }

        private fun surroundAzimuths(channels: Int): FloatArray = when (channels) {
            3 -> floatArrayOf(-30f, 30f, 0f)
            4 -> floatArrayOf(-30f, 30f, -110f, 110f)
            5 -> floatArrayOf(-30f, 30f, 0f, -110f, 110f)
            6 -> floatArrayOf(-30f, 30f, 0f, Float.NaN, -110f, 110f)
            7 -> floatArrayOf(-30f, 30f, 0f, Float.NaN, 180f, -90f, 90f)
            else -> floatArrayOf(-30f, 30f, 0f, Float.NaN, -150f, 150f, -90f, 90f)
        }

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
