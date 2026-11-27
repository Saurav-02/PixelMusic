package com.saurav.pixelmusic.audiolab

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.audiolab.audio.CustomEqualizerAudioProcessor
import com.saurav.pixelmusic.audiolab.data.ParametricEQ
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** hands the current equalizer design to every player's processor (a crossmix runs two at once), one added later picks up the current design */
@Singleton
@OptIn(UnstableApi::class)
class EqualizerService @Inject constructor() {

    private val audioProcessors = CopyOnWriteArrayList<CustomEqualizerAudioProcessor>()

    @Volatile private var current: ParametricEQ? = null

    fun addAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.add(processor)
        current?.let(processor::applyProfile) ?: processor.disable()
    }

    fun removeAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.remove(processor)
    }

    /** null switches the equalizer off; either way the change is live and click-free */
    fun apply(eq: ParametricEQ?) {
        current = eq
        audioProcessors.forEach { processor ->
            if (eq != null) processor.applyProfile(eq) else processor.disable()
        }
    }

    fun disable() = apply(null)

    fun isEnabled(): Boolean = current != null

    fun release() {
        audioProcessors.clear()
    }
}
