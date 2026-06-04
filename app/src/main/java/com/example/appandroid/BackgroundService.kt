package com.example.android_notes.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.telephony.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment

class BackgroundService : Service(), LocationListener {

    companion object {
        private const val SERVER_ADDRESS = "tcp://192.168.0.12:5556"
        private const val FILE_NAME = "tracking_data.jsonl"
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private var zmqContext: ZContext? = null
    private var isRunning = false

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        zmqContext = ZContext()
        ensureDataFileExists()
    }

    private fun ensureDataFileExists() {
        val file = getDataFile()
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
    }

    private fun getDataFile(): File {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME
            )
        } else {
            File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.getStringExtra("action")) {
            "START" -> {
                startForegroundService()
                startBackgroundWork()
            }
            "STOP" -> stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Отслеживание активно")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun startBackgroundWork() {
        if (isRunning) return

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            sendPermissionRequest("location")
            return
        }

        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            sendPermissionRequest("phone")
        }

        isRunning = true
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                1f,
                this
            )
        }
    }

    private fun sendPermissionRequest(type: String) {
        val intent = Intent("PERMISSION_REQUEST").apply {
            putExtra("type", type)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onLocationChanged(location: Location) {
        serviceScope.launch {
            val cellData = getCellularData()
            saveToFile(location, cellData)
            sendToServer(location, cellData)
            sendMessageToActivity("${location.latitude}, ${location.longitude}")
        }
    }

    private fun saveToFile(location: Location, cellData: JSONObject) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(location.time))

        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("altitude", location.altitude)
            put("accuracy", location.accuracy)
            put("time", timeStr)
            put("networkType", cellData.optString("type", "GPS"))
            put("cell_data", cellData)
            put("saved_at", System.currentTimeMillis())
        }.toString()

        getDataFile().appendText(json + "\n")
    }

    private fun getCellularData(): JSONObject {
        val result = JSONObject()

        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) return result

        val cellInfoList = telephonyManager.allCellInfo
        if (cellInfoList.isNullOrEmpty()) return result

        val cellInfo = cellInfoList.firstOrNull { it.isRegistered } ?: return result

        when (cellInfo) {
            is CellInfoLte -> {
                val id = cellInfo.cellIdentity
                val sig = cellInfo.cellSignalStrength
                result.put("type", "LTE")
                result.put("identity", JSONObject().apply {
                    put("mcc", id.mccString ?: "N/A")
                    put("mnc", id.mncString ?: "N/A")
                    put("pci", id.pci)
                    put("earfcn", id.earfcn)
                })
                result.put("signal", JSONObject().apply {
                    put("rsrp", sig.rsrp)
                    put("rsrq", sig.rsrq)
                    put("rssi", sig.rssi)
                    put("asuLevel", sig.asuLevel)
                })
            }
            is CellInfoGsm -> {
                result.put("type", "GSM")
                result.put("identity", JSONObject().apply {
                    put("mcc", cellInfo.cellIdentity.mccString ?: "N/A")
                    put("mnc", cellInfo.cellIdentity.mncString ?: "N/A")
                })
            }
            is CellInfoNr -> {
                val id = cellInfo.cellIdentity as CellIdentityNr
                val sig = cellInfo.cellSignalStrength
                result.put("type", "5G-NR")
                result.put("identity", JSONObject().apply {
                    put("mcc", id.mccString ?: "N/A")
                    put("mnc", id.mncString ?: "N/A")
                })
            }
        }
        return result
    }

    private fun sendToServer(location: Location, cellData: JSONObject) {
        serviceScope.launch {
            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(location.time))

            val json = JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", location.altitude)
                put("accuracy", location.accuracy)
                put("time", timeStr)
                put("networkType", cellData.optString("type", "GPS"))
                put("cell_data", cellData)
            }.toString()

            zmqContext?.let { ctx ->
                val socket = ctx.createSocket(SocketType.REQ).apply {
                    connect(SERVER_ADDRESS)
                    sendTimeOut = 2000
                    receiveTimeOut = 2000
                    linger = 0
                }
                socket.send(json.toByteArray(ZMQ.CHARSET), 0)
                socket.recv(0)
                socket.close()
            }
        }
    }

    private fun sendMessageToActivity(msg: String?) {
        val intent = Intent("BackGroundUpdate").apply {
            putExtra("Status", msg)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        locationManager.removeUpdates(this)
        serviceJob.cancel()
        zmqContext?.close()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}