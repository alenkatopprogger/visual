package com.example.popit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.popit.CalculatorActivity
import com.example.popit.LocationActivity
import com.example.popit.MediaPlayerActivity
import com.example.popit.TelephonyActivity
import com.example.popit.ZMQActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup button listeners for navigation
        findViewById<Button>(R.id.btnGoToCalculator).setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToPlayer).setOnClickListener {
            startActivity(Intent(this, MediaPlayerActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToLocation).setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToTelephony).setOnClickListener {
            startActivity(Intent(this, TelephonyActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToZMQ).setOnClickListener {
            startActivity(Intent(this, ZMQActivity::class.java))
        }
    }
}