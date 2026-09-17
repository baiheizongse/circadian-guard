package jp.circadianguard.ui

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.circadianguard.data.AppDatabase
import jp.circadianguard.data.BaselineStat
import jp.circadianguard.data.DayType
import jp.circadianguard.data.DetectionDirection
import jp.circadianguard.data.DetectionEvent
import jp.circadianguard.data.LightSample
import jp.circadianguard.data.PlaceCluster
import jp.circadianguard.data.SampleSource
import jp.circadianguard.geo.Geo
import jp.circadianguard.sensor.LocationTracker
import jp.circadianguard.stats.Stats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val sampleDao = db.lightSampleDao()
    private val placeDao = db.placeClusterDao()
    private val baselineDao = db.baselineStatDao()
    private val detectionDao = db.detectionEventDao()

    private val _currentLux = MutableStateFlow<Float?>(null)
    val currentLux: StateFlow<Float?> = _currentLux.asStateFlow()

    private val _todaySamples = MutableStateFlow<List<LightSample>>(emptyList())
    val todaySamples: StateFlow<List<LightSample>> = _todaySamples.asStateFlow()

    private val _hasSensor = MutableStateFlow(true)
    val hasSensor: StateFlow<Boolean> = _hasSensor.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _locationGranted = MutableStateFlow(false)
    val locationGranted: StateFlow<Boolean> = _locationGranted.asStateFlow()

    private val _notificationsGranted = MutableStateFlow(false)
    val notificationsGranted: StateFlow<Boolean> = _notificationsGranted.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val places: StateFlow<List<PlaceCluster>> = placeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val detections: StateFlow<List<DetectionEvent>> = detectionDao.getRecent(DETECTION_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var locationTracker: LocationTracker? = null
    private var cachedPlaces: List<PlaceCluster> = emptyList()

    /** (ts, lux) ring buffer for the ~5-minute smoothing window. */
    private val recentLux = ArrayDeque<Pair<Long, Float>>()

    /** Last alert timestamp per (cell, direction) for the cooldown window. */
    private val cooldowns = HashMap<String, Long>()

    private var lastSampleAt = 0L

    init {
        refreshToday()
        viewModelScope.launch {
            places.collect { cachedPlaces = it }
        }
    }

    fun setSensorAvailable(available: Boolean) {
        _hasSensor.value = available
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        _locationGranted.value = granted
        if (granted) startLocation() else stopLocation()
    }

    fun onNotificationPermissionChanged(granted: Boolean) {
        _notificationsGranted.value = granted
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** Register the current coarse location as a named place with the default radius. */
    fun registerPlace(name: String) {
        val trimmed = name.trim()
        val location = _currentLocation.value
        if (location == null) {
            _message.value = "No location yet — grant coarse location and wait for a fix."
            return
        }
        if (trimmed.isEmpty()) {
            _message.value = "Please enter a name."
            return
        }
        viewModelScope.launch {
            placeDao.insert(
                PlaceCluster(
                    name = trimmed,
                    lat = location.latitude,
                    lng = location.longitude,
                    radiusM = DEFAULT_RADIUS_M,
                    createdAt = System.currentTimeMillis(),
                )
            )
            _message.value = "Registered \"$trimmed\" with a ${DEFAULT_RADIUS_M} m radius."
        }
    }

    fun deletePlace(cluster: PlaceCluster) {
        viewModelScope.launch { placeDao.delete(cluster) }
    }

    /** Callback from the ambient light sensor. Throttled to 1 second for persistence. */
    fun onLux(lux: Float) {
        _currentLux.value = lux
        val now = System.currentTimeMillis()
        if (now - lastSampleAt < SAMPLE_INTERVAL_MS) return
        lastSampleAt = now

        // Update the smoothing window.
        recentLux.addLast(now to lux)
        val cutoff = now - SMOOTH_WINDOW_MS
        while (recentLux.isNotEmpty() && recentLux.first().first < cutoff) {
            recentLux.removeFirst()
        }

        val clusterId = matchCluster(_currentLocation.value, cachedPlaces)

        viewModelScope.launch {
            sampleDao.insert(
                LightSample(
                    ts = now,
                    clusterId = clusterId,
                    lux = lux,
                    source = SampleSource.FOREGROUND,
                )
            )
            refreshToday()
            if (clusterId != null) {
                evaluateDeviation(clusterId, now, lux)
            }
        }
    }

    private fun refreshToday() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            _todaySamples.value = sampleDao.getSamplesBetween(start, start + DAY_MS)
        }
    }

    private fun startLocation() {
        if (locationTracker != null) return
        val tracker = LocationTracker(getApplication()) { location ->
            _currentLocation.value = location
        }
        locationTracker = tracker
        tracker.start()
    }

    private fun stopLocation() {
        locationTracker?.stop()
        locationTracker = null
    }

    private fun matchCluster(location: Location?, clusters: List<PlaceCluster>): Long? {
        if (location == null) return null
        var bestId: Long? = null
        var bestDistance = Double.MAX_VALUE
        for (cluster in clusters) {
            val distance = Geo.haversineMeters(
                location.latitude,
                location.longitude,
                cluster.lat,
                cluster.lng,
            )
            if (distance <= cluster.radiusM && distance < bestDistance) {
                bestDistance = distance
                bestId = cluster.id
            }
        }
        return bestId
    }

    private suspend fun evaluateDeviation(clusterId: Long, now: Long, lux: Float) {
        val smoothed = Stats.median(recentLux.map { it.second })
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val hourBucket = calendar.get(Calendar.HOUR_OF_DAY)
        val dayType = DayType.from(calendar)

        val existing = baselineDao.get(clusterId, hourBucket, dayType)
        val state = existing
            ?.let { Stats.WelfordState(it.mean, it.m2, it.n) }
            ?: Stats.WelfordState(0f, 0f, 0)

        if (state.n < MIN_SAMPLES_FOR_ALERT) {
            // Not enough history: learn only, never alert.
            val updated = Stats.update(state, smoothed)
            baselineDao.upsert(toStat(clusterId, hourBucket, dayType, updated, now))
            return
        }

        val std = Stats.std(state)
        val deviation = abs(smoothed - state.mean)
        val direction =
            if (smoothed > state.mean) DetectionDirection.BRIGHTER else DetectionDirection.DIMMER

        val isDeviation = deviation > ALERT_K * std && deviation > MIN_ABS_LUX

        if (isDeviation) {
            val key = cooldownKey(clusterId, hourBucket, dayType, direction)
            val lastAlert = cooldowns[key]
            if (lastAlert == null || now - lastAlert > COOLDOWN_MS) {
                cooldowns[key] = now
                detectionDao.insert(
                    DetectionEvent(
                        ts = now,
                        clusterId = clusterId,
                        hourBucket = hourBucket,
                        observedLux = smoothed,
                        baselineMean = state.mean,
                        baselineStd = std,
                        direction = direction,
                    )
                )
                val clusterName = cachedPlaces.firstOrNull { it.id == clusterId }?.name ?: "Place"
                postNotification(clusterName, smoothed, state.mean)
            }
            // Deviant values are not folded into the baseline.
            return
        }

        val updated = Stats.update(state, smoothed)
        baselineDao.upsert(toStat(clusterId, hourBucket, dayType, updated, now))
    }

    private fun toStat(
        clusterId: Long,
        hourBucket: Int,
        dayType: DayType,
        state: Stats.WelfordState,
        now: Long,
    ) = BaselineStat(
        clusterId = clusterId,
        hourBucket = hourBucket,
        dayType = dayType,
        mean = state.mean,
        m2 = state.m2,
        n = state.n,
        updatedAt = now,
    )

    private fun cooldownKey(
        clusterId: Long,
        hourBucket: Int,
        dayType: DayType,
        direction: DetectionDirection,
    ) = "$clusterId|$hourBucket|${dayType.name}|${direction.name}"

    private fun postNotification(clusterName: String, observed: Float, mean: Float) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Light deviations",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }

        val text = String.format(
            Locale.US,
            "Unusual light at %s: %.0f lx vs usual ~%.0f lx",
            clusterName,
            observed,
            mean,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("CircadianGuard")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onCleared() {
        stopLocation()
        super.onCleared()
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 1_000L
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val SMOOTH_WINDOW_MS = 5 * 60 * 1000L
        private const val MIN_SAMPLES_FOR_ALERT = 20
        private const val ALERT_K = 3f
        private const val MIN_ABS_LUX = 50f
        private const val COOLDOWN_MS = 30 * 60 * 1000L
        private const val DEFAULT_RADIUS_M = 100
        private const val DETECTION_LIMIT = 50
        private const val CHANNEL_ID = "deviations"
        private const val NOTIFICATION_ID = 1
    }
}
