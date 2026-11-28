package com.saurav.pixelmusic.data.worker

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequest
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.saurav.pixelmusic.data.observer.MediaStoreObserver
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing the progress of the sync operation.
 */
data class SyncProgress(
    val isRunning: Boolean = false,
    val currentCount: Int = 0,
    val totalCount: Int = 0,
    val isCompleted: Boolean = false,
    val phase: SyncPhase = SyncPhase.IDLE
) {
    enum class SyncPhase {
        IDLE,
        FETCHING_MEDIASTORE,
        PROCESSING_FILES,
        SAVING_TO_DATABASE,
        SCANNING_LRC,
        CLEANING_CACHE,
        SYNCING_CLOUD,
        COMPLETING
    }

    val progress: Float
        get() = if (totalCount > 0) currentCount.toFloat() / totalCount else 0f

    val hasProgress: Boolean
        get() = totalCount > 0
    }

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mediaStoreObserver: MediaStoreObserver
) {
    private val workManager = WorkManager.getInstance(context)
    private val sharingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // EXPONE UN FLOW<BOOLEAN> SIMPLE
    val isSyncing: Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME)
            .map { workInfos ->
                val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }
                val isEnqueued = workInfos.any { it.state == WorkInfo.State.ENQUEUED }
                isRunning || isEnqueued
            }
            .distinctUntilChanged()
            .shareIn(
                scope = sharingScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                replay = 1
            )

    init {
        // Auto-scan newly downloaded or modified audio files in real time (debounced 3s)
        @OptIn(FlowPreview::class)
        sharingScope.launch {
            mediaStoreObserver.externalMediaStoreChanges
                .debounce(AUTO_SYNC_DEBOUNCE_MS)
                .collect {
                    val setupDone = runCatching { userPreferencesRepository.initialSetupDoneFlow.first() }.getOrDefault(false)
                    if (!setupDone) return@collect
                    Log.i(TAG, "MediaStore change detected — scheduling automatic incremental sync")
                    enqueueSyncWork(
                        request = SyncWorker.incrementalSyncWork(),
                        policy = ExistingWorkPolicy.KEEP,
                        notifyObserver = false
                    )
                }
        }

        // Also catch files downloaded while the app was closed/backgrounded when returning to foreground
        sharingScope.launch {
            mediaStoreObserver.foregroundResumed.collect {
                val setupDone = runCatching { userPreferencesRepository.initialSetupDoneFlow.first() }.getOrDefault(false)
                if (!setupDone) return@collect
                val lastSyncMs = runCatching { userPreferencesRepository.getLastSyncTimestamp() }.getOrDefault(0L)
                if (lastSyncMs <= 0L) return@collect
                val hasNewAudio = hasNewOrModifiedAudioSince(lastSyncMs)
                if (hasNewAudio) {
                    Log.i(TAG, "New/modified audio detected on foreground resume — scheduling automatic incremental sync")
                    enqueueSyncWork(
                        request = SyncWorker.incrementalSyncWork(),
                        policy = ExistingWorkPolicy.KEEP,
                        notifyObserver = false
                    )
                }
            }
        }
    }

    private suspend fun hasNewOrModifiedAudioSince(lastSyncMs: Long): Boolean = withContext(Dispatchers.IO) {
        val lastSyncSec = (lastSyncMs / 1000L).coerceAtLeast(0L)
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (${MediaStore.Audio.Media.DATE_ADDED} > ? OR ${MediaStore.Audio.Media.DATE_MODIFIED} > ?)",
                arrayOf(lastSyncSec.toString(), lastSyncSec.toString()),
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        }.getOrDefault(false)
    }

    /**
     * Flow that exposes the detailed sync progress including song count.
     */
    val syncProgress: Flow<SyncProgress> =
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME)
            .map { workInfos ->
                val runningWork = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                val succeededWork = workInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED }
                val enqueuedWork = workInfos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }

                when {
                    runningWork != null -> {
                        val current = runningWork.progress.getInt(SyncWorker.PROGRESS_CURRENT, 0)
                        val total = runningWork.progress.getInt(SyncWorker.PROGRESS_TOTAL, 0)
                        val phaseOrdinal = runningWork.progress.getInt(SyncWorker.PROGRESS_PHASE, 0)
                        val phase = try {
                            SyncProgress.SyncPhase.entries[phaseOrdinal]
                        } catch (e: IndexOutOfBoundsException) {
                            SyncProgress.SyncPhase.IDLE
                        }
                        SyncProgress(
                            isRunning = true,
                            currentCount = current,
                            totalCount = total,
                            isCompleted = false,
                            phase = phase
                        )
                    }
                    succeededWork != null -> {
                        val total = succeededWork.outputData.getInt(SyncWorker.OUTPUT_TOTAL_SONGS, 0)
                        SyncProgress(
                            isRunning = false,
                            currentCount = total,
                            totalCount = total,
                            isCompleted = true,
                            phase = SyncProgress.SyncPhase.COMPLETING
                        )
                    }
                    enqueuedWork != null -> {
                        SyncProgress(isRunning = true, isCompleted = false, phase = SyncProgress.SyncPhase.IDLE)
                    }
                    else -> SyncProgress()
                }
            }
            .distinctUntilChanged()
            .shareIn(
                scope = sharingScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                replay = 1
            )

    /**
     * Emits `true` while the worker is in the early "library changes" phases —
     * scanning MediaStore for added/removed/modified files and writing them to the
     * unified DB. This is what powers the pull-to-refresh indicator: the UI only
     * needs to confirm that local additions/deletions have landed.
     */
    val isFetchingChanges: Flow<Boolean> = syncProgress
        .map { progress ->
            progress.isRunning && progress.phase in CHANGE_PHASES
        }
        .distinctUntilChanged()
        .shareIn(
            scope = sharingScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1
        )

    /**
     * Emits `true` while the worker is performing background maintenance that does
     * not gate the user's pull-to-refresh gesture: LRC scanning, album-art cache
     * cleanup, and cloud-source synchronization. This drives the slim linear
     * indicator under [LibraryActionRow].
     */
    val isPerformingMaintenance: Flow<Boolean> = syncProgress
        .map { progress ->
            progress.isRunning && progress.phase in MAINTENANCE_PHASES
        }
        .distinctUntilChanged()
        .shareIn(
            scope = sharingScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1
        )

    fun sync() {
        sharingScope.launch {
            val now = System.currentTimeMillis()
            val lastSyncTimestamp = userPreferencesRepository.getLastSyncTimestamp()
            val shouldRunSync =
                lastSyncTimestamp <= 0L || (now - lastSyncTimestamp) >= MIN_SYNC_INTERVAL_MS

            if (!shouldRunSync) {
                val ageSeconds = (now - lastSyncTimestamp) / 1000
                Log.d(TAG, "Skipping startup sync (last sync ${ageSeconds}s ago)")
                return@launch
            }

            Log.i(TAG, "Startup sync requested - Scheduling Incremental Sync")
            enqueueSyncWork(
                request = SyncWorker.incrementalSyncWork(),
                policy = ExistingWorkPolicy.KEEP,
                notifyObserver = false
            )
        }
    }

    /**
     * Performs an incremental sync, only processing files that have changed
     * since the last sync. Much faster for large libraries with few changes.
     * This is the recommended sync method for pull-to-refresh actions.
     */
    fun incrementalSync() {
        Log.i(TAG, "Incremental sync requested - Scheduling incremental worker")
        enqueueSyncWork(
            request = SyncWorker.incrementalSyncWork(),
            policy = ExistingWorkPolicy.REPLACE
        )
    }

    /**
     * Performs a full library rescan, ignoring the last sync timestamp.
     * Use this when the user explicitly wants to force a complete rescan.
     */
    fun fullSync() {
        Log.i(TAG, "Full sync requested - Scheduling full sync worker")
        enqueueSyncWork(
            request = SyncWorker.fullSyncWork(),
            policy = ExistingWorkPolicy.REPLACE
        )
    }

    /**
     * Completely rebuilds the database from scratch.
     * Clears all existing data including user edits (lyrics, etc.) and rescans.
     * Use when database is corrupted or songs are missing.
     */
    fun rebuildDatabase() {
        Log.i(TAG, "Rebuild database requested - Scheduling rebuild worker")
        enqueueSyncWork(
            request = SyncWorker.rebuildDatabaseWork(),
            policy = ExistingWorkPolicy.REPLACE
        )
    }

    /**
     * Fuerza una nueva sincronización, reemplazando cualquier trabajo de sincronización
     * existente. Ideal para el botón de "Refrescar Biblioteca".
     */
    fun forceRefresh() {
        Log.i(TAG, "Force refresh requested - Scheduling incremental worker")
        enqueueSyncWork(
            request = SyncWorker.incrementalSyncWork(),
            policy = ExistingWorkPolicy.REPLACE
        )
    }

    private fun enqueueSyncWork(
        request: OneTimeWorkRequest,
        policy: ExistingWorkPolicy,
        notifyObserver: Boolean = true
    ) {
        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            policy,
            request
        )
        if (notifyObserver) {
            // Keep reactive MediaStore-based views in sync with manual refresh actions.
            mediaStoreObserver.forceRescan()
        }
    }

    companion object {
        private const val TAG = "SyncManager"
        private const val MIN_SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
        private const val AUTO_SYNC_DEBOUNCE_MS = 3_000L

        private val CHANGE_PHASES = setOf(
            SyncProgress.SyncPhase.IDLE,
            SyncProgress.SyncPhase.FETCHING_MEDIASTORE,
            SyncProgress.SyncPhase.PROCESSING_FILES,
            SyncProgress.SyncPhase.SAVING_TO_DATABASE
        )

        private val MAINTENANCE_PHASES = setOf(
            SyncProgress.SyncPhase.SCANNING_LRC,
            SyncProgress.SyncPhase.CLEANING_CACHE,
            SyncProgress.SyncPhase.SYNCING_CLOUD,
            SyncProgress.SyncPhase.COMPLETING
        )
    }
}
