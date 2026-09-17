package jp.circadianguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.circadianguard.data.DetectionEvent
import jp.circadianguard.data.LightSample
import jp.circadianguard.data.PlaceCluster
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentLux by viewModel.currentLux.collectAsState()
    val samples by viewModel.todaySamples.collectAsState()
    val hasSensor by viewModel.hasSensor.collectAsState()
    val places by viewModel.places.collectAsState()
    val detections by viewModel.detections.collectAsState()
    val locationGranted by viewModel.locationGranted.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val notificationsGranted by viewModel.notificationsGranted.collectAsState()
    val message by viewModel.message.collectAsState()

    var showRegisterDialog by remember { mutableStateOf(false) }

    val placeNames = remember(places) { places.associate { it.id to it.name } }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("CircadianGuard") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Current illuminance", style = MaterialTheme.typography.titleMedium)
            Text(
                text = currentLux?.let { String.format(Locale.US, "%.0f lx", it) } ?: "Measuring…",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!hasSensor) {
                Text(
                    text = "This device has no ambient light sensor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Today's light trend", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Samples: ${samples.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LuxChart(
                samples = samples,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            Spacer(Modifier.height(24.dp))

            Text("Places", style = MaterialTheme.typography.titleMedium)
            if (!locationGranted) {
                Text(
                    text = "Coarse location permission is needed to register places.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { showRegisterDialog = true },
                enabled = locationGranted && currentLocation != null,
            ) {
                Text("Register current place")
            }
            if (locationGranted && currentLocation == null) {
                Text(
                    text = "Waiting for a location fix…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (places.isEmpty()) {
                Text(
                    text = "No places yet. Register your current location as a place to label samples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            places.forEach { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${place.name} · ${place.radiusM} m",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { viewModel.deletePlace(place) }) {
                        Text("Remove")
                    }
                }
            }
            message?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Recent detections", style = MaterialTheme.typography.titleMedium)
            if (!notificationsGranted) {
                Text(
                    text = "Notifications are off — deviations are logged but not shown as alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (detections.isEmpty()) {
                Text(
                    text = "No deviations detected yet. The app learns each place's usual light first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            detections.forEach { detection ->
                Text(
                    text = formatDetection(detection, placeNames[detection.clusterId]),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
    }

    if (showRegisterDialog) {
        RegisterPlaceDialog(
            onDismiss = { showRegisterDialog = false },
            onConfirm = { name ->
                viewModel.registerPlace(name)
                showRegisterDialog = false
            },
        )
    }
}

@Composable
private fun RegisterPlaceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("Home") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register place") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatDetection(detection: DetectionEvent, name: String?): String {
    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(detection.ts))
    val label = name ?: "Place #${detection.clusterId}"
    return String.format(
        Locale.US,
        "%s  %s — %.0f lx vs usual ~%.0f lx (%s)",
        time,
        label,
        detection.observedLux,
        detection.baselineMean,
        detection.direction.name.lowercase(Locale.US),
    )
}

@Composable
private fun LuxChart(samples: List<LightSample>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        // Horizontal grid lines (4 divisions)
        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        if (samples.isEmpty()) return@Canvas

        val maxLux = samples.maxOf { it.lux }.coerceAtLeast(1f)
        val minTs = samples.first().ts
        val maxTs = samples.last().ts
        val spanTs = (maxTs - minTs).coerceAtLeast(1L)

        if (samples.size == 1) {
            val s = samples.first()
            val x = 0f
            val y = size.height - (s.lux / maxLux) * size.height
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
            return@Canvas
        }

        val path = Path()
        samples.forEachIndexed { i, s ->
            val x = ((s.ts - minTs).toFloat() / spanTs) * size.width
            val y = size.height - (s.lux / maxLux) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
