package com.example.hardware

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log

enum class AudioOutputMode(val displayName: String) {
    WIRED_HEADSET("Wired Headset / Antenna"),
    TABLET_SPEAKERS("Tablet Speakers (Tab A9+)")
}

class QualcommAudioRouter(private val context: Context) {
    companion object {
        private const val TAG = "QualcommAudioRouter"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var currentOutputMode: AudioOutputMode = AudioOutputMode.WIRED_HEADSET
        private set
    var isFmAudioActive: Boolean = false
        private set

    fun isHeadsetConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                return true
            }
        }
        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn
    }

    fun startFmAudioRoute() {
        try {
            audioManager.setParameters("fm_mode=on")
            audioManager.setParameters("fm_radio_volume=on")
            audioManager.setParameters("handle_fm=1")
            isFmAudioActive = true
            applyOutputRouting(currentOutputMode)
            Log.i(TAG, "Qualcomm SM6375 FM Audio routing started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting FM audio route: ${e.message}")
        }
    }

    fun applyOutputRouting(mode: AudioOutputMode) {
        currentOutputMode = mode
        try {
            when (mode) {
                AudioOutputMode.TABLET_SPEAKERS -> {
                    audioManager.isSpeakerphoneOn = true
                    audioManager.setParameters("fm_routing=speaker")
                    audioManager.setParameters("force_use=1") // FORCE_SPEAKER
                    Log.i(TAG, "Routed FM audio to Samsung Galaxy Tab A9+ Built-in Speakers.")
                }
                AudioOutputMode.WIRED_HEADSET -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.setParameters("fm_routing=headset")
                    audioManager.setParameters("force_use=0") // FORCE_NONE / HEADSET
                    Log.i(TAG, "Routed FM audio to Wired Headphone Output.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Note on routing audio parameters: ${e.message}")
        }
    }

    fun stopFmAudioRoute() {
        try {
            audioManager.setParameters("fm_mode=off")
            audioManager.setParameters("fm_radio_volume=off")
            audioManager.isSpeakerphoneOn = false
            isFmAudioActive = false
            Log.i(TAG, "Qualcomm SM6375 FM Audio routing stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping FM audio: ${e.message}")
        }
    }
}
