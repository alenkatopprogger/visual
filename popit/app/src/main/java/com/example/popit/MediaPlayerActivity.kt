package com.example.popit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarVolume: SeekBar
    private lateinit var tracksContainer: LinearLayout
    private lateinit var tvCurrent: TextView
    private var currentFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        seekBar = findViewById(R.id.seekBar)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        tracksContainer = findViewById(R.id.tracksContainer)
        tvCurrent = findViewById(R.id.tvCurrent)

        mediaPlayer = MediaPlayer()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupVolumeControl()
        setupButtons()
        setupSeekBar()

        requestPermission()
    }

    private fun setupVolumeControl() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        seekBarVolume.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                if (f) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 100)
        } else {
            loadTracks()
        }
    }

    private fun loadTracks() {
        val audioFiles =  File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ), ""
        )

        for (file in audioFiles.listFiles()) {
            if (file.isDirectory) continue
            val textView = TextView(this).apply {
                text = file.name
                setPadding(16, 16, 16, 16)
                textSize = 16f
            }
            textView.setOnClickListener {
                currentFile = file
                tvCurrent.text = "Играет сейчас: ${file.name}"
                playTrack()
            }
            tracksContainer.addView(textView)
        }
    }
    private fun playTrack() {
        val file = currentFile ?: return
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
        isPlaying = true
        seekBar.max = mediaPlayer.duration
        updateSeekBar()
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer.seekTo(p)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun updateSeekBar() {
        seekBar.progress = mediaPlayer.currentPosition
        if (mediaPlayer.isPlaying) {
            handler.postDelayed({ updateSeekBar() }, 1000)
        }
    }
    private fun setupButtons() {
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            if (!isPlaying) {
                mediaPlayer.start()
                isPlaying = true
                updateSeekBar()
            }
        }

        findViewById<Button>(R.id.btnPause).setOnClickListener {
            if (isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            }
        }
    }
}