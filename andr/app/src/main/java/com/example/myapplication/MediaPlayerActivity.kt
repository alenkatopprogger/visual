package com.example.myapplication

import android.Manifest
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
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var playPauseButton: Button
    private lateinit var trackListView: ListView
    private lateinit var audioManager: AudioManager
    private val tracks = mutableListOf<File>()
    private var currentTrackIndex = -1
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                seekBar.progress = mediaPlayer.currentPosition
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        seekBar = findViewById(R.id.seekBar)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        playPauseButton = findViewById(R.id.playPauseButton)
        trackListView = findViewById(R.id.trackListView)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 1)
        } else {
            loadTracks()
        }

        setupVolumeControl()
        setupSeekBar()
        setupButtons()
    }

    private fun loadTracks() {
        val musicDir = File("/storage/emulated/0/Music")
        if (musicDir.exists() && musicDir.isDirectory) {
            tracks.addAll(musicDir.listFiles { file -> file.isFile && file.name.endsWith(".mp3") } ?: emptyArray())
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tracks.map { it.name })
        trackListView.adapter = adapter
        trackListView.setOnItemClickListener { _, _, position, _ ->
            playTrack(position)
        }
    }

    private fun playTrack(index: Int) {
        if (index in tracks.indices) {
            currentTrackIndex = index
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.release()
            }
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tracks[index].absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            seekBar.max = mediaPlayer.duration
            playPauseButton.text = "Pause"
            handler.post(updateSeekBar)
        }
    }

    private fun setupVolumeControl() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.max = maxVolume
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && ::mediaPlayer.isInitialized) {
                    mediaPlayer.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        playPauseButton.setOnClickListener {
            if (::mediaPlayer.isInitialized) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    playPauseButton.text = "Play"
                } else {
                    mediaPlayer.start()
                    playPauseButton.text = "Pause"
                    handler.post(updateSeekBar)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            playPauseButton.text = "Play"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadTracks()
        }
    }
}