package com.saurav.pixelmusic.data.remote.jiosaavn

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts JioSaavn's encrypted_media_url values.
 * Scheme (stable since ~2022): DES/ECB/PKCS5Padding with ASCII key "38346591".
 */
object JioSaavnCrypto {

    private val DES_KEY = "38346591".toByteArray(Charsets.UTF_8)

    /**
     * Returns the direct stream URL (e.g. https://aac.saavncdn.com/..._96.mp4),
     * or null if decryption fails.
     */
    fun decryptMediaUrl(encryptedMediaUrl: String): String? {
        if (encryptedMediaUrl.isBlank()) return null
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(DES_KEY, "DES"))
            val decoded = Base64.decode(encryptedMediaUrl.trim(), Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_8).trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Swaps the bitrate suffix (_96/_160/_320) on a decrypted Saavn CDN URL.
     */
    fun withBitrate(streamUrl: String, kbps: Int): String {
        if (kbps <= 0) return streamUrl
        return streamUrl.replace(Regex("_\\d+\\.mp4$"), "_${kbps}.mp4")
    }
}
