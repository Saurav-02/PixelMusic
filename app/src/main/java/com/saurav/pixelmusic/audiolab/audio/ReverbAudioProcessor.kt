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
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.tanh

data class ReverbParams(
    val enabled: Boolean = false,
    val mix: Float = 0.5f,
    val roomSize: Float = 0.55f,
    val damping: Float = 0.45f,
    val wet: Float = 0.6f,
    val dry: Float = 1f,
    val width: Float = 1f,
    val preDelayMs: Float = 12f,
    val lowCutHz: Float = 180f,
)

@UnstableApi
class ReverbAudioProcessor(private val service: AudioFxService) : BaseAudioProcessor() {

    private var sampleRate = 0
    private var channels = 0

    private var combsL: Array<Comb> = emptyArray()
    private var combsR: Array<Comb> = emptyArray()
    private var allpassL: Array<Allpass> = emptyArray()
    private var allpassR: Array<Allpass> = emptyArray()
    private var preDelay = FloatArray(1)
    private var preDelayIndex = 0
    private var lowCutState = 0f
    private var lowCutInput = 0f

    private var amount = 0f
    private var wet1 = 0f
    private var wet2 = 0f
    private var dryGain = 1f
    private var mix = 0f
    private var roomEmpty = true

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    private fun effectiveParams(): ReverbParams {
        val reverb = service.reverb
        if (reverb.enabled) return reverb
        val eightD = service.eightD
        val level = eightD.reverbLevel.coerceIn(0f, 1f)
        if (eightD.enabled && level > 0f) {
            return ReverbParams(
                enabled = true,
                mix = (0.18f + 0.32f * level).coerceIn(0f, 1f),
                roomSize = (0.45f + 0.35f * level).coerceIn(0f, 1f),
                damping = 0.45f,
                wet = (0.35f + 0.35f * level).coerceIn(0f, 1f),
                dry = 1f,
                width = 1f,
                preDelayMs = 14f,
                lowCutHz = 180f,
            )
        }
        return reverb
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        if (channels <= 0 || sampleRate <= 0 || combsL.isEmpty()) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }
        val params = effectiveParams()
        val target = if (params.enabled) 1f else 0f
        val out = replaceOutputBuffer(bytes)
        if (target == 0f && amount == 0f) {
            if (!roomEmpty) clearRoom()
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
        roomEmpty = false

        val feedback = params.roomSize.coerceIn(0f, 1f) * ScaleRoom + OffsetRoom
        val damp = params.damping.coerceIn(0f, 1f) * ScaleDamp
        for (c in combsL) c.set(feedback, damp)
        for (c in combsR) c.set(feedback, damp)
        val delayFrames = (params.preDelayMs.coerceIn(0f, MaxPreDelayMs) / 1000f * sampleRate).roundToInt()
            .coerceIn(0, preDelay.size - 1)
        val lowCut = exp(-2.0 * PI * params.lowCutHz.coerceIn(10f, 2000f) / sampleRate).toFloat()

        val width = params.width.coerceIn(0f, 1f)
        val wetLevel = params.wet.coerceIn(0f, 1f) * ScaleWet
        val targetWet1 = wetLevel * (width / 2f + 0.5f)
        val targetWet2 = wetLevel * ((1f - width) / 2f)
        val targetDry = params.dry.coerceIn(0f, 1f)
        val targetMix = params.mix.coerceIn(0f, 1f)

        val step = 1f / frames
        val toggleStep = 1f / (sampleRate * ToggleSeconds)
        val dWet1 = (targetWet1 - wet1) * step
        val dWet2 = (targetWet2 - wet2) * step
        val dDry = (targetDry - dryGain) * step
        val dMix = (targetMix - mix) * step

        val scale = 1f / 32768f
        for (f in 0 until frames) {
            val inL = input.getShort() * scale
            val inR = if (channels == 2) input.getShort() * scale else inL

            amount = if (target > amount) minOf(target, amount + toggleStep) else maxOf(target, amount - toggleStep)
            wet1 += dWet1
            wet2 += dWet2
            dryGain += dDry
            mix += dMix

            val send = (inL + inR) * FixedGain
            preDelay[preDelayIndex] = send
            var read = preDelayIndex - delayFrames
            if (read < 0) read += preDelay.size
            preDelayIndex = if (preDelayIndex + 1 == preDelay.size) 0 else preDelayIndex + 1
            val delayed = preDelay[read]
            lowCutState = lowCut * (lowCutState + delayed - lowCutInput)
            if (!lowCutState.isFinite()) lowCutState = 0f
            lowCutInput = delayed
            val reverbInput = lowCutState + AntiDenormal

            var accL = 0f
            var accR = 0f
            for (c in combsL) accL += c.process(reverbInput)
            for (a in allpassL) accL = a.process(accL)
            if (channels == 2) {
                for (c in combsR) accR += c.process(reverbInput)
                for (a in allpassR) accR = a.process(accR)
            }

            val g = amount * mix
            if (channels == 2) {
                val roomL = accL * wet1 + accR * wet2 + inL * dryGain
                val roomR = accR * wet1 + accL * wet2 + inR * dryGain
                out.putShort(toPcm(inL + (roomL - inL) * g))
                out.putShort(toPcm(inR + (roomR - inR) * g))
            } else {
                val room = accL * (wet1 + wet2) + inL * dryGain
                out.putShort(toPcm(inL + (room - inL) * g))
            }
        }
        wet1 = targetWet1
        wet2 = targetWet2
        dryGain = targetDry
        mix = targetMix
        out.flip()
    }

    private fun clearRoom() {
        combsL.forEach { it.clear() }
        combsR.forEach { it.clear() }
        allpassL.forEach { it.clear() }
        allpassR.forEach { it.clear() }
        preDelay.fill(0f)
        lowCutState = 0f
        lowCutInput = 0f
        roomEmpty = true
    }

    private fun build(rate: Int) {
        val k = rate / 44_100f
        combsL = Array(CombTuning.size) { Comb((CombTuning[it] * k).roundToInt()) }
        combsR = Array(CombTuning.size) { Comb(((CombTuning[it] + StereoSpread) * k).roundToInt()) }
        allpassL = Array(AllpassTuning.size) { Allpass((AllpassTuning[it] * k).roundToInt()) }
        allpassR = Array(AllpassTuning.size) { Allpass(((AllpassTuning[it] + StereoSpread) * k).roundToInt()) }
        preDelay = FloatArray((rate * MaxPreDelayMs / 1000f).roundToInt() + 2)
        preDelayIndex = 0
        roomEmpty = false
        clearRoom()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        val format = inputAudioFormat
        if (format != AudioProcessor.AudioFormat.NOT_SET &&
            (format.sampleRate != sampleRate || format.channelCount != channels || combsL.isEmpty())
        ) {
            sampleRate = format.sampleRate
            channels = format.channelCount
            build(sampleRate)
        } else {
            clearRoom()
        }
        val params = effectiveParams()
        amount = if (params.enabled) 1f else 0f
        val width = params.width.coerceIn(0f, 1f)
        val wetLevel = params.wet.coerceIn(0f, 1f) * ScaleWet
        wet1 = wetLevel * (width / 2f + 0.5f)
        wet2 = wetLevel * ((1f - width) / 2f)
        dryGain = params.dry.coerceIn(0f, 1f)
        mix = params.mix.coerceIn(0f, 1f)
    }

    override fun onReset() {
        clearRoom()
        sampleRate = 0
        channels = 0
        combsL = emptyArray()
        combsR = emptyArray()
        allpassL = emptyArray()
        allpassR = emptyArray()
    }

    private class Comb(size: Int) {
        private val line = FloatArray(size.coerceAtLeast(1))
        private var index = 0
        private var store = 0f
        private var feedback = 0.84f
        private var damp1 = 0.2f
        private var damp2 = 0.8f

        fun set(feedback: Float, damp: Float) {
            this.feedback = feedback
            damp1 = damp
            damp2 = 1f - damp
        }

        fun process(input: Float): Float {
            val output = line[index]
            var nextStore = output * damp2 + store * damp1
            if (!nextStore.isFinite()) nextStore = 0f
            store = nextStore
            val written = input + store * feedback
            line[index] = if (written.isFinite()) written else 0f
            index = if (index + 1 == line.size) 0 else index + 1
            return if (output.isFinite()) output else 0f
        }

        fun clear() {
            line.fill(0f)
            store = 0f
        }
    }

    private class Allpass(size: Int) {
        private val line = FloatArray(size.coerceAtLeast(1))
        private var index = 0

        fun process(input: Float): Float {
            val buffered = line[index]
            val written = input + buffered * 0.5f
            line[index] = if (written.isFinite()) written else 0f
            index = if (index + 1 == line.size) 0 else index + 1
            val out = buffered - input
            return if (out.isFinite()) out else 0f
        }

        fun clear() = line.fill(0f)
    }

    companion object {
        private val CombTuning = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        private val AllpassTuning = intArrayOf(556, 441, 341, 225)
        private const val StereoSpread = 23
        private const val FixedGain = 0.015f
        private const val ScaleWet = 3f
        private const val ScaleDamp = 0.4f
        private const val ScaleRoom = 0.28f
        private const val OffsetRoom = 0.7f
        private const val MaxPreDelayMs = 150f
        private const val ToggleSeconds = 0.15f
        private const val AntiDenormal = 1e-18f
        private const val Knee = 0.89f

        private fun toPcm(sample: Float): Short {
            if (!sample.isFinite()) return 0
            val a = abs(sample)
            val shaped = if (a <= Knee) sample else {
                val over = (a - Knee) / (1f - Knee)
                (Knee + (1f - Knee) * tanh(over)) * if (sample < 0) -1f else 1f
            }
            val s = (shaped * 32767f).roundToInt()
            return s.coerceIn(-32768, 32767).toShort()
        }
    }
}
