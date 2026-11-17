package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.CookingSession
import com.example.ui.components.ChefVoicePreset
import com.example.ui.components.CookingSpeechCommandController
import com.example.ui.components.CookingStepVisualStage
import com.example.ui.components.CookingVoiceCommand
import com.example.ui.components.VoiceEqualizerBars
import com.example.ui.components.applyVoicePresetToTts
import com.example.ui.components.buildHumanoidCookingSegments
import com.example.ui.components.detectCookingAction
import com.example.ui.components.hasAudioPermission
import com.example.ui.components.loadSavedVoicePreset
import com.example.ui.components.playTimerCompletionChime
import com.example.ui.components.saveVoicePreset
import com.example.ui.components.speakHumanoidSegments
import com.example.ui.components.speakWithPreset
import com.example.ui.components.stopChefSpeech
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun CookingModeScreen(
  session: CookingSession,
  onClose: () -> Unit,
  onPrevStep: () -> Unit,
  onNextStep: () -> Unit,
  onToggleTimer: () -> Unit,
  onResetTimer: () -> Unit,
  onToggleVoiceReadout: () -> Unit,
  onDismissDeductDialog: () -> Unit,
  onConfirmAutoDeduct: () -> Unit,
) {
  BackHandler(onBack = onClose)

  val context = LocalContext.current
  var ttsReady by remember { mutableStateOf(false) }
  var isSpeaking by remember { mutableStateOf(false) }
  var selectedVoice by remember { mutableStateOf(loadSavedVoicePreset(context)) }
  var showVoicePicker by remember { mutableStateOf(false) }
  var replayTrigger by remember { mutableIntStateOf(0) }

  // Hands-free voice commands & keep-screen-on state
  var voiceCommandsEnabled by remember { mutableStateOf(false) }
  var isMicListening by remember { mutableStateOf(false) }
  var lastVoiceFeedback by remember { mutableStateOf<String?>(null) }

  val currentOnNextStep by rememberUpdatedState(onNextStep)
  val currentOnPrevStep by rememberUpdatedState(onPrevStep)
  val currentOnToggleTimer by rememberUpdatedState(onToggleTimer)
  val currentOnResetTimer by rememberUpdatedState(onResetTimer)

  DisposableEffect(context) {
    val window = context.findCookingActivity()?.window
    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  val speechController =
    remember(context) {
      CookingSpeechCommandController(
        context = context,
        onCommand = { cmd, spoken ->
          lastVoiceFeedback = "\"$spoken\""
          when (cmd) {
            CookingVoiceCommand.NEXT_STEP -> currentOnNextStep()
            CookingVoiceCommand.PREV_STEP -> currentOnPrevStep()
            CookingVoiceCommand.TOGGLE_TIMER -> currentOnToggleTimer()
            CookingVoiceCommand.RESET_TIMER -> currentOnResetTimer()
            CookingVoiceCommand.REPLAY_VOICE -> replayTrigger++
          }
        },
        onListeningStateChanged = { listening -> isMicListening = listening },
      )
    }

  DisposableEffect(speechController) {
    onDispose { speechController.destroy() }
  }

  LaunchedEffect(voiceCommandsEnabled) {
    speechController.setEnabled(voiceCommandsEnabled)
  }

  LaunchedEffect(isSpeaking) {
    speechController.setPausedForTts(isSpeaking)
  }

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        lastVoiceFeedback = "Mikrofon aktif! Ucapkan: Lanjut, Kembali, Timer, Ulangi"
        voiceCommandsEnabled = true
      } else {
        voiceCommandsEnabled = false
        lastVoiceFeedback = "Izin mikrofon ditolak"
      }
    }

  val tts = remember {
    TextToSpeech(context) { status -> ttsReady = (status == TextToSpeech.SUCCESS) }
  }

  DisposableEffect(tts) {
    onDispose {
      stopChefSpeech(tts)
      tts.shutdown()
    }
  }

  LaunchedEffect(ttsReady, selectedVoice) {
    if (ttsReady) {
      applyVoicePresetToTts(tts, selectedVoice)
    }
  }

  val totalSteps = session.recipe.steps.size.coerceAtLeast(1)
  val currentStep = session.recipe.steps.getOrNull(session.currentStepIndex)

  LaunchedEffect(
    session.currentStepIndex,
    session.voiceReadoutEnabled,
    ttsReady,
    selectedVoice,
    replayTrigger,
  ) {
    if (session.voiceReadoutEnabled && ttsReady && currentStep != null) {
      val segments =
        buildHumanoidCookingSegments(
          stepIndex = session.currentStepIndex,
          totalSteps = totalSteps,
          rawInstruction = currentStep.instruction,
        )
      speakHumanoidSegments(
        tts = tts,
        preset = selectedVoice,
        segments = segments,
        utterancePrefix = "step_${session.currentStepIndex}_${selectedVoice.name}_$replayTrigger",
        onSpeakingChanged = { active -> isSpeaking = active },
      )
    } else if (!session.voiceReadoutEnabled) {
      stopChefSpeech(tts) { active -> isSpeaking = active }
    }
  }

  // Track timer completion to play gentle chime + conversational voice alert
  var prevRemaining by remember(session.currentStepIndex) {
    mutableIntStateOf(session.remainingSeconds)
  }
  LaunchedEffect(session.remainingSeconds, session.currentStepIndex) {
    if (prevRemaining > 0 && session.remainingSeconds == 0) {
      playTimerCompletionChime()
      if (session.voiceReadoutEnabled && ttsReady) {
        speakWithPreset(
          tts = tts,
          preset = selectedVoice,
          text = "Nah, waktunya habis ya, timer langkah ke ${session.currentStepIndex + 1} sudah bunyi, yuk cek masakannya.",
          utteranceId = "timer_done_${session.currentStepIndex}",
          onSpeakingChanged = { active -> isSpeaking = active },
        )
      }
    }
    prevRemaining = session.remainingSeconds
  }

  val minutes = (session.remainingSeconds / 60).toString().padStart(2, '0')
  val seconds = (session.remainingSeconds % 60).toString().padStart(2, '0')
  val stepDuration = (currentStep?.durationSeconds ?: 180).coerceAtLeast(1)
  val timerProgress by
    animateFloatAsState(
      targetValue = (session.remainingSeconds.toFloat() / stepDuration.toFloat()).coerceIn(0f, 1f),
      animationSpec = tween(450),
      label = "timer_ring",
    )

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Slate950)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(20.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top Header
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onClose,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("cooking_close_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = "Keluar", tint = Slate400)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Keluar", color = Slate400)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.cooking_mode_tag).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Emerald500,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = session.recipe.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
          Text(
            text = "${session.recipe.servings} Porsi • ${session.recipe.resolvedNutrition().caloriesKcal} kkal/porsi",
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = if (voiceCommandsEnabled) Emerald600 else Slate900,
            border = BorderStroke(1.dp, if (voiceCommandsEnabled) Emerald500 else Slate800),
          ) {
            IconButton(
              onClick = {
                if (voiceCommandsEnabled) {
                  voiceCommandsEnabled = false
                  lastVoiceFeedback = null
                } else if (hasAudioPermission(context)) {
                  lastVoiceFeedback = "Siap mendengarkan..."
                  voiceCommandsEnabled = true
                } else {
                  audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("cooking_mic_toggle"),
            ) {
              Icon(
                imageVector = if (voiceCommandsEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = "Perintah Suara Hands-Free",
                tint = if (voiceCommandsEnabled) Color.White else Slate400,
              )
            }
          }

          Surface(shape = CircleShape, color = Slate900, border = BorderStroke(1.dp, Slate800)) {
            IconButton(
              onClick = { showVoicePicker = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("cooking_voice_picker_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.RecordVoiceOver,
                contentDescription = "Pilih Referensi Suara Chef",
                tint = Emerald500,
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = if (session.voiceReadoutEnabled) Emerald600 else Emerald900,
          ) {
            IconButton(
              onClick = onToggleVoiceReadout,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("cooking_tts_toggle"),
            ) {
              Icon(
                imageVector =
                  if (session.voiceReadoutEnabled) Icons.AutoMirrored.Filled.VolumeUp
                  else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = "Baca langkah dengan suara",
                tint = if (session.voiceReadoutEnabled) Color.White else Emerald500,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Animated Step Progress Bars
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        repeat(totalSteps) { index ->
          val barColor by
            animateColorAsState(
              targetValue = if (index <= session.currentStepIndex) Emerald500 else Slate800,
              animationSpec = tween(300),
              label = "step_bar_$index",
            )
          Box(
            modifier =
              Modifier.weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(barColor)
          )
        }
      }
    }

    // Center Step Animation + Instruction + Countdown Timer
    Column(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      AnimatedContent(
        targetState = session.currentStepIndex,
        transitionSpec = {
          val forward = targetState >= initialState
          (fadeIn(animationSpec = tween(260)) +
            slideInHorizontally(
              animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
              initialOffsetX = { fullWidth -> if (forward) fullWidth / 3 else -fullWidth / 3 },
            )) togetherWith
            (fadeOut(animationSpec = tween(180)) +
              slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (forward) -fullWidth / 3 else fullWidth / 3 }
              ))
        },
        label = "step_transition",
      ) { stepIndex ->
        val step = session.recipe.steps.getOrNull(stepIndex)
        val actionType = remember(step?.instruction) {
          detectCookingAction(step?.instruction.orEmpty())
        }
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          CookingStepVisualStage(actionType = actionType)

          Surface(
            shape = CircleShape,
            color = Emerald950,
            border = BorderStroke(1.dp, Emerald700),
          ) {
            Text(
              text = "LANGKAH ${stepIndex + 1} DARI $totalSteps",
              style = MaterialTheme.typography.labelMedium,
              color = Emerald500,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
          }

          Text(
            text = step?.instruction.orEmpty(),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 29.sp),
            color = Slate100,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Live Step Timer Card with Circular Progress Ring
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Slate900,
        border = BorderStroke(1.dp, if (session.timerRunning) Emerald600 else Slate800),
        modifier = Modifier.fillMaxWidth(0.88f),
      ) {
        Column(
          modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "TIMER MEMASAK LANGKAH INI",
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Box(contentAlignment = Alignment.Center, modifier = Modifier.size(128.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val stroke = 10.dp.toPx()
              drawArc(
                color = Slate800,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
              )
              drawArc(
                color = if (session.remainingSeconds == 0) AmberAccent else Emerald500,
                startAngle = -90f,
                sweepAngle = 360f * timerProgress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
              )
            }
            Text(
              text = "$minutes:$seconds",
              style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
              color = if (session.remainingSeconds == 0) AmberAccent else Emerald500,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onToggleTimer,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
              modifier = Modifier.height(48.dp).testTag("cooking_timer_toggle"),
            ) {
              Icon(
                imageVector =
                  if (session.timerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text =
                  when {
                    session.timerRunning -> "Pause"
                    session.remainingSeconds == 0 -> "Ulang"
                    else -> "Start"
                  }
              )
            }
            Button(
              onClick = onResetTimer,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Slate100),
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reset")
            }
          }
        }
      }
    }

    // Bottom Voice Preset Cue + Hands-Free Status + Step Navigation
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (voiceCommandsEnabled || lastVoiceFeedback != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isMicListening) Emerald900 else Slate900,
          border = BorderStroke(1.dp, if (isMicListening) Emerald500 else Slate800),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = if (voiceCommandsEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
                tint = if (isMicListening) Emerald500 else Slate400,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (voiceCommandsEnabled) {
                    val status = if (isSpeaking) "Menunggu chef selesai bicara..." else "Hands-Free: \"Lanjut\" • \"Kembali\" • \"Timer\" • \"Ulangi\""
                    if (lastVoiceFeedback != null && !lastVoiceFeedback!!.startsWith("Siap")) "$status ($lastVoiceFeedback)" else status
                  } else {
                    lastVoiceFeedback.orEmpty()
                  },
                style = MaterialTheme.typography.labelSmall,
                color = Slate100,
              )
            }
          }
        }
      }
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Emerald950,
        border = BorderStroke(1.dp, Emerald900),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { showVoicePicker = true },
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (session.voiceReadoutEnabled && isSpeaking) {
              VoiceEqualizerBars(isActive = true)
            } else {
              Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = selectedVoice.title,
                style = MaterialTheme.typography.labelMedium,
                color = Emerald100,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  if (session.voiceReadoutEnabled) "Pemandu suara aktif • Ketuk untuk ganti karakter"
                  else "Ketuk speaker di atas untuk aktifkan suara • Ketuk di sini untuk pilih karakter",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
              )
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
          if (session.voiceReadoutEnabled && ttsReady) {
            IconButton(
              onClick = { replayTrigger++ },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("cooking_replay_voice_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Ulangi instruksi suara",
                tint = Emerald500,
              )
            }
          }
          Text(
            text = "Pilih",
            style = MaterialTheme.typography.labelMedium,
            color = Emerald500,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      HorizontalDivider(color = Slate800)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onPrevStep,
          enabled = session.currentStepIndex > 0,
          shape = RoundedCornerShape(16.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Slate900,
              contentColor = Slate100,
              disabledContainerColor = Slate900.copy(alpha = 0.4f),
              disabledContentColor = Slate700,
            ),
          modifier = Modifier.weight(1f).height(52.dp).testTag("cooking_prev_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Kembali")
        }

        Button(
          onClick = onNextStep,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
          modifier = Modifier.weight(1.5f).height(52.dp).testTag("cooking_next_button"),
        ) {
          Text(
            text =
              if (session.currentStepIndex == totalSteps - 1) "Selesai Memasak!"
              else "Langkah Berikutnya",
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }
    }
  }

  // Voice Reference Picker Dialog
  if (showVoicePicker) {
    AlertDialog(
      onDismissRequest = { showVoicePicker = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = Emerald700)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Referensi Suara Mode Masak", style = MaterialTheme.typography.titleMedium)
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Pilih karakter suara Chef yang paling nyaman didengar saat memasak. Ketuk 'Dengar' untuk mencoba contoh suara:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          ChefVoicePreset.entries.forEach { preset ->
            val isSelected = preset == selectedVoice
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
              border =
                BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color =
                    if (isSelected) Emerald600 else MaterialTheme.colorScheme.outline,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    selectedVoice = preset
                    saveVoicePreset(context, preset)
                  },
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                      if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                  )
                  Surface(shape = CircleShape, color = Emerald700) {
                    Text(
                      text = preset.badge,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }
                Text(
                  text = preset.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = if (isSelected) "Dipilih" else "Ketuk kartu untuk memilih",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  )
                  OutlinedButton(
                    onClick = {
                      selectedVoice = preset
                      saveVoicePreset(context, preset)
                      if (ttsReady) {
                        speakWithPreset(
                          tts = tts,
                          preset = preset,
                          text = preset.sampleText,
                          utteranceId = "preview_${preset.name}",
                          onSpeakingChanged = { active -> isSpeaking = active },
                        )
                      }
                    },
                    shape = CircleShape,
                    modifier = Modifier.height(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dengar", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showVoicePicker = false },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
        ) {
          Text("Gunakan Suara Ini")
        }
      },
    )
  }

  // Accurate Auto-Deduct Confirmation Dialog
  if (session.showDeductDialog) {
    AlertDialog(
      onDismissRequest = onDismissDeductDialog,
      icon = {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = Emerald700,
          modifier = Modifier.size(40.dp),
        )
      },
      title = {
        Text(
          text = "Masakan Selesai!",
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Center,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text =
              if (session.pendingDeductions.isNotEmpty())
                "Sistem akan otomatis mengurangi stok bahan terpakai untuk \"${session.recipe.title}\" dari Smart Pantry Anda."
              else "Resep \"${session.recipe.title}\" selesai dimasak! Tidak ada bahan kulkas yang perlu dikurangi.",
            style = MaterialTheme.typography.bodySmall,
          )
          if (session.pendingDeductions.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "Bahan Terpotong:",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
                session.pendingDeductions.forEach { d ->
                  Text(
                    text = "• ${d.ingredientName} (-${d.deductAmount} ${d.unit})",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = onConfirmAutoDeduct,
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
          modifier = Modifier.testTag("confirm_auto_deduct_button"),
        ) {
          Text(
            if (session.pendingDeductions.isNotEmpty()) "Update Stok Pantry"
            else "Selesai",
          )
        }
      },
      dismissButton = {
        TextButton(onClick = onClose) { Text("Tutup Tanpa Potong") }
      },
    )
  }
}

private val AmberAccent = Color(0xFFF59E0B)

private tailrec fun Context.findCookingActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findCookingActivity()
    else -> null
  }
