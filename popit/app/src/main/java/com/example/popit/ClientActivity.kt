package com.example.popit

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.ZMQ


class ClientActivity : AppCompatActivity() {

    private lateinit var tvResponse: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        tvResponse = findViewById<TextView>(R.id.tvResponse)
        findViewById<Button>(R.id.btnSend).setOnClickListener {
            Thread {
                val context = ZMQ.context(1)
                val socket = context.socket(ZMQ.REQ)
                socket.connect("tcp://192.168.0.14:8888")
                socket.send("Hello from Android!", 0)
                val reply = socket.recvStr(0)
                handler.post {
                    tvResponse.text = reply
                }
                socket.close()
                context.term()
            }.start()
        }
    }
}
