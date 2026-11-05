package com.atlas.locationchanger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class LocationSpoofService : Service() {
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var latitude = 55.7558
    private var longitude = 37.6173
    private var cityName = "Москва"
    private var providersAdded = false

    private val updateTask = object : Runnable {
        override fun run() {
            try {
                publishLocation(LocationManager.GPS_PROVIDER)
                publishLocation(LocationManager.NETWORK_PROVIDER)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, true).apply()
                handler.postDelayed(this, 2000L)
            } catch (e: Exception) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(KEY_ACTIVE, false)
                    .putString(KEY_ERROR, e.message ?: e.javaClass.simpleName)
                    .apply()
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        latitude = intent?.getDoubleExtra(EXTRA_LAT, 55.7558) ?: 55.7558
        longitude = intent?.getDoubleExtra(EXTRA_LON, 37.6173) ?: 37.6173
        cityName = intent?.getStringExtra(EXTRA_CITY) ?: "Выбранная точка"
        startForeground(NOTIFICATION_ID, buildNotification("Активна: $cityName"))

        try {
            addMockProvider(LocationManager.GPS_PROVIDER)
            addMockProvider(LocationManager.NETWORK_PROVIDER)
            providersAdded = true
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ERROR, "").apply()
            handler.removeCallbacks(updateTask)
            handler.post(updateTask)
        } catch (e: Exception) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putString(KEY_ERROR, e.message ?: "Не удалось включить подмену")
                .apply()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun addMockProvider(provider: String) {
        try {
            locationManager.removeTestProvider(provider)
        } catch (_: Exception) { }
        locationManager.addTestProvider(
            provider,
            false, false, false, false,
            true, true, true,
            Criteria.POWER_LOW,
            Criteria.ACCURACY_FINE
        )
        locationManager.setTestProviderEnabled(provider, true)
    }

    private fun publishLocation(provider: String) {
        val location = Location(provider).apply {
            latitude = this@LocationSpoofService.latitude
            longitude = this@LocationSpoofService.longitude
            accuracy = 5f
            altitude = 0.0
            speed = 0f
            bearing = 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        @Suppress("DEPRECATION")
        locationManager.setTestProviderLocation(provider, location)
    }

    private fun buildNotification(message: String): Notification {
        val stopIntent = Intent(this, LocationSpoofService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("MLBB Location Changer")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Остановить", stopPending).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Подмена местоположения", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        handler.removeCallbacks(updateTask)
        if (providersAdded) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                try { locationManager.removeTestProvider(provider) } catch (_: Exception) { }
            }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.atlas.locationchanger.STOP"
        const val EXTRA_LAT = "latitude"
        const val EXTRA_LON = "longitude"
        const val EXTRA_CITY = "city"
        const val PREFS = "location_changer"
        const val KEY_ACTIVE = "active"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "location_spoof_channel"
        private const val NOTIFICATION_ID = 701
    }
}
