package com.saurav.pixelmusic.data.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An [AudioProcessor] that downmixes 5.1 (6-channel) and 7.1 (8-channel) surround audio
 * to stereo (2-channel) PCM using the standard Dolby downmix matrix coefficients.
 */
@UnstableApi
class SurroundDownmixProcessor : BaseAudioProcessor() {

    companion object {
        private const val COEFF_SURROUND = 0.707f
        private const val COEFF_LFE = 0.707f
        private const val MAX_SUPPORTED_CHANNELS = 8

        private const val FL_51  = 0
        private const val FR_51  = 1
        private const val FC_51  = 2
        private const val LFE_51 = 3
        private const val SL_51  = 4
        private const val SR_51  = 5

        private const val FL_71  = 0
        private const val FR_71  = 1
        private const val FC_71  = 2
        private const val LFE_71 = 3
        private const val SL_71  = 4
        private const val SR_71  = 5
        private const val SBL_71 = 6
        private const val SBR_71 = 7
    }

    private val floatScratch = FloatArray(MAX_SUPPORTED_CHANNELS)
    private val shortScratch = ShortArray(MAX_SUPPORTED_CHANNELS)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val isSupported = (inputAudioFormat.channelCount == 6 || inputAudioFormat.channelCount == 8)
                && (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT)

        return if (isSupported) {
            AudioFormat(
                inputAudioFormat.sampleRate,
                /* channelCount = */ 2,
                inputAudioFormat.encoding
            )
        } else {
            AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val channelCount = inputAudioFormat.channelCount
        if (channelCount != 6 && channelCount != 8) {
            val out = replaceOutputBuffer(inputBuffer.remaining())
            out.put(inputBuffer)
            out.flip()
            return
        }

        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            val bytesPerFrame = channelCount * Float.SIZE_BYTES
            val frameCount = inputBuffer.remaining() / bytesPerFrame
            val out = replaceOutputBuffer(frameCount * 2 * Float.SIZE_BYTES)

            val floatInput = inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asFloatBuffer()
            repeat(frameCount) {
                floatInput.get(floatScratch, 0, channelCount)
                val left: Float
                val right: Float
                if (channelCount == 6) {
                    left = downmix51Left(floatScratch)
                    right = downmix51Right(floatScratch)
                } else {
                    left = downmix71Left(floatScratch)
                    right = downmix71Right(floatScratch)
                }
                out.putFloat(left.coerceIn(-1f, 1f))
                out.putFloat(right.coerceIn(-1f, 1f))
            }
            inputBuffer.position(inputBuffer.position() + frameCount * bytesPerFrame)
            out.flip()
        } else {
            val bytesPerFrame = channelCount * Short.SIZE_BYTES
            val frameCount = inputBuffer.remaining() / bytesPerFrame
            val out = replaceOutputBuffer(frameCount * 2 * Short.SIZE_BYTES)

            val shortInput = inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
            repeat(frameCount) {
                shortInput.get(shortScratch, 0, channelCount)
                val left: Float
                val right: Float
                if (channelCount == 6) {
                    left = downmix51Left(shortScratch)
                    right = downmix51Right(shortScratch)
                } else {
                    left = downmix71Left(shortScratch)
                    right = downmix71Right(shortScratch)
                }
                out.putShort(left.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                out.putShort(right.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
            inputBuffer.position(inputBuffer.position() + frameCount * bytesPerFrame)
            out.flip()
        }
    }

    private fun downmix51Left(s: ShortArray): Float =
        s[FL_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SL_51] + COEFF_LFE * s[LFE_51]

    private fun downmix51Right(s: ShortArray): Float =
        s[FR_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SR_51] + COEFF_LFE * s[LFE_51]

    private fun downmix71Left(s: ShortArray): Float =
        s[FL_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SL_71] + COEFF_SURROUND * s[SBL_71] + COEFF_LFE * s[LFE_71]

    private fun downmix71Right(s: ShortArray): Float =
        s[FR_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SR_71] + COEFF_SURROUND * s[SBR_71] + COEFF_LFE * s[LFE_71]

    private fun downmix51Left(s: FloatArray): Float =
        s[FL_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SL_51] + COEFF_LFE * s[LFE_51]

    private fun downmix51Right(s: FloatArray): Float =
        s[FR_51] + COEFF_SURROUND * s[FC_51] + COEFF_SURROUND * s[SR_51] + COEFF_LFE * s[LFE_51]

    private fun downmix71Left(s: FloatArray): Float =
        s[FL_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SL_71] + COEFF_SURROUND * s[SBL_71] + COEFF_LFE * s[LFE_71]

    private fun downmix71Right(s: FloatArray): Float =
        s[FR_71] + COEFF_SURROUND * s[FC_71] + COEFF_SURROUND * s[SR_71] + COEFF_SURROUND * s[SBR_71] + COEFF_LFE * s[LFE_71]
}
