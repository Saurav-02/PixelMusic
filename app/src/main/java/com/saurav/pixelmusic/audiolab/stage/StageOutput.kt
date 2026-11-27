package com.saurav.pixelmusic.audiolab.stage

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import com.saurav.pixelmusic.audiolab.AudioOutputs

/** where the music is playing, and whether that's a speaker (3d sound made for two ears doesn't work there) */
data class StageOutput(
    val key: String,
    val name: String,
    val kind: AudioOutputs.Kind,
    val guessedSpeaker: Boolean,
) {
    fun isSpeaker(settings: StageSettings): Boolean = settings.outputKinds[key] ?: guessedSpeaker

    companion object {
        val PhoneSpeaker = StageOutput("phone", "", AudioOutputs.Kind.PHONE, guessedSpeaker = true)

        private val SpeakerClasses = setOf(
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
            BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
        )

        fun of(context: Context, device: AudioDeviceInfo?): StageOutput {
            if (device == null) return PhoneSpeaker
            val kind = AudioOutputs.kindOf(device.type) ?: return PhoneSpeaker
            if (kind == AudioOutputs.Kind.PHONE) return PhoneSpeaker
            val name = if (kind == AudioOutputs.Kind.WIRED) "" else device.productName?.toString()?.trim().orEmpty()
            val speaker = when {
                kind == AudioOutputs.Kind.HDMI -> true
                device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER -> true
                device.type == AudioDeviceInfo.TYPE_LINE_ANALOG || device.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                    device.type == AudioDeviceInfo.TYPE_AUX_LINE -> true
                kind == AudioOutputs.Kind.BLUETOOTH -> bluetoothSpeaker(context, device.address)
                else -> false
            }
            return StageOutput("${kind.name}:${device.type}:$name", name, kind, speaker)
        }

        private fun bluetoothSpeaker(context: Context, address: String?): Boolean {
            if (address.isNullOrBlank()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) return false
            return runCatching {
                val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                adapter?.getRemoteDevice(address)?.bluetoothClass?.deviceClass in SpeakerClasses
            }.getOrDefault(false)
        }
    }
}
