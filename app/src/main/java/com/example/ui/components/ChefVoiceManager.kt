package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class ChefVoicePreset(
  val title: String,
  val subtitle: String,
  val pitch: Float,
  val speechRate: Float,
  val sampleText: String,
  val badge: String,
) {
  CHEF_RAMAH(
    title = "Chef Ramah (Natural)",
    subtitle = "Hangat, ekspresif dengan jeda napas alami di samping kompor",
    pitch = 1.03f,
    speechRate = 0.92f,
    sampleText =
      "Oke, langkah pertama ya, panaskan 2 sendok makan minyak, terus tumis bawang putih sampai harum, nah, jaga apinya sedang aja biar nggak cepat gosong ya.",
    badge = "Rekomendasi",
  ),
  ASISTEN_TENANG(
    title = "Asisten Tenang (Lembut)",
    subtitle = "Tempo santai & jeda koma lebih panjang saat fokus mengiris",
    pitch = 0.95f,
    speechRate = 0.85f,
    sampleText =
      "Nah, langkah kedua ya, iris tipis bawang merah pelan-pelan, terus sisihkan dengan rapi, hati-hati jarinya pas megang pisau ya.",
    badge = "Jelas & Santai",
  ),
  CHEF_ENERGIK(
    title = "Chef Energik (Cepat)",
    subtitle = "Intonasi ceria naik-turun & ritme sigap untuk masak praktis",
    pitch = 1.10f,
    speechRate = 1.02f,
    sampleText =
      "Oke, kita lanjut nih, kocok lepas 2 butir telur, terus langsung tuang ke wajan hangat, nah, jangan lupa diaduk rata biar lembut ya.",
    badge = "Ceria",
  ),
  PEMANDU_TEGAS(
    title = "Pemandu Tegas (Bariton)",
    subtitle = "Nada lebih dalam & mantap ala rekan chef profesional",
    pitch = 0.82f,
    speechRate = 0.93f,
    sampleText =
      "Nah, perhatikan langkah ini ya, rebus kaldu dengan api kecil, terus tutup pancinya sebentar, ingat, jangan ditinggal biar nggak meluap ya.",
    badge = "Profesional",
  ),
}

data class SpeechSegment(
  val text: String,
  val pitchDelta: Float = 0f,
  val rateDelta: Float = 0f,
  val pauseAfterMs: Long = 220L,
)

private const val VOICE_PREFS_NAME = "kitchen_voice_prefs"
private const val KEY_VOICE_PRESET = "selected_voice_preset"

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
private val speechSessionToken = AtomicLong(0L)

fun loadSavedVoicePreset(context: Context): ChefVoicePreset {
  val saved =
    context.applicationContext
      .getSharedPreferences(VOICE_PREFS_NAME, Context.MODE_PRIVATE)
      .getString(KEY_VOICE_PRESET, null)
  return ChefVoicePreset.entries.firstOrNull { it.name == saved } ?: ChefVoicePreset.CHEF_RAMAH
}

fun saveVoicePreset(context: Context, preset: ChefVoicePreset) {
  context.applicationContext
    .getSharedPreferences(VOICE_PREFS_NAME, Context.MODE_PRIVATE)
    .edit()
    .putString(KEY_VOICE_PRESET, preset.name)
    .apply()
}

fun applyVoicePresetToTts(tts: TextToSpeech, preset: ChefVoicePreset): String? {
  val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
  tts.language = idLocale
  tts.setPitch(preset.pitch)
  tts.setSpeechRate(preset.speechRate)

  val voices =
    try {
      tts.voices?.toList().orEmpty()
    } catch (_: Exception) {
      emptyList()
    }

  val indonesianVoices =
    voices
      .filter { v ->
        val lang = v.locale.language.lowercase()
        (lang == "id" || lang == "in") && !v.isNetworkConnectionRequired
      }
      .sortedWith(
        compareByDescending<Voice> { it.quality }
          .thenBy { it.latency }
          .thenBy { it.name }
      )

  val pool =
    if (indonesianVoices.isNotEmpty()) indonesianVoices
    else {
      voices
        .filter { v ->
          val lang = v.locale.language.lowercase()
          lang == "id" || lang == "in"
        }
        .sortedByDescending { it.quality }
    }

  if (pool.isNotEmpty()) {
    val chosen =
      when (preset) {
        ChefVoicePreset.CHEF_RAMAH -> pool.first()
        ChefVoicePreset.ASISTEN_TENANG -> pool.getOrElse(1) { pool.first() }
        ChefVoicePreset.CHEF_ENERGIK -> pool.getOrElse(2) { pool.first() }
        ChefVoicePreset.PEMANDU_TEGAS ->
          pool.firstOrNull {
            it.name.contains("male", ignoreCase = true) ||
              it.name.contains("idd", ignoreCase = true) ||
              it.name.contains("ide", ignoreCase = true)
          } ?: pool.last()
      }
    try {
      tts.voice = chosen
      return chosen.name
    } catch (_: Exception) {
      // Fallback to default locale voice
    }
  }
  return null
}

fun expandCulinaryTermsForSpeech(rawText: String): String {
  return rawText
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*sdm\\b"), "$1 sendok makan")
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*sdt\\b"), "$1 sendok teh")
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*pcs\\b"), "$1 butir")
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*ml\\b"), "$1 mililiter")
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*kg\\b"), "$1 kilogram")
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*(g|gr)\\b"), "$1 gram")
    .replace(Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*cm\\b"), "$1 sentimeter")
    .replace("°C", " derajat celcius")
    .replace("/", " atau ")
    .replace(Regex("\\s+"), " ")
    .trim()
}

private fun buildStoveSideReminder(stepIndex: Int, rawInstruction: String): String {
  val lower = rawInstruction.lowercase()
  return when (detectCookingAction(rawInstruction)) {
    CookingActionType.CHOPPING ->
      if (stepIndex % 2 == 0) {
        "hati-hati ya, jaga jarinya pas mengiris biar ukurannya tetap rata,"
      } else {
        "potongnya santai aja ya, yang penting rapi dan aman,"
      }
    CookingActionType.FRYING ->
      when {
        lower.contains("api besar") ->
          "nah, perhatiin minyaknya ya, jangan sampai terlalu berasap atau gosong,"
        stepIndex % 2 == 0 ->
          "jaga apinya sedang aja ya, biar matangnya pas dan nggak cepat gosong,"
        else ->
          "sambil dibolak-balik pelan ya, supaya warnanya cantik merata,"
      }
    CookingActionType.BOILING ->
      if (stepIndex % 2 == 0) {
        "sesekali dicek pancinya ya, jangan sampai kuahnya meluap,"
      } else {
        "ingat ya, jaga panasnya stabil biar bumbunya meresap sempurna,"
      }
    CookingActionType.MIXING ->
      if (stepIndex % 2 == 0) {
        "aduknya pelan sampai tercampur rata ya, biar nggak ada yang menggumpal,"
      } else {
        "pastikan semua bagian kebagian bumbu ya, biar rasanya mantap,"
      }
    CookingActionType.BAKING ->
      "hati-hati panas ya, jangan lupa dicek sesekali supaya permukaannya nggak kering,"
    CookingActionType.SERVING ->
      "nah, sajikan selagi hangat ya, biar aromanya masih segar,"
  }
}

fun buildHumanoidCookingSegments(
  stepIndex: Int,
  totalSteps: Int,
  rawInstruction: String,
): List<SpeechSegment> {
  val introText =
    when {
      stepIndex == 0 -> "Oke, kita mulai dari langkah pertama ya,"
      stepIndex == totalSteps - 1 -> "Nah, ini langkah terakhir ya,"
      stepIndex % 3 == 1 -> "Nah, sekarang masuk langkah ke ${stepIndex + 1},"
      stepIndex % 3 == 2 -> "Terus, lanjut ke langkah ke ${stepIndex + 1} ya,"
      else -> "Oke, kita lanjut langkah ke ${stepIndex + 1} ya,"
    }

  val expanded =
    expandCulinaryTermsForSpeech(rawInstruction)
      .replace(Regex("(?i)\\blalu\\b"), ", terus")
      .replace(Regex("(?i)\\bkemudian\\b"), ", terus")
      .replace(Regex(",\\s*,+"), ",")
      .trim()

  val rawClauses =
    expanded
      .split(Regex("(?<=[,.!?])\\s+"))
      .map { it.trim().trimEnd('.') }
      .filter { it.isNotBlank() }
      .ifEmpty { listOf(expanded.trimEnd('.')) }

  val segments = mutableListOf<SpeechSegment>()

  // 1. Nomor Langkah + Filler ramah dengan nada sedikit naik
  segments +=
    SpeechSegment(
      text = introText,
      pitchDelta = +0.05f,
      rateDelta = -0.02f,
      pauseAfterMs = 260L,
    )

  // 2. Bahan & Aksi dengan intonasi naik-turun antar klausa + jeda napas
  rawClauses.forEachIndexed { idx, clause ->
    val withBreathingComma =
      if (clause.endsWith(",") || clause.endsWith("?") || clause.endsWith("!")) clause
      else "$clause,"
    val spokenClause =
      if (idx > 0 && !withBreathingComma.lowercase().startsWith("terus") &&
        !withBreathingComma.lowercase().startsWith("nah") &&
        !withBreathingComma.lowercase().startsWith("oke")
      ) {
        "terus, $withBreathingComma"
      } else {
        withBreathingComma
      }

    val pitchWave =
      when (idx % 3) {
        0 -> +0.02f
        1 -> -0.02f
        else -> +0.04f
      }
    val rateWave = if (idx % 2 == 0) 0f else -0.03f

    segments +=
      SpeechSegment(
        text = spokenClause,
        pitchDelta = pitchWave,
        rateDelta = rateWave,
        pauseAfterMs = 240L,
      )
  }

  // 3. Peringatan Kecil bergaya teman di samping kompor (intonasi hangat & meyakinkan)
  val reminder = buildStoveSideReminder(stepIndex, rawInstruction)
  segments +=
    SpeechSegment(
      text = "Nah, $reminder",
      pitchDelta = -0.03f,
      rateDelta = -0.05f,
      pauseAfterMs = 0L,
    )

  return segments
}

fun buildHumanoidPreviewSegments(rawText: String): List<SpeechSegment> {
  val clauses =
    expandCulinaryTermsForSpeech(rawText)
      .split(Regex("(?<=[,.!?])\\s+"))
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .ifEmpty { listOf(rawText) }

  return clauses.mapIndexed { idx, clause ->
    val isLast = idx == clauses.lastIndex
    SpeechSegment(
      text = clause,
      pitchDelta =
        when {
          idx == 0 -> +0.05f
          isLast -> -0.03f
          idx % 2 == 1 -> +0.01f
          else -> -0.02f
        },
      rateDelta = if (isLast) -0.05f else if (idx == 0) -0.02f else 0f,
      pauseAfterMs = if (isLast) 0L else 230L,
    )
  }
}

fun formatStepForNaturalSpeech(
  stepIndex: Int,
  totalSteps: Int,
  rawInstruction: String,
): String {
  return buildHumanoidCookingSegments(stepIndex, totalSteps, rawInstruction)
    .joinToString(" ") { it.text }
    .replace(Regex("\\s+"), " ")
    .trim()
}

fun stopChefSpeech(tts: TextToSpeech, onSpeakingChanged: (Boolean) -> Unit = {}) {
  speechSessionToken.incrementAndGet()
  mainHandler.removeCallbacksAndMessages(null)
  try {
    tts.stop()
  } catch (_: Exception) {}
  onSpeakingChanged(false)
}

fun speakHumanoidSegments(
  tts: TextToSpeech,
  preset: ChefVoicePreset,
  segments: List<SpeechSegment>,
  utterancePrefix: String,
  onSpeakingChanged: (Boolean) -> Unit,
) {
  if (segments.isEmpty()) return

  val activeToken = speechSessionToken.incrementAndGet()
  mainHandler.removeCallbacksAndMessages(null)
  applyVoicePresetToTts(tts, preset)

  fun enqueueSegment(index: Int) {
    if (speechSessionToken.get() != activeToken || index >= segments.size) {
      onSpeakingChanged(false)
      return
    }
    val segment = segments[index]
    val targetPitch = (preset.pitch + segment.pitchDelta).coerceIn(0.68f, 1.40f)
    val targetRate = (preset.speechRate + segment.rateDelta).coerceIn(0.68f, 1.25f)
    tts.setPitch(targetPitch)
    tts.setSpeechRate(targetRate)

    val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) }
    val utteranceId = "${utterancePrefix}_seg_$index"
    val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
    tts.speak(segment.text, queueMode, params, utteranceId)
  }

  tts.setOnUtteranceProgressListener(
    object : UtteranceProgressListener() {
      override fun onStart(id: String?) {
        if (speechSessionToken.get() == activeToken) {
          mainHandler.post { onSpeakingChanged(true) }
        }
      }

      override fun onDone(id: String?) {
        if (speechSessionToken.get() != activeToken) return
        val index = id?.substringAfterLast("_seg_")?.toIntOrNull() ?: return
        val nextIndex = index + 1
        if (nextIndex < segments.size) {
          val pauseMs = segments[index].pauseAfterMs.coerceAtLeast(120L)
          mainHandler.postDelayed(
            { enqueueSegment(nextIndex) },
            pauseMs,
          )
        } else {
          mainHandler.post { onSpeakingChanged(false) }
        }
      }

      @Deprecated("Deprecated in Java")
      override fun onError(id: String?) {
        if (speechSessionToken.get() == activeToken) {
          mainHandler.post { onSpeakingChanged(false) }
        }
      }

      override fun onError(id: String?, errorCode: Int) {
        if (speechSessionToken.get() == activeToken) {
          mainHandler.post { onSpeakingChanged(false) }
        }
      }
    }
  )

  enqueueSegment(0)
}

fun speakWithPreset(
  tts: TextToSpeech,
  preset: ChefVoicePreset,
  text: String,
  utteranceId: String,
  onSpeakingChanged: (Boolean) -> Unit,
) {
  speakHumanoidSegments(
    tts = tts,
    preset = preset,
    segments = buildHumanoidPreviewSegments(text),
    utterancePrefix = utteranceId,
    onSpeakingChanged = onSpeakingChanged,
  )
}

fun playTimerCompletionChime() {
  try {
    val generator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
    Handler(Looper.getMainLooper()).postDelayed(
      {
        try {
          generator.release()
        } catch (_: Exception) {}
      },
      600L,
    )
  } catch (_: Exception) {
    // Ignore audio hardware limitations on headless test/emulator environments
  }
}
