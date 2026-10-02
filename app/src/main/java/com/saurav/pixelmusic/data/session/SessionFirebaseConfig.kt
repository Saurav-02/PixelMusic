package com.saurav.pixelmusic.data.session

import com.saurav.pixelmusic.BuildConfig

/**
 * Firebase project values for Listen Together (project: listen-together-02,
 * free Spark tier — no payment card needed).
 *
 * The Firebase App ID is picked at runtime from the running package name, so
 * test builds (com.saurav.pixelmusic.test) and the production release
 * (com.saurav.pixelmusic) both work with no code changes.
 */
internal object SessionFirebaseConfig {
    const val API_KEY = "AIzaSyB0Z7CTeQSzDWNTZsoSMmm67h02kDBSV4A"
    const val PROJECT_ID = "listen-together-02"
    const val DATABASE_URL = "https://listen-together-02-default-rtdb.firebaseio.com"

    private const val APP_ID_TEST = "1:407682818529:android:9510bc0f6b83e8536ab2a0"
    private const val APP_ID_PROD = "1:407682818529:android:a797f4b76dbfef416ab2a0"

    val applicationId: String
        get() = if (BuildConfig.APPLICATION_ID == "com.saurav.pixelmusic.test") {
            APP_ID_TEST
        } else {
            APP_ID_PROD
        }
}
