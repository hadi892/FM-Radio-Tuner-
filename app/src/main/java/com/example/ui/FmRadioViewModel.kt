package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FmDatabase
import com.example.data.FmStation
import com.example.hardware.AudioOutputMode
import com.example.hardware.HardwareScanResult
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
    val currentRssi: Int = V4l2RadioDriver.NOISE_FLOOR_RSSI,
    val isStereo: Boolean = false,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val scanDiagnostics: List<HardwareScanResult> = emptyList(),
    val audioOutputMode: AudioOutputMode = AudioOutputMode.WIRED_HEADSET,
    val isHeadsetConnected: Boolean = true,
    val currentStationName: String = "FM 98.1 MHz",
    val currentRdsText: String = "V4L2 Tuner Standby [Direct /dev/radio0 Access]",
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
            delay(200) // Brief hardware settling delay
            _uiState.update { it.copy(isScanning = false, scanProgress = 1f) }
            tuneFrequency(lockedFreq)
        }
    }

    fun startFullBandScan() {
        if (!_uiState.value.isPowerOn || _uiState.value.isScanning) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            // Purge all previously discovered or unconfirmed stations before hardware audit
            stationDao.deleteAllStations()

            _uiState.update { it.copy(isScanning = true, scanProgress = 0f, scanDiagnostics = emptyList()) }
            
            val diagnosticsList = mutableListOf<HardwareScanResult>()
            val newlyDiscovered = mutableListOf<FmStation>()

            val evaluatedResults = radioDriver.performHardwareFullScan { progress, evaluation ->
                diagnosticsList.add(evaluation)
                _uiState.update { current ->
                    current.copy(
                        currentFrequency = evaluation.frequencyMHz,
                        currentRssi = evaluation.rssi,
                        isStereo = evaluation.isStereo,
                        scanProgress = progress,
                        scanDiagnostics = diagnosticsList.toList()
                    )
                }

                // Strictly require physical hardware carrier confirmation before accepting station
                if (evaluation.isLocked) {
                    val station = FmStation(
                        frequencyMHz = evaluation.frequencyMHz,
                        name = resolveStationName(evaluation.frequencyMHz),
                        isFavorite = false,
                        rdsText = if (evaluation.rdsAvailable) "RDS SYNC ACTIVE [19kHz Pilot Verified]" else "V4L2 Hardware Lock [RSSI: ${evaluation.rssi} dBm]",
                        signalStrengthRssi = evaluation.rssi,
                        isStereo = evaluation.isStereo
                    )
                    newlyDiscovered.add(station)
                    viewModelScope.launch(Dispatchers.IO) {
                        stationDao.insertOrUpdateStation(station)
                    }
                }
            }

            _uiState.update { it.copy(isScanning = false, scanProgress = 1f, scanDiagnostics = evaluatedResults) }

            val bestStation = newlyDiscovered.maxByOrNull { it.signalStrengthRssi }
            if (bestStation != null) {
                tuneFrequency(bestStation.frequencyMHz)
            } else {
                tuneFrequency(_uiState.value.currentFrequency)
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
        return "FM ${String.format("%.1f", freq)} MHz"
    }

    private fun resolveRdsText(freq: Float): String {
        return "V4L2 RF Lock [RSSI: ${radioDriver.currentRssi} dBm]"
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        telemetryJob?.cancel()
        radioDriver.closeDriver()
        audioRouter.stopFmAudioRoute()
    }
}

