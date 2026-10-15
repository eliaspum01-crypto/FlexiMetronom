package de.fleximetronom.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MetronomeService : Service() {
    companion object {
        const val ACTION_START = "de.fleximetronom.app.START"
        const val ACTION_STOP = "de.fleximetronom.app.STOP"
        const val ACTION_TOGGLE = "de.fleximetronom.app.TOGGLE"
        const val EXTRA_NUM = "num"
        const val EXTRA_DEN = "den"
        const val EXTRA_BPM = "bpm"
        const val EXTRA_UNIT = "unit"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_GROUPS = "groups"
        const val EXTRA_ACCENTS = "accents"
        private const val CHANNEL_ID = "flexi_metronome"
        private const val NOTIFICATION_ID = 42
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var beatJob: Job? = null
    private var tone: ToneGenerator? = null
    private var playing = false
    private var bpm = 100
    private var numerator = 4
    private var denominator = 4
    private var beatUnit = 4
    private var sound = "Click"
    private var groupsText = ""
    private var accentsText = ""
    private var pulse = 0

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tone = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMetronome()
            ACTION_TOGGLE -> if (playing) stopMetronome() else startMetronome(intent)
            ACTION_START -> startMetronome(intent)
        }
        return START_STICKY
    }

    private fun startMetronome(intent: Intent?) {
        intent?.let {
            numerator = it.getIntExtra(EXTRA_NUM, numerator).coerceIn(1, 64)
            denominator = it.getIntExtra(EXTRA_DEN, denominator).coerceIn(1, 32)
            bpm = it.getIntExtra(EXTRA_BPM, bpm).coerceIn(20, 300)
            beatUnit = it.getIntExtra(EXTRA_UNIT, beatUnit)
            sound = it.getStringExtra(EXTRA_SOUND) ?: sound
            groupsText = it.getStringExtra(EXTRA_GROUPS) ?: groupsText
            accentsText = it.getStringExtra(EXTRA_ACCENTS) ?: accentsText
        }
        beatJob?.cancel()
        playing = true
        pulse = 0
        startForeground(NOTIFICATION_ID, makeNotification(true))
        val pulsesPerBar = (numerator.toDouble() * beatUnit / denominator).toInt().coerceAtLeast(1)
        val intervalMs = (60_000.0 / bpm).toLong().coerceAtLeast(20L)
        val accents = accentsText.split(",", " ", ";").mapNotNull { it.trim().toIntOrNull() }.toSet()
        val groups = groupsText.split("+", "-", ",", " ").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
        val groupStarts = mutableSetOf<Int>()
        var total = 0
        groups.dropLast(1).forEach { total += it; groupStarts.add(total) }

        beatJob = serviceScope.launch {
            while (isActive && playing) {
                val strong = pulse == 0 || accents.contains(pulse + 1) || groupStarts.contains(pulse)
                val toneType = when {
                    strong -> ToneGenerator.TONE_PROP_ACK
                    sound == "Woodblock" -> ToneGenerator.TONE_PROP_BEEP2
                    sound == "Bell" -> ToneGenerator.TONE_PROP_BEEP
                    else -> ToneGenerator.TONE_PROP_NACK
                }
                tone?.startTone(toneType, if (strong) 75 else 45)
                pulse = (pulse + 1) % pulsesPerBar
                delay(intervalMs)
            }
        }
        updateNotification()
    }

    private fun stopMetronome() {
        playing = false
        beatJob?.cancel()
        beatJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun makeNotification(isPlaying: Boolean): Notification {
        val stopIntent = Intent(this, MetronomeService::class.java).setAction(ACTION_STOP)
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        val stopPending = PendingIntent.getService(this, 2, stopIntent, pendingFlags)
        val openPending = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), pendingFlags
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("FlexiMetronom · ${numerator}/${denominator}")
            .setContentText(if (isPlaying) "$bpm BPM · läuft im Hintergrund" else "Pausiert")
            .setContentIntent(openPending)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_pause, "Stoppen", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, makeNotification(playing))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "FlexiMetronom", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Steuerung des Metronoms im Hintergrund"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        playing = false
        beatJob?.cancel()
        serviceScope.cancel()
        tone?.release()
        tone = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
