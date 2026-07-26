package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import kotlin.random.Random

object QueueNotificationAudioHelper {
    private var ttsEngine: TextToSpeech? = null
    private var isTtsInitialized = false
    private var lastWasMale = false

    fun initTts(context: Context) {
        if (ttsEngine == null) {
            ttsEngine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                }
            }
        }
    }

    private fun applyVoiceStyle(tts: TextToSpeech, isMaleVoice: Boolean) {
        try {
            tts.language = Locale.US
            tts.setPitch(1.0f)
            tts.setSpeechRate(0.90f)

            val availableVoices = tts.voices
            if (!availableVoices.isNullOrEmpty()) {
                val usMaleVoice = availableVoices.firstOrNull { voice ->
                    val lang = voice.locale.language
                    val country = voice.locale.country
                    val name = voice.name.lowercase(Locale.ROOT)
                    val isUs = lang == "en" && (country == "US" || country == "")
                    isUs && (name.contains("male") || name.contains("iom") || name.contains("sfg") || name.contains("olb") || name.contains("en-us") || name.contains("en_us"))
                } ?: availableVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    voice.locale.language == "en" && (name.contains("male") || name.contains("en-us") || name.contains("en_us"))
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale.language == "en"
                }

                if (usMaleVoice != null) {
                    tts.voice = usMaleVoice
                }
            }
        } catch (_: Exception) {
            tts.language = Locale.US
            tts.setPitch(1.0f)
            tts.setSpeechRate(0.90f)
        }
    }

    fun playReadyChimeAndAnnouncement(context: Context, bikeNumber: String) {
        // 1. Play chime sound and ensure high volume for TV speaker broadcast
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                try {
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val targetVol = (maxVol * 0.95).toInt().coerceAtLeast(1)
                    if (currentVol < targetVol) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    }
                } catch (_: Throwable) {
                    // Ignore volume modification errors if prohibited by AppOps / system policy
                }
            }

            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = false
                }
                ringtone.play()
            } else {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
            }
        } catch (_: Exception) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 350)
            } catch (_: Exception) {}
        }

        // 2. Speak voice announcement in male voice
        try {
            initTts(context)

            val isMaleVoice = true
            lastWasMale = true

            // Format bike number digit by digit, explicitly pronouncing '0' as "zero" with comma cadence
            val formattedBikeNumber = bikeNumber.map { char ->
                when (char) {
                    '0' -> "zero"
                    '1' -> "one"
                    '2' -> "two"
                    '3' -> "three"
                    '4' -> "four"
                    '5' -> "five"
                    '6' -> "six"
                    '7' -> "seven"
                    '8' -> "eight"
                    '9' -> "nine"
                    else -> char.toString()
                }
            }.joinToString(", ")

            // Natural speech script with realistic breath & cadence pauses
            val speechText = "Attention please. Bike number, $formattedBikeNumber, is ready. I repeat. Bike number, $formattedBikeNumber, is ready."

            // Maximum TTS volume bundle
            val ttsParams = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val engine = ttsEngine
                val utteranceId = "bike_ready_${bikeNumber}_${System.currentTimeMillis()}"
                if (engine != null && isTtsInitialized) {
                    applyVoiceStyle(engine, isMaleVoice)
                    engine.speak(speechText, TextToSpeech.QUEUE_FLUSH, ttsParams, utteranceId)
                } else {
                    ttsEngine = TextToSpeech(context.applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            ttsEngine?.let { tts ->
                                isTtsInitialized = true
                                applyVoiceStyle(tts, isMaleVoice)
                                tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, ttsParams, utteranceId)
                            }
                        }
                    }
                }
            }, 700)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        try {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
            ttsEngine = null
            isTtsInitialized = false
        } catch (_: Exception) {}
    }
}
