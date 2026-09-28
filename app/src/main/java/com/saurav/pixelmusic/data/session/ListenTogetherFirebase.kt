package com.saurav.pixelmusic.data.session

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Manual Firebase setup for Listen Together — no google-services.json needed,
 * so the values stay visible and editable in [SessionFirebaseConfig].
 *
 * Called once from [com.saurav.pixelmusic.PixelMusicApplication.onCreate].
 * Everything here is defensive: if Firebase can't start, the feature simply
 * stays dormant instead of crashing the app.
 */
object ListenTogetherFirebase {

    @Volatile
    private var initialized = false

    /**
     * Initializes Firebase (idempotent). Safe to call from Application.onCreate;
     * never throws.
     */
    fun init(context: Context) {
        if (initialized) return
        try {
            // Don't double-init if something else already created the default app.
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(SessionFirebaseConfig.API_KEY)
                    .setApplicationId(SessionFirebaseConfig.applicationId)
                    .setProjectId(SessionFirebaseConfig.PROJECT_ID)
                    .setDatabaseUrl(SessionFirebaseConfig.DATABASE_URL)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            initialized = true
            Timber.d("ListenTogether: Firebase initialized")
        } catch (t: Throwable) {
            Timber.w(t, "ListenTogether: Firebase init failed")
        }
    }

    /**
     * Ensures there's an anonymous Firebase Auth user, signing in if needed.
     * Returns false when Firebase isn't ready or sign-in fails.
     */
    suspend fun ensureSignedIn(): Boolean {
        if (!initialized) return false
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            auth.currentUser != null
        } catch (t: Throwable) {
            Timber.w(t, "ListenTogether: anonymous sign-in failed")
            false
        }
    }

    /** True once [init] has succeeded. */
    fun isReady(): Boolean = initialized
}
