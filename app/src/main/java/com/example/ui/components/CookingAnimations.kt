package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class CookingActionType(val badgeLabel: String) {
  CHOPPING("MEMOTONG & PERSIAPAN"),
  FRYING("MENGGORENG & MENUMIS"),
  BOILING("MEREBUS & MENGUKUS"),
  MIXING("MENGADUK & MERACIK"),
  BAKING("MEMANGGANG & BAKAR"),
  SERVING("MENYAJIKAN HIDANGAN"),
}

fun detectCookingAction(instruction: String): CookingActionType {
  val text = instruction.lowercase()
  return when {
    listOf("iris", "potong", "cincang", "geprek", "dadu", "rajang", "kupas", "suwir").any {
      text.contains(it)
    } -> CookingActionType.CHOPPING
    listOf("goreng", "tumis", "wajan", "teflon", "minyak", "lelehkan", "oseng").any {
      text.contains(it)
    } -> CookingActionType.FRYING
    listOf("rebus", "didih", "kukus", "kuah", "kaldu", "panci", "air mendidih").any {
      text.contains(it)
    } -> CookingActionType.BOILING
    listOf("kocok", "aduk", "campur", "lumuri", "marinasi", "uleni", "bumbui").any {
      text.contains(it)
    } -> CookingActionType.MIXING
    listOf("panggang", "bakar", "oven", "grill", "oven").any { text.contains(it) } ->
      CookingActionType.BAKING
    else -> CookingActionType.SERVING
  }
}

@Composable
fun CookingStepVisualStage(
  actionType: CookingActionType,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "cooking_stage")
  val cycle by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
      label = "cycle",
    )
  val pulse by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "pulse",
    )

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Slate900.copy(alpha = 0.9f),
    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.45f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(modifier = Modifier.size(width = 160.dp, height = 104.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height
          when (actionType) {
            CookingActionType.CHOPPING -> {
              // Cutting board
              drawRoundRect(
                color = Color(0xFFD97706),
                topLeft = Offset(w * 0.14f, h * 0.72f),
                size = Size(w * 0.72f, h * 0.16f),
                cornerRadius = CornerRadius(14f, 14f),
              )
              drawRoundRect(
                color = Color(0xFFB45309),
                topLeft = Offset(w * 0.14f, h * 0.82f),
                size = Size(w * 0.72f, h * 0.06f),
                cornerRadius = CornerRadius(8f, 8f),
              )

              // Ingredient pieces popping up on impact
              val bounce = kotlin.math.abs(sin(cycle * 2f * PI.toFloat()))
              drawCircle(
                color = Emerald500,
                radius = 7f,
                center = Offset(w * 0.36f - bounce * 8f, h * 0.67f - bounce * 18f),
              )
              drawCircle(
                color = Amber500,
                radius = 6f,
                center = Offset(w * 0.48f, h * 0.67f - bounce * 10f),
              )
              drawCircle(
                color = Rose500,
                radius = 6f,
                center = Offset(w * 0.62f + bounce * 10f, h * 0.67f - bounce * 20f),
              )

              // Chopping knife pivoting down smoothly
              val chopAngle = -24f + pulse * 26f
              rotate(degrees = chopAngle, pivot = Offset(w * 0.74f, h * 0.62f)) {
                // Handle
                drawRoundRect(
                  color = Slate700,
                  topLeft = Offset(w * 0.62f, h * 0.43f),
                  size = Size(w * 0.24f, h * 0.10f),
                  cornerRadius = CornerRadius(10f, 10f),
                )
                // Steel blade
                val blade =
                  Path().apply {
                    moveTo(w * 0.25f, h * 0.42f)
                    lineTo(w * 0.63f, h * 0.42f)
                    lineTo(w * 0.63f, h * 0.62f)
                    lineTo(w * 0.30f, h * 0.60f)
                    close()
                  }
                drawPath(blade, color = Slate100)
                drawLine(
                  color = Slate400,
                  start = Offset(w * 0.29f, h * 0.59f),
                  end = Offset(w * 0.62f, h * 0.61f),
                  strokeWidth = 3f,
                )
              }
            }

            CookingActionType.FRYING -> {
              // Flickering stove flames underneath pan
              val flameOffset = pulse * 6f
              listOf(0.28f, 0.42f, 0.56f).forEachIndexed { idx, xPos ->
                val heightFactor = if (idx == 1) 1.25f else 1f
                drawOval(
                  color = if (idx == 1) Amber500 else Color(0xFFEA580C),
                  topLeft = Offset(w * xPos, h * (0.76f - 0.04f * pulse)),
                  size = Size(w * 0.08f, (h * 0.16f + flameOffset) * heightFactor),
                )
              }

              // Frying pan gentle toss animation
              val panTilt = sin(cycle * 2f * PI.toFloat()) * 4f
              rotate(degrees = panTilt, pivot = Offset(w * 0.45f, h * 0.65f)) {
                // Handle
                drawRoundRect(
                  color = Slate700,
                  topLeft = Offset(w * 0.68f, h * 0.56f),
                  size = Size(w * 0.26f, h * 0.09f),
                  cornerRadius = CornerRadius(10f, 10f),
                )
                // Pan body
                drawRoundRect(
                  color = Slate800,
                  topLeft = Offset(w * 0.16f, h * 0.55f),
                  size = Size(w * 0.55f, h * 0.18f),
                  cornerRadius = CornerRadius(18f, 18f),
                )
                drawLine(
                  color = Slate400,
                  start = Offset(w * 0.18f, h * 0.56f),
                  end = Offset(w * 0.69f, h * 0.56f),
                  strokeWidth = 3f,
                )
              }

              // Sizzling food pops & steam droplets
              val pop1 = ((cycle + 0.1f) % 1f)
              val pop2 = ((cycle + 0.55f) % 1f)
              drawCircle(
                color = Amber100.copy(alpha = 1f - pop1),
                radius = 5f + pop1 * 4f,
                center = Offset(w * 0.32f, h * 0.50f - pop1 * h * 0.34f),
              )
              drawCircle(
                color = Amber500.copy(alpha = 1f - pop2),
                radius = 6f,
                center = Offset(w * 0.50f, h * 0.50f - pop2 * h * 0.38f),
              )
              drawCircle(
                color = Emerald500.copy(alpha = 0.85f * (1f - pop1)),
                radius = 4.5f,
                center = Offset(w * 0.42f, h * 0.48f - pop1 * h * 0.25f),
              )
            }

            CookingActionType.BOILING -> {
              // Pot handles & body
              drawRoundRect(
                color = Slate400,
                topLeft = Offset(w * 0.18f, h * 0.50f),
                size = Size(w * 0.64f, h * 0.07f),
                cornerRadius = CornerRadius(6f, 6f),
              )
              drawRoundRect(
                color = Slate700,
                topLeft = Offset(w * 0.24f, h * 0.46f),
                size = Size(w * 0.52f, h * 0.38f),
                cornerRadius = CornerRadius(18f, 18f),
              )
              // Lid floating gently from steam
              val lidLift = pulse * 6f
              drawRoundRect(
                color = Slate400,
                topLeft = Offset(w * 0.22f, h * 0.41f - lidLift),
                size = Size(w * 0.56f, h * 0.06f),
                cornerRadius = CornerRadius(8f, 8f),
              )
              drawCircle(
                color = Emerald500,
                radius = 6f,
                center = Offset(w * 0.50f, h * 0.37f - lidLift),
              )

              // Boiling bubbles & rising steam
              listOf(0.36f, 0.50f, 0.64f).forEachIndexed { i, xPos ->
                val phase = (cycle + i * 0.33f) % 1f
                drawCircle(
                  color = Emerald100.copy(alpha = (1f - phase) * 0.85f),
                  radius = 4f + phase * 6f,
                  center = Offset(w * xPos + sin(phase * 6f) * 6f, h * 0.36f - phase * h * 0.30f),
                )
              }
            }

            CookingActionType.MIXING -> {
              // Mixing bowl
              drawArc(
                color = Emerald700,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.22f, h * 0.34f),
                size = Size(w * 0.56f, h * 0.52f),
              )
              drawOval(
                color = Amber100.copy(alpha = 0.9f),
                topLeft = Offset(w * 0.26f, h * 0.53f),
                size = Size(w * 0.48f, h * 0.14f),
              )

              // Whisk stirring in smooth figure-8 / orbit
              val orbitX = cos(cycle * 2f * PI.toFloat()) * w * 0.14f
              val orbitY = sin(cycle * 2f * PI.toFloat()) * h * 0.04f
              drawLine(
                color = Slate100,
                start = Offset(w * 0.52f + orbitX * 0.4f, h * 0.12f),
                end = Offset(w * 0.50f + orbitX, h * 0.58f + orbitY),
                strokeWidth = 7f,
                cap = StrokeCap.Round,
              )
              drawOval(
                color = Slate400,
                topLeft = Offset(w * 0.45f + orbitX, h * 0.50f + orbitY),
                size = Size(w * 0.10f, h * 0.14f),
                style = Stroke(width = 3.5f),
              )
            }

            CookingActionType.BAKING -> {
              // Grill rack / tray
              drawRoundRect(
                color = Slate700,
                topLeft = Offset(w * 0.16f, h * 0.58f),
                size = Size(w * 0.68f, h * 0.10f),
                cornerRadius = CornerRadius(10f, 10f),
              )
              // Dish / roast on rack
              drawOval(
                color = Color(0xFFD97706),
                topLeft = Offset(w * 0.30f, h * 0.38f),
                size = Size(w * 0.40f, h * 0.22f),
              )
              // Glowing heat embers below
              listOf(0.28f, 0.44f, 0.60f).forEachIndexed { idx, xPos ->
                drawCircle(
                  color = Color(0xFFEA580C).copy(alpha = 0.55f + 0.45f * pulse),
                  radius = 10f + (if (idx == 1) 4f else 0f) * pulse,
                  center = Offset(w * (xPos + 0.06f), h * 0.80f),
                )
              }
              // Rising heat waves
              listOf(0.36f, 0.50f, 0.64f).forEachIndexed { idx, xPos ->
                val phase = (cycle + idx * 0.3f) % 1f
                drawCircle(
                  color = Amber500.copy(alpha = 0.7f * (1f - phase)),
                  radius = 4f,
                  center = Offset(w * xPos, h * 0.32f - phase * h * 0.24f),
                )
              }
            }

            CookingActionType.SERVING -> {
              // Serving plate
              drawOval(
                color = Slate100,
                topLeft = Offset(w * 0.18f, h * 0.56f),
                size = Size(w * 0.64f, h * 0.24f),
              )
              drawOval(
                color = Emerald500.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.30f, h * 0.50f),
                size = Size(w * 0.40f, h * 0.18f),
              )
              // Aroma & sparkle
              listOf(0.36f, 0.50f, 0.64f).forEachIndexed { idx, xPos ->
                val phase = (cycle + idx * 0.28f) % 1f
                drawCircle(
                  color = Emerald100.copy(alpha = 1f - phase),
                  radius = 4f + pulse * 2f,
                  center = Offset(w * xPos + sin(phase * 5f) * 5f, h * 0.44f - phase * h * 0.32f),
                )
              }
            }
          }
        }
      }

      Surface(
        shape = CircleShape,
        color = Emerald950,
        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.6f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Canvas(modifier = Modifier.size(7.dp)) {
            drawCircle(color = Emerald500.copy(alpha = 0.5f + 0.5f * pulse))
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = actionType.badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Emerald100,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun VoiceEqualizerBars(
  isActive: Boolean,
  modifier: Modifier = Modifier,
  barColor: Color = Emerald500,
) {
  val transition = rememberInfiniteTransition(label = "voice_eq")
  val wave by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
      label = "wave",
    )

  Canvas(modifier = modifier.size(width = 24.dp, height = 16.dp)) {
    val barCount = 4
    val spacing = size.width / (barCount * 2f)
    for (i in 0 until barCount) {
      val ratio =
        if (isActive) 0.35f + 0.65f * kotlin.math.abs(sin(wave + i * 1.1f)) else 0.25f
      val barHeight = size.height * ratio
      val x = spacing * (i * 2 + 0.5f)
      drawLine(
        color = barColor,
        start = Offset(x, size.height - (size.height - barHeight) / 2f),
        end = Offset(x, (size.height - barHeight) / 2f),
        strokeWidth = spacing * 0.85f,
        cap = StrokeCap.Round,
      )
    }
  }
}
