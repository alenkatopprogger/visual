package com.example.popit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class LocationActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var tvResult: TextView
    private lateinit var tvFile: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        tvResult = findViewById(R.id.tvResult)
        tvFile = findViewById(R.id.tvFile)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            }
        }
    }
    private fun getLocation() {
        val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (loc != null) {
            showLocation(loc)
            saveLocation(loc)
        } else {
            tvResult.text = "Location not available"
        }
    }

    private fun showLocation(location: Location) {
        val date = java.util.Date(location.time)
        tvResult.text =
            "Lat: ${"%.6f".format(location.latitude)}\n" +
                    "Lon: ${"%.6f".format(location.longitude)}\n" +
                    "Alt: ${"%.1f".format(location.altitude)} m\n" +
                    "Time: ${date.toString()}"
    }

    private fun saveLocation(location: Location) {
        val json = """{"lat":${location.latitude},"lon":${location.longitude},"alt":${location.altitude},"time":${location.time}}"""
        File(filesDir, "log.txt").appendText(json + "\n")
    }
}