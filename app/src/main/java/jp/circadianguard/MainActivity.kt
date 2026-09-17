package jp.circadianguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import jp.circadianguard.sensor.LightSensorMonitor
import jp.circadianguard.ui.MainScreen
import jp.circadianguard.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var monitor: LightSensorMonitor? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onLocationPermissionChanged(
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
        if (Build.VERSION.SDK_INT >= 33) {
            viewModel.onNotificationPermissionChanged(
                result[Manifest.permission.POST_NOTIFICATIONS] == true,
            )
        } else {
            viewModel.onNotificationPermissionChanged(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val m = LightSensorMonitor(
            context = this,
            onLux = { viewModel.onLux(it) },
            onAvailability = { viewModel.setSensorAvailable(it) },
        )
        monitor = m
        m.start()
        requestPermissionsIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        monitor?.stop()
        monitor = null
    }

    private fun requestPermissionsIfNeeded() {
        val toRequest = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            toRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            viewModel.onLocationPermissionChanged(true)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onNotificationPermissionChanged(true)
            }
        } else {
            viewModel.onNotificationPermissionChanged(true)
        }

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }
}
