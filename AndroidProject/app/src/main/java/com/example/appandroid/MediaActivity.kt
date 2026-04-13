package com.example.appandroid

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
import java.io.IOException

class MediaActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarVolume: SeekBar
    private lateinit var tracksContainer: LinearLayout
    private lateinit var tvCurrent: TextView
    private var currentFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private lateinit var audioManager: AudioManager
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        seekBar = findViewById(R.id.seekBar)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        tracksContainer = findViewById(R.id.tracksContainer)
        tvCurrent = findViewById(R.id.tvCurrent)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupVolumeControl()
        setupButtons()
        setupSeekBar()

        requestPermission()
    }

    private fun setupVolumeControl() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        seekBarVolume.max = maxVolume
        seekBarVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                100
            )
        } else {
            loadTracks()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadTracks()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTracks() {
        val musicDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MUSIC
        )

        tracksContainer.removeAllViews()

        if (!musicDir.exists() || !musicDir.canRead()) {
            val errorText = TextView(this).apply {
                text = "Cannot access music directory\nPlease check storage permission"
                setPadding(16, 16, 16, 16)
                textSize = 14f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(ContextCompat.getColor(this@MediaActivity, android.R.color.darker_gray))
            }
            tracksContainer.addView(errorText)
            return
        }

        val audioFiles = musicDir.listFiles()

        if (audioFiles.isNullOrEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No audio files found\nPlace music files in:\n${musicDir.absolutePath}"
                setPadding(16, 16, 16, 16)
                textSize = 14f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(ContextCompat.getColor(this@MediaActivity, android.R.color.darker_gray))
            }
            tracksContainer.addView(emptyText)
            return
        }

        for (file in audioFiles) {
            if (file.isDirectory) continue

            val extension = file.extension.lowercase()
            if (extension in listOf("mp3", "wav", "m4a", "flac", "aac", "ogg")) {
                val trackItem = createTrackItem(file)
                tracksContainer.addView(trackItem)
            }
        }

        if (tracksContainer.childCount == 0) {
            val noAudioText = TextView(this).apply {
                text = "No supported audio files found\nSupported formats: MP3, WAV, M4A, FLAC, AAC, OGG"
                setPadding(16, 16, 16, 16)
                textSize = 14f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(ContextCompat.getColor(this@MediaActivity, android.R.color.darker_gray))
            }
            tracksContainer.addView(noAudioText)
        }
    }

    private fun createTrackItem(file: File): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 14, 16, 14)
            setBackgroundResource(android.R.drawable.list_selector_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = TextView(this).apply {
            text = "🎵"
            textSize = 28f
            setPadding(0, 0, 16, 0)
        }

        val name = TextView(this).apply {
            text = file.nameWithoutExtension
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MediaActivity, android.R.color.black))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val duration = TextView(this).apply {
            text = getAudioDuration(file)
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MediaActivity, android.R.color.darker_gray))
            setPadding(8, 0, 0, 0)
        }

        container.addView(icon)
        container.addView(name)
        container.addView(duration)

        container.setOnClickListener {
            currentFile = file
            tvCurrent.text = "🎵 ${file.nameWithoutExtension}"
            playTrack()
        }

        return container
    }

    private fun getAudioDuration(file: File): String {
        return try {
            val mp = MediaPlayer()
            try {
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                val duration = mp.duration / 1000
                val minutes = duration / 60
                val seconds = duration % 60
                String.format("%d:%02d", minutes, seconds)
            } finally {
                mp.release()
            }
        } catch (e: Exception) {
            "--:--"
        }
    }

    private fun playTrack() {
        try {
            val file = currentFile ?: return

            releaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    this@MediaActivity.isPlaying = false  // ← Явно указываем внешний класс
                    this@MediaActivity.stopSeekBarUpdate()
                    this@MediaActivity.seekBar.progress = 0
                    Toast.makeText(this@MediaActivity, "Playback completed", Toast.LENGTH_SHORT).show()
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(this@MediaActivity, "Error playing file", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            isPlaying = true
            seekBar.max = mediaPlayer?.duration ?: 0
            startSeekBarUpdate()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Cannot play this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releaseMediaPlayer() {
        stopSeekBarUpdate()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    private fun startSeekBarUpdate() {
        stopSeekBarUpdate()
        updateRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        seekBar.progress = mp.currentPosition
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopSeekBarUpdate() {
        updateRunnable?.let {
            handler.removeCallbacks(it)
        }
        updateRunnable = null
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnPause = findViewById<Button>(R.id.btnPause)

        btnPlay.setOnClickListener {
            mediaPlayer?.let { mp ->
                if (!mp.isPlaying && currentFile != null) {
                    mp.start()
                    isPlaying = true
                    startSeekBarUpdate()
                    Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show()
                } else if (currentFile == null) {
                    Toast.makeText(this, "Select a track first", Toast.LENGTH_SHORT).show()
                } else if (mp.isPlaying) {
                    Toast.makeText(this, "Already playing", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnPause.setOnClickListener {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    isPlaying = false
                    stopSeekBarUpdate()
                    Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Nothing is playing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            mediaPlayer?.pause()
            stopSeekBarUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying && mediaPlayer != null) {
            mediaPlayer?.start()
            startSeekBarUpdate()
        }
    }
}