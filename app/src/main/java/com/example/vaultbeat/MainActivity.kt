package com.example.vaultbeat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vaultbeat.ui.library.LibraryScreen
import com.example.vaultbeat.ui.library.LibraryViewModel
import com.example.vaultbeat.ui.nowplaying.NowPlayingScreen
import com.example.vaultbeat.ui.nowplaying.NowPlayingViewModel
import com.example.vaultbeat.ui.theme.VaultBeatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VaultBeatTheme { VaultBeatApp() } }
    }
}

@Composable
private fun VaultBeatApp(viewModel: LibraryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var audioGranted by remember { mutableStateOf(hasAudioPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        audioGranted = result[audioPermission()] == true
    }
    val library by viewModel.state.collectAsState()
    val player by viewModel.player.state.collectAsState()

    LaunchedEffect(audioGranted) { if (audioGranted) viewModel.loadLibrary() }

    if (audioGranted) {
        // Single main screen approach: show the combined layout (left menu + thumbnails + content + wheel)
        val nowVm: NowPlayingViewModel = hiltViewModel()
        NowPlayingScreen(
            viewModel = nowVm,
            songs = library.songs,
            onSongSelected = { index -> viewModel.player.playQueue(library.songs, index) },
            onRefresh = viewModel::loadLibrary
        )
    } else {
        PermissionScreen {
            permissionLauncher.launch(buildList {
                add(audioPermission())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray())
        }
    }
}

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tu música, en tu dispositivo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("VaultBeat necesita permiso para leer tu biblioteca de audio local. No se sube ni se descarga música.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("Permitir acceso a la música") }
    }
}

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        audioPermission()
    ) == PackageManager.PERMISSION_GRANTED

private fun audioPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}
