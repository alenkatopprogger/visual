package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnCalculator).setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        findViewById<Button>(R.id.btnMediaPlayer).setOnClickListener {
            startActivity(Intent(this, MediaPlayerActivity::class.java))
        }

        findViewById<Button>(R.id.btnLocation).setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        findViewById<Button>(R.id.btnNetwork).setOnClickListener {
            startActivity(Intent(this, NetworkActivity::class.java))
        }
    }
}