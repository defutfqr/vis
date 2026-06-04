package com.example.appandroid

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import android.widget.ToggleButton
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.android_notes.services.BackgroundService
import android.os.Build
import android.util.Log
class LocationActivity : AppCompatActivity() {

    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvAcc: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvBand: TextView
    private lateinit var tvMccMnc: TextView
    private lateinit var tvPci: TextView
    private lateinit var tvRsrp: TextView
    private lateinit var tvRssi: TextView
    private lateinit var toggleStart: ToggleButton
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val msg = intent.getStringExtra("Status")

        }
    }
    companion object {
        private const val REQ_CODE = 100
        private val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,

        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        toggleStart = findViewById(R.id.toggleButton)

        toggleStart.setOnCheckedChangeListener { _, isChecked ->
            checkMiuiPermissions()
            if (isChecked) {
                if (hasPermissions()) {
                    startService()
                } else {
                    toggleStart.isChecked = false
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_CODE)
                }
            } else {
                stopService()
            }
        }
    }

    private fun startService() {
        Intent(this, BackgroundService::class.java).apply {
            putExtra("action", "START")
            startService(this)
        }

    }

    private fun stopService() {
        Intent(this, BackgroundService::class.java).apply {
            putExtra("action", "STOP")
            startService(this)
        }

    }

    private fun hasPermissions() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkMiuiPermissions() {
        val brand = Build.BRAND.lowercase()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            toggleStart.isChecked = true
            startService()
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter("BackGroundUpdate")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}