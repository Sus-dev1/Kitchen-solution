package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

enum class CookingVoiceCommand {
  NEXT_STEP,
  PREV_STEP,
  TOGGLE_TIMER,
  RESET_TIMER,
  REPLAY_VOICE,
}

fun parseCookingVoiceCommand(spokenText: String): CookingVoiceCommand? {
  val text = spokenText.lowercase().trim()
  if (text.isBlank()) return null
  return when {
    text.contains("ulang timer") || text.contains("reset") -> CookingVoiceCommand.RESET_TIMER
    text.contains("timer") ||
      text.contains("mulai") ||
      text.contains("start") ||
      text.contains("jeda") ||
      text.contains("pause") -> CookingVoiceCommand.TOGGLE_TIMER
    text.contains("kembali") || text.contains("sebelum") || text.contains("mundur") ->
      CookingVoiceCommand.PREV_STEP
    text.contains("ulangi") || text.contains("baca") || text.contains("apa tadi") ->
      CookingVoiceCommand.REPLAY_VOICE
    text.contains("lanjut") ||
      text.contains("berikut") ||
      text.contains("selanjut") ||
      text.contains("next") ||
      text.contains("selesai") -> CookingVoiceCommand.NEXT_STEP
    else -> null
  }
}

fun hasAudioPermission(context: Context): Boolean {
  return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
    PackageManager.PERMISSION_GRANTED
}

class CookingSpeechCommandController(
  private val context: Context,
  private val onCommand: (CookingVoiceCommand, String) -> Unit,
  private val onListeningStateChanged: (Boolean) -> Unit,
) {
  private var recognizer: SpeechRecognizer? = null
  private val handler = Handler(Looper.getMainLooper())
  private var isEnabled = false
  private var isPausedForTts = false

  fun setEnabled(enabled: Boolean) {
    isEnabled = enabled
    if (enabled && !isPausedForTts) {
      startListeningInternal()
    } else if (!enabled) {
      stopInternal()
    }
  }

  fun setPausedForTts(paused: Boolean) {
    isPausedForTts = paused
    if (paused) {
      handler.removeCallbacksAndMessages(null)
      try {
        recognizer?.stopListening()
      } catch (_: Exception) {}
      onListeningStateChanged(false)
    } else if (isEnabled) {
      scheduleRestart(450L)
    }
  }

  private fun startListeningInternal() {
    if (!isEnabled || isPausedForTts || !hasAudioPermission(context)) return
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return

    try {
      if (recognizer == null) {
        recognizer =
          SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(
              object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                  onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                  onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                  onListeningStateChanged(false)
                  if (isEnabled && !isPausedForTts) {
                    val delay =
                      if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 900L else 600L
                    scheduleRestart(delay)
                  }
                }

                override fun onResults(results: Bundle?) {
                  onListeningStateChanged(false)
                  val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                  val recognized = matches.firstOrNull().orEmpty()
                  val command = matches.firstNotNullOfOrNull { parseCookingVoiceCommand(it) }
                  if (command != null) {
                    onCommand(command, recognized)
                  }
                  if (isEnabled && !isPausedForTts) {
                    scheduleRestart(650L)
                  }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
              }
            )
          }
      }

      val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
      recognizer?.startListening(intent)
    } catch (_: Exception) {
      onListeningStateChanged(false)
    }
  }

  private fun scheduleRestart(delayMs: Long) {
    handler.removeCallbacksAndMessages(null)
    handler.postDelayed(
      {
        if (isEnabled && !isPausedForTts) {
          startListeningInternal()
        }
      },
      delayMs,
    )
  }

  private fun stopInternal() {
    handler.removeCallbacksAndMessages(null)
    try {
      recognizer?.stopListening()
      recognizer?.cancel()
    } catch (_: Exception) {}
    onListeningStateChanged(false)
  }

  fun destroy() {
    isEnabled = false
    stopInternal()
    try {
      recognizer?.destroy()
    } catch (_: Exception) {}
    recognizer = null
  }
}
