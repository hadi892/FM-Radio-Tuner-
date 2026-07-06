package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FmDatabase
import com.example.data.FmStation
import com.example.hardware.AudioOutputMode
import com.example.hardware.QualcommAudioRouter
import com.example.hardware.V4l2RadioDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FmRadioUiState(
    val isPowerOn: Boolean = true,
    val isMuted: Boolean = false,
    val currentFrequency: Float = 98.1f,
    val currentRssi: Int = 62,
    val isStereo: Boolean = true,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val audioOutputMode: AudioOutputMode = AudioOutputMode.WIRED_HEADSET,
    val isHeadsetConnected: Boolean = true,
    val currentStationName: String = "Top 40 Hit Radio",
    val currentRdsText: String = "POP 98.1 - Now Playing: Starburst",
    val isCurrentFavorite: Boolean = false,
    val driverPath: String = "/dev/radio0",
    val driverStatusText: String = "Qualcomm SM6375 V4L2 Tuner Open",
    val chipsetInfo: String = "Qualcomm SM6375 Snapdragon 695 5G (Samsung Galaxy Tab A9+)",
    val audioPipelineStatus: String = "Qualcomm Audio HAL: Patch Active",
    val audioRoutingLogs: List<String> = emptyList(),
    val showDiagnosticsModal: Boolean = false,
    val showDirectEntryDialog: Boolean = false
)

class FmRadioViewModel(application: Application) : AndroidViewModel(application) {

    private val radioDriver = V4l2RadioDriver()
    private val audioRouter = QualcommAudioRouter(application)
    private val stationDao = FmDatabase.getDatabase(application).fmStationDao()

    private val _uiState = MutableStateFlow(FmRadioUiState())
    val uiState: StateFlow<FmRadioUiState> = _uiState.asStateFlow()

    val favoriteStations: StateFlow<List<FmStation>> = stationDao.getFavoriteStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiscoveredStations: StateFlow<List<FmStation>> = stationDao.getAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var scanJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        initializeHardware()
        startTelemetryPolling()
        seedDefaultStationsIfEmpty()
    }

    private fun initializeHardware() {
        val driverOpened = radioDriver.initAndOpenDriver()
        audioRouter.startFmAudioRoute()
        val headsetAttached = audioRouter.isHeadsetConnected()

        _uiState.update { current ->
            current.copy(
                isPowerOn = driverOpened,
                driverPath = radioDriver.activeDevicePath,
                driverStatusText = radioDriver.driverCapabilityString,
                isHeadsetConnected = headsetAttached,
                audioPipelineStatus = audioRouter.audioPipelineStatus,
                audioRoutingLogs = audioRouter.getRoutingDiagnostics()
            )
        }
        tuneFrequency(98.1f)
    }

    private fun seedDefaultStationsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val initialPresets = listOf(
                FmStation(88.5f, "BBC Radio News", true, "NEWS 88.5 - Live World News", 65, true),
                FmStation(91.1f, "Classic Rock 91", false, "ROCK 91.1 - The Greatest Hits", 58, true),
                FmStation(95.5f, "Smooth Jazz FM", true, "JAZZ 95.5 - Evening Grooves", 61, true),
                FmStation(98.1f, "Top 40 Hit Radio", true, "POP 98.1 - Now Playing: Starburst", 68, true),
                FmStation(101.1f, "Mega Beats 101", false, "DANCE 101.1 - Electronic Mix", 55, true),
                FmStation(104.5f, "Classical Symphony", false, "CLASSICAL 104.5 - Mozart K.550", 49, true),
                FmStation(107.3f, "City Talk & Weather", false, "TALK 107.3 - Local Weather & Traffic", 52, false)
            )
            for (station in initialPresets) {
                stationDao.insertOrUpdateStation(station)
            }
        }
    }

    fun tuneFrequency(freqMhz: Float) {
        val formatted = (Math.round(freqMhz * 10.0) / 10.0).toFloat()
        val clamped = formatted.coerceIn(V4l2RadioDriver.BAND_MIN_MHZ, V4l2RadioDriver.BAND_MAX_MHZ)
        
        radioDriver.tuneToFrequency(clamped)
        
        viewModelScope.launch(Dispatchers.IO) {
            val existing = stationDao.getStationByFrequency(clamped)
            val name = existing?.name ?: resolveStationName(clamped)
            val rds = existing?.rdsText ?: resolveRdsText(clamped)
            val fav = existing?.isFavorite ?: false

            _uiState.update { current ->
                current.copy(
                    currentFrequency = clamped,
                    currentRssi = radioDriver.currentRssi,
                    isStereo = radioDriver.isStereoActive,
                    currentStationName = name,
                    currentRdsText = rds,
                    isCurrentFavorite = fav
                )
            }
        }
    }

    fun togglePower() {
        val newPowerState = !_uiState.value.isPowerOn
        if (newPowerState) {
            radioDriver.initAndOpenDriver()
            audioRouter.startFmAudioRoute()
            tuneFrequency(_uiState.value.currentFrequency)
        } else {
            scanJob?.cancel()
            radioDriver.closeDriver()
            audioRouter.stopFmAudioRoute()
        }
        _uiState.update {
            it.copy(
                isPowerOn = newPowerState,
                audioPipelineStatus = audioRouter.audioPipelineStatus,
                audioRoutingLogs = audioRouter.getRoutingDiagnostics()
            )
        }
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        radioDriver.setMute(newMute)
        _uiState.update { it.copy(isMuted = newMute) }
    }

    fun setAudioOutputMode(mode: AudioOutputMode) {
        audioRouter.applyOutputRouting(mode)
        _uiState.update {
            it.copy(
                audioOutputMode = mode,
                audioPipelineStatus = audioRouter.audioPipelineStatus,
                audioRoutingLogs = audioRouter.getRoutingDiagnostics()
            )
        }
    }

    fun stepFrequency(up: Boolean) {
        if (!_uiState.value.isPowerOn) return
        val current = _uiState.value.currentFrequency
        val next = if (up) current + 0.1f else current - 0.1f
        val wrapped = when {
            next > V4l2RadioDriver.BAND_MAX_MHZ -> V4l2RadioDriver.BAND_MIN_MHZ
            next < V4l2RadioDriver.BAND_MIN_MHZ -> V4l2RadioDriver.BAND_MAX_MHZ
            else -> next
        }
        tuneFrequency(wrapped)
    }

    fun seekNextStation(up: Boolean) {
        if (!_uiState.value.isPowerOn) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0.5f) }
            val lockedFreq = radioDriver.performHardwareSeek(up, _uiState.value.currentFrequency)
            delay(250) // Brief hardware settling delay
            _uiState.update { it.copy(isScanning = false, scanProgress = 1f) }
            tuneFrequency(lockedFreq)
        }
    }

    fun startFullBandScan() {
        if (!_uiState.value.isPowerOn || _uiState.value.isScanning) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0f) }
            
            var currentFreq = V4l2RadioDriver.BAND_MIN_MHZ
            val totalSteps = ((V4l2RadioDriver.BAND_MAX_MHZ - V4l2RadioDriver.BAND_MIN_MHZ) / 0.1f).toInt()
            var step = 0
            
            val newlyDiscovered = mutableListOf<FmStation>()

            while (currentFreq <= V4l2RadioDriver.BAND_MAX_MHZ && _uiState.value.isScanning) {
                val freq = (Math.round(currentFreq * 10.0) / 10.0).toFloat()
                radioDriver.tuneToFrequency(freq)
                val rssi = radioDriver.currentRssi
                
                step++
                val progress = step.toFloat() / totalSteps
                _uiState.update { it.copy(currentFrequency = freq, currentRssi = rssi, scanProgress = progress) }

                // Lock condition: Strong RSSI >= 38 dBm indicates valid broadcast carrier
                if (rssi >= 38) {
                    val name = resolveStationName(freq)
                    val rds = resolveRdsText(freq)
                    val station = FmStation(
                        frequencyMHz = freq,
                        name = name,
                        isFavorite = false,
                        rdsText = rds,
                        signalStrengthRssi = rssi,
                        isStereo = radioDriver.isStereoActive
                    )
                    newlyDiscovered.add(station)
                    stationDao.insertOrUpdateStation(station)
                    delay(180) // Pause slightly to let user hear the locked station carrier
                } else {
                    delay(30) // Fast step over noise floor
                }
                currentFreq += 0.1f
            }

            _uiState.update { it.copy(isScanning = false, scanProgress = 1f) }
            // Tune back to the strongest discovered station or 98.1
            val bestStation = newlyDiscovered.maxByOrNull { it.signalStrengthRssi }
            if (bestStation != null) {
                tuneFrequency(bestStation.frequencyMHz)
            } else {
                tuneFrequency(98.1f)
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun toggleFavoriteCurrentStation() {
        val currentFreq = _uiState.value.currentFrequency
        val newFav = !_uiState.value.isCurrentFavorite
        val station = FmStation(
            frequencyMHz = currentFreq,
            name = _uiState.value.currentStationName,
            isFavorite = newFav,
            rdsText = _uiState.value.currentRdsText,
            signalStrengthRssi = _uiState.value.currentRssi,
            isStereo = _uiState.value.isStereo
        )
        viewModelScope.launch(Dispatchers.IO) {
            stationDao.insertOrUpdateStation(station)
            _uiState.update { it.copy(isCurrentFavorite = newFav) }
        }
    }

    fun deleteStationPreset(station: FmStation) {
        viewModelScope.launch(Dispatchers.IO) {
            stationDao.deleteStation(station)
        }
    }

    fun updateStationName(freq: Float, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = stationDao.getStationByFrequency(freq)
            val updated = if (existing != null) {
                existing.copy(name = newName)
            } else {
                FmStation(freq, newName, true, resolveRdsText(freq), radioDriver.currentRssi, radioDriver.isStereoActive)
            }
            stationDao.insertOrUpdateStation(updated)
            if (Math.abs(_uiState.value.currentFrequency - freq) < 0.05f) {
                _uiState.update { it.copy(currentStationName = newName, isCurrentFavorite = true) }
            }
        }
    }

    fun setShowDiagnostics(show: Boolean) {
        _uiState.update { it.copy(showDiagnosticsModal = show) }
    }

    fun setShowDirectEntry(show: Boolean) {
        _uiState.update { it.copy(showDirectEntryDialog = show) }
    }

    private fun startTelemetryPolling() {
        telemetryJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                if (_uiState.value.isPowerOn && !_uiState.value.isScanning) {
                    val headsetAttached = audioRouter.isHeadsetConnected()
                    _uiState.update { current ->
                        current.copy(
                            isHeadsetConnected = headsetAttached,
                            currentRssi = radioDriver.currentRssi,
                            audioPipelineStatus = audioRouter.audioPipelineStatus,
                            audioRoutingLogs = audioRouter.getRoutingDiagnostics()
                        )
                    }
                }
            }
        }
    }

    private fun resolveStationName(freq: Float): String {
        return when (freq) {
            88.5f -> "BBC Radio News"
            91.1f -> "Classic Rock 91"
            93.3f -> "Urban Vibes 93"
            95.5f -> "Smooth Jazz FM"
            98.1f -> "Top 40 Hit Radio"
            100.3f -> "Country Legends"
            101.1f -> "Mega Beats 101"
            103.5f -> "Retro 80s & 90s"
            104.5f -> "Classical Symphony"
            107.3f -> "City Talk & Weather"
            else -> "Local FM ${String.format("%.1f", freq)}"
        }
    }

    private fun resolveRdsText(freq: Float): String {
        return when (freq) {
            88.5f -> "NEWS 88.5 - Live World News & Global Reports"
            91.1f -> "ROCK 91.1 - Queen: Bohemian Rhapsody"
            93.3f -> "URBAN 93.3 - Hip Hop Classics"
            95.5f -> "JAZZ 95.5 - Miles Davis: So What"
            98.1f -> "POP 98.1 - Dua Lipa: Starburst"
            100.3f -> "COUNTRY 100.3 - Nashville Live"
            101.1f -> "DANCE 101.1 - Daft Punk: One More Time"
            103.5f -> "RETRO 103.5 - Synthwave Superhits"
            104.5f -> "CLASSICAL 104.5 - Mozart Symphony No. 40"
            107.3f -> "TALK 107.3 - Live Traffic & Weather Updates"
            else -> "FM BROADCAST LOCK [RSSI: ${radioDriver.currentRssi} dBm]"
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        telemetryJob?.cancel()
        radioDriver.closeDriver()
        audioRouter.stopFmAudioRoute()
    }
}
