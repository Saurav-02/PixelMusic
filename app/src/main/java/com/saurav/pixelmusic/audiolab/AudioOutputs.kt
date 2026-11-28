package com.saurav.pixelmusic.audiolab

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioOutputs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class Kind { PHONE, WIRED, USB, BLUETOOTH, HDMI }

    data class Device(
        val id: Int,
        val name: String,
        val kind: Kind,
        val isActive: Boolean,
        val info: AudioDeviceInfo,
    )

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _activeInfo = MutableStateFlow<AudioDeviceInfo?>(null)
    val activeInfo: StateFlow<AudioDeviceInfo?> = _activeInfo.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }
    }

    init {
        refresh()
        runCatching {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }
    }

    fun refresh() {
        val outs = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }.getOrDefault(emptyList())
        val mapped = outs.mapNotNull { info ->
            val k = kindOf(info.type) ?: return@mapNotNull null
            Device(
                id = info.id,
                name = info.productName?.toString()?.trim().orEmpty(),
                kind = k,
                isActive = false,
                info = info,
            )
        }
        val active = mapped.firstOrNull { it.kind == Kind.USB }
            ?: mapped.firstOrNull { it.kind == Kind.WIRED }
            ?: mapped.firstOrNull { it.kind == Kind.BLUETOOTH }
            ?: mapped.firstOrNull { it.kind == Kind.HDMI }
            ?: mapped.firstOrNull { it.kind == Kind.PHONE }

        _devices.value = mapped.map { it.copy(isActive = it.id == active?.id) }
        _activeInfo.value = active?.info
    }

    companion object {
        fun kindOf(type: Int): Kind? = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Kind.PHONE
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_AUX_LINE -> Kind.WIRED
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_HEADSET -> Kind.USB
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Kind.BLUETOOTH
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC -> Kind.HDMI
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
                ) Kind.BLUETOOTH else null
            }
        }
    }
}
