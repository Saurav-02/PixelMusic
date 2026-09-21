/*
 * Ported for PixelMusic
 * GPL-3.0 License
 */

package com.saurav.pixelmusic.data.shazam

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicRecognizer {

    @SuppressLint("MissingPermission")
    suspend fun recognizeCurrentAudio(): Result<RecognitionResult> = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        
        // Use a slightly larger buffer for stability
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 4096
        
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return@withContext Result.failure(Exception("Failed to initialize microphone. Check permissions."))
        }

        val generator = ShazamSignatureGenerator()
        val buffer = ShortArray(bufferSize)

        audioRecord.startRecording()

        try {
            val startTime = System.currentTimeMillis()

            // FORCE the microphone to listen for exactly 4 seconds to build a rich signature.
            while (System.currentTimeMillis() - startTime < 4000) {
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    generator.feedPcm16Mono(buffer.copyOfRange(0, readSize))
                }
            }

            // Now that we have 4 seconds of audio, generate the final fingerprint
            val signature = generator.nextSignatureOrNull()

            if (signature == null) {
                return@withContext Result.failure(Exception("Could not capture audio signature. It might be too quiet."))
            }

            // Send the complete signature to Shazam's API
            return@withContext Shazam.recognize(signature.uri, signature.sampleDurationMs)

        } finally {
            runCatching {
                audioRecord.stop()
                audioRecord.release()
            }
            generator.reset()
        }
    }
}
