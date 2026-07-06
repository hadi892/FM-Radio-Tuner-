package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FmRadioScreen
import com.example.ui.FmRadioViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TunerDarkBackground

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = TunerDarkBackground) {
          val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { _ -> }

          LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
              permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          }

          val viewModel: FmRadioViewModel = viewModel()
          val uiState by viewModel.uiState.collectAsState()
          val favoriteStations by viewModel.favoriteStations.collectAsState()
          val allStations by viewModel.allDiscoveredStations.collectAsState()

          FmRadioScreen(
            uiState = uiState,
            favoriteStations = favoriteStations,
            allStations = allStations,
            onTuneFrequency = { freq -> viewModel.tuneFrequency(freq) },
            onTogglePower = { viewModel.togglePower() },
            onToggleMute = { viewModel.toggleMute() },
            onSetAudioMode = { mode -> viewModel.setAudioOutputMode(mode) },
            onStepDown = { viewModel.stepFrequency(false) },
            onStepUp = { viewModel.stepFrequency(true) },
            onSeekDown = { viewModel.seekNextStation(false) },
            onSeekUp = { viewModel.seekNextStation(true) },
            onStartScan = { viewModel.startFullBandScan() },
            onCancelScan = { viewModel.cancelScan() },
            onToggleFavorite = { viewModel.toggleFavoriteCurrentStation() },
            onDeleteStation = { station -> viewModel.deleteStationPreset(station) },
            onUpdateStationName = { freq, name -> viewModel.updateStationName(freq, name) },
            onShowDiagnostics = { show -> viewModel.setShowDiagnostics(show) },
            onShowDirectEntry = { show -> viewModel.setShowDirectEntry(show) }
          )
        }
      }
    }
  }
}
