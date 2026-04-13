package com.example.appandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val calcButton = findViewById<Button>(R.id.calcButton)
        calcButton.setOnClickListener {
            val intent = Intent(this, CalcActivity::class.java)
            startActivity(intent)
        }
        val mediaButton = findViewById<Button>(R.id.playerButton)
        mediaButton.setOnClickListener {
            val intent = Intent(this, MediaActivity::class.java)
            startActivity(intent)
        }
        val locationButton = findViewById<Button>(R.id.locationButton)
        locationButton.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }

        val clientButton = findViewById<Button>(R.id.clientButton)
        clientButton.setOnClickListener {
            val intent = Intent(this, ClientActivity::class.java)
            startActivity(intent)

        }
    }
}