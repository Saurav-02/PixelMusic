package com.saurav.pixelmusic.data.remote.lyrics_providers.spotify

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SecretData(
    val secret: List<Int>,
    val version: Int
)

object SpotifySecrets {
    private val SOURCES = listOf(
        "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/refs/heads/main/secrets/secretBytes.json",
        "https://code.thetadev.de/ThetaDev/spotify-secrets/raw/branch/main/secrets/secretBytes.json",
        "https://code.thetadev.de/ThetaDev/spotify-secrets/raw/branch/main/secrets/secretDict.json",
    )

    private val BUNDLED = listOf(
        SecretData(listOf(123,105,79,70,110,59,52,125,60,49,80,70,89,75,80,86,63,53,123,37,117,49,52,93,77,62,47,86,48,104,68,72), 59),
        SecretData(listOf(79,109,69,123,90,65,46,74,94,34,58,48,70,71,92,85,122,63,91,64,87,87), 60),
        SecretData(listOf(44,55,47,42,70,40,34,114,76,74,50,111,120,97,75,76,94,102,43,69,49,120,118,80,64,78), 61),
    )

    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000 
    private val gson = Gson()
    
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun getPrefs() = appContext?.getSharedPreferences("spotify_secrets_prefs", Context.MODE_PRIVATE)

    fun current(): List<SecretData> {
        val prefs = getPrefs() ?: return BUNDLED
        val raw = prefs.getString("spotify_secrets_json", "") ?: ""
        if (raw.isBlank()) return BUNDLED
        
        return try {
            val type = object : TypeToken<List<SecretData>>() {}.type
            gson.fromJson<List<SecretData>>(raw, type).ifEmpty { BUNDLED }
        } catch (e: Exception) {
            BUNDLED
        }
    }

    fun lastFetchedAt(): Long? {
        return getPrefs()?.getLong("spotify_secrets_fetched_at", 0L)?.takeIf { it > 0 }
    }

    private fun isFresh(): Boolean {
        val t = lastFetchedAt() ?: return false
        return System.currentTimeMillis() - t < MAX_AGE_MS
    }

    suspend fun refresh(client: OkHttpClient, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!force && isFresh()) return@withContext false
        
        for (url in SOURCES) {
            val list = runCatching { fetch(client, url) }.getOrNull()
            if (!list.isNullOrEmpty()) {
                persist(list)
                Log.d("SpotifySecrets", "Updated secrets from $url (latest v${list.last().version})")
                return@withContext true
            }
        }
        return@withContext false
    }

    private fun fetch(client: OkHttpClient, url: String): List<SecretData> {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string()?.trim() ?: return emptyList()

        return if (body.startsWith("[")) {
            val type = object : TypeToken<List<SecretData>>() {}.type
            gson.fromJson(body, type)
        } else {
            val type = object : TypeToken<Map<String, List<Int>>>() {}.type
            val map: Map<String, List<Int>> = gson.fromJson(body, type)
            map.map { (version, secret) -> SecretData(secret, version.toInt()) }
        }.sortedBy { it.version }
    }

    private fun persist(list: List<SecretData>) {
        getPrefs()?.edit()
            ?.putString("spotify_secrets_json", gson.toJson(list))
            ?.putLong("spotify_secrets_fetched_at", System.currentTimeMillis())
            ?.apply()
    }
}

