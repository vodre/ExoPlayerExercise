package com.example.exoplayerexercise

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.exoplayerexercise.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource


const val VIDEO_URL =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var sensorManager: SensorManager? = null
    private var totalSteps = 0
    private var mAccel = 0f
    private var mAcc: Sensor? = null
    private var mGyro: Sensor? = null
    private var mStep: Sensor? = null
    private var prevXType: MeasureType = MeasureType.Positive
    private var prevZType: MeasureType = MeasureType.Positive
    private lateinit var audioManager: AudioManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAcc = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        mAcc = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        player!!.playWhenReady = true
        player!!.setMediaSource(buildMediaSource())
        player!!.prepare()
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= 24) {
            initPlayer()
        }
    }

    override fun onResume() {
        sensorManager?.registerListener(this, mAcc,
            SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager?.registerListener(this, mGyro,
            SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager?.registerListener(this, mStep,
            SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume()
        if (Build.VERSION.SDK_INT < 24 || player == null) {
            initPlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < 24) {
            releasePlayer()
        }
        sensorManager?.unregisterListener(this);
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= 24) {
            releasePlayer()
        }
    }


    private fun releasePlayer() {
        player?.release()
        player = null
    }

    //creating mediaSource
    private fun buildMediaSource(): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(VIDEO_URL))
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                totalSteps = event.values?.get(0)?.toInt() ?: 0
                if (totalSteps > 10) {
                    player?.seekTo(0)
                    player?.play()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                totalSteps = event.values?.get(0)?.toInt() ?: 0
                if (totalSteps > 8) {
                    player?.pause()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                with(x.toMeasureType()) {
                    if (this != prevXType) {
                        when (this) {
                            MeasureType.Positive -> {
                                audioManager.adjustVolume(
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_PLAY_SOUND
                                )
                            }
                            MeasureType.Negative -> {
                                audioManager.adjustVolume(
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_PLAY_SOUND
                                )
                            }
                        }
                        prevXType = this
                    }
                }

                with(z.toMeasureType()) {
                    if (this != prevZType) {
                        when (this) {
                            MeasureType.Positive -> player?.seekForward()
                            MeasureType.Negative -> player?.seekBack()
                        }
                        prevZType = this
                    }
                }
            }
            else -> Unit
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // No action
    }
}

fun Float.toMeasureType(): MeasureType = when {
        this >= 0 -> MeasureType.Positive
        this < 0 -> MeasureType.Negative
        else -> MeasureType.Positive
    }

sealed interface MeasureType {
    object Negative : MeasureType
    object Positive : MeasureType
}