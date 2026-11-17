package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MainDestination
import com.example.model.NotesTab
import com.example.model.PantryItem
import com.example.model.Recipe
import com.example.model.UserProfile
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald200
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple700
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose800
import com.example.ui.theme.Sky100
import com.example.ui.theme.Sky700
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal100
import com.example.ui.theme.Teal700

private data class QuickMenuAction(
  val label: String,
  val icon: ImageVector,
  val containerColor: Color,
  val iconTint: Color,
  val tag: String,
  val badge: String? = null,
  val onClick: () -> Unit,
)

@Composable
fun HomeScreen(
  profile: UserProfile?,
  pantryItems: List<PantryItem>,
  recipes: List<Recipe>,
  isOnline: Boolean = true,
  onNavigate: (MainDestination, NotesTab?) -> Unit,
  onStartCooking: (Recipe) -> Unit,
  onOpenScan: () -> Unit,
  onOpenSubstitution: () -> Unit,
) {
  val expiringItems = remember(pantryItems) { pantryItems.filter { it.expiringSoon } }
  val topExpiring = expiringItems.minByOrNull { it.daysUntilExpiry }
  val featuredRecipe =
    remember(recipes, pantryItems) {
      recipes.maxByOrNull {
        val status = it.evaluateMatch(pantryItems)
        status.matchPercent + if (status.isPreventWaste) 25 else 0
      } ?: recipes.firstOrNull()
    }
  val featuredMatch =
    remember(featuredRecipe, pantryItems) { featuredRecipe?.evaluateMatch(pantryItems) }

  val pulseTransition = rememberInfiniteTransition(label = "home_pulse")
  val alertScale by
    pulseTransition.animateFloat(
      initialValue = 1f,
      targetValue = 1.14f,
      animationSpec =
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "alert_scale",
    )

  val quickActions =
    remember(featuredRecipe) {
      listOf(
        QuickMenuAction(
          label = "Smart Pantry",
          icon = Icons.Filled.Inventory2,
          containerColor = Emerald100,
          iconTint = Emerald700,
          tag = "grid_pantry",
          onClick = { onNavigate(MainDestination.PANTRY, null) },
        ),
        QuickMenuAction(
          label = "AI Matcher",
          icon = Icons.Filled.AutoAwesome,
          containerColor = Emerald600,
          iconTint = Color.White,
          tag = "grid_ai_matcher",
          badge = "AI",
          onClick = { onNavigate(MainDestination.AI_MATCHER, null) },
        ),
        QuickMenuAction(
          label = "Mode Masak",
          icon = Icons.Filled.LocalFireDepartment,
          containerColor = Amber100,
          iconTint = Amber800,
          tag = "grid_cooking_mode",
          onClick = { featuredRecipe?.let(onStartCooking) },
        ),
        QuickMenuAction(
          label = "Explorer",
          icon = Icons.Filled.Explore,
          containerColor = Sky100,
          iconTint = Sky700,
          tag = "grid_explorer",
          onClick = { onNavigate(MainDestination.EXPLORER, null) },
        ),
        QuickMenuAction(
          label = "Jurnal Resep",
          icon = Icons.Filled.Book,
          containerColor = Purple100,
          iconTint = Purple700,
          tag = "grid_journal",
          onClick = { onNavigate(MainDestination.NOTES, NotesTab.JOURNAL) },
        ),
        QuickMenuAction(
          label = "Daftar Belanja",
          icon = Icons.Filled.ShoppingBasket,
          containerColor = Rose100,
          iconTint = Rose800,
          tag = "grid_shopping",
          onClick = { onNavigate(MainDestination.NOTES, NotesTab.SHOPPING) },
        ),
        QuickMenuAction(
          label = "Substitusi",
          icon = Icons.Filled.SwapHoriz,
          containerColor = Teal100,
          iconTint = Teal700,
          tag = "grid_substitution",
          onClick = onOpenSubstitution,
        ),
        QuickMenuAction(
          label = "Scan Struk",
          icon = Icons.Filled.QrCodeScanner,
          containerColor = Slate200,
          iconTint = Slate700,
          tag = "grid_scan_receipt",
          onClick = onOpenScan,
        ),
        QuickMenuAction(
          label = "Favorit",
          icon = Icons.Filled.Bookmark,
          containerColor = Amber100,
          iconTint = Amber800,
          tag = "grid_saved_recipes",
          onClick = { onNavigate(MainDestination.SAVED_RECIPES, null) },
        ),
        QuickMenuAction(
          label = "Menu Minggu",
          icon = Icons.Filled.CalendarMonth,
          containerColor = Sky100,
          iconTint = Sky700,
          tag = "grid_meal_planner",
          onClick = { onNavigate(MainDestination.MEAL_PLANNER, null) },
        ),
      )
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Greeting Header Card
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(Brush.horizontalGradient(listOf(Emerald800, Emerald700)))
              .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = CircleShape,
                color = Emerald600,
                border = androidx.compose.foundation.BorderStroke(2.dp, Emerald200),
                modifier = Modifier.size(48.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = "Chef Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Selamat Memasak,",
                  style = MaterialTheme.typography.bodySmall,
                  color = Emerald200,
                )
                Text(
                  text = "${profile?.displayName ?: "Chef Alex"}!",
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            IconButton(
              onClick = { onNavigate(MainDestination.PANTRY, null) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("home_bell_button"),
            ) {
              BadgedBox(
                badge = {
                  if (expiringItems.isNotEmpty()) {
                    Badge(containerColor = Rose500, contentColor = Color.White) {
                      Text("${expiringItems.size}")
                    }
                  }
                }
              ) {
                Surface(
                  shape = CircleShape,
                  color = Emerald600.copy(alpha = 0.55f),
                  modifier = Modifier.size(40.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.Notifications,
                      contentDescription = "Peringatan Kedaluwarsa",
                      tint = Color.White,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    if (!isOnline) {
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth().testTag("offline_banner"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.CloudOff,
              contentDescription = null,
              tint = Slate700,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Mode Offline aktif: Pantry, Belanja, Favorit & Rencana Menu tetap bisa dipakai.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // 2. Dynamic Stock Expiry Alert Banner (reacts dynamically to Pantry state)
    item {
      if (topExpiring != null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Amber50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.45f)),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onNavigate(MainDestination.AI_MATCHER, null) }
              .testTag("expiry_alert_banner"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Warning,
              contentDescription = null,
              tint = Amber500,
              modifier = Modifier.size(24.dp).scale(alertScale),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Peringatan Kedaluwarsa!",
                  style = MaterialTheme.typography.labelLarge,
                  color = Amber800,
                  fontWeight = FontWeight.Bold,
                )
                Surface(shape = RoundedCornerShape(6.dp), color = Amber100) {
                  Text(
                    text = "H-${topExpiring.daysUntilExpiry}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber800,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text =
                  "${topExpiring.name} (${topExpiring.quantity} ${topExpiring.unit}) hampir kedaluwarsa! Ketuk untuk rekomendasi resep AI.",
                style = MaterialTheme.typography.bodySmall,
                color = Amber800,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = Amber500,
            )
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Emerald50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onNavigate(MainDestination.PANTRY, null) },
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = null,
              tint = Emerald700,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Semua bahan di Smart Pantry dalam kondisi segar dan aman.",
              style = MaterialTheme.typography.bodySmall,
              color = Emerald950,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // 3. 8-Item Menu Dapur Utama Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Menu Dapur Utama",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "10 Fitur Aktif",
            style = MaterialTheme.typography.labelMedium,
            color = Emerald700,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          quickActions.chunked(4).forEach { rowItems ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              rowItems.forEach { action ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier =
                    Modifier.weight(1f)
                      .clip(RoundedCornerShape(14.dp))
                      .clickable(onClick = action.onClick)
                      .padding(vertical = 6.dp)
                      .testTag(action.tag),
                ) {
                  Box {
                    Surface(
                      shape = RoundedCornerShape(16.dp),
                      color = action.containerColor,
                      shadowElevation = 2.dp,
                      modifier = Modifier.size(56.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = action.icon,
                          contentDescription = action.label,
                          tint = action.iconTint,
                          modifier = Modifier.size(24.dp),
                        )
                      }
                    }
                    if (action.badge != null) {
                      Surface(
                        shape = CircleShape,
                        color = Rose500,
                        modifier =
                          Modifier.align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .scale(alertScale),
                      ) {
                        Text(
                          text = action.badge,
                          style = MaterialTheme.typography.labelSmall,
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              repeat((4 - rowItems.size).coerceAtLeast(0)) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }

    // 3b. Chef AI Chatbot Quick Access Banner
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Emerald50,
        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onNavigate(MainDestination.CHAT, null) }
            .testTag("home_chef_ai_banner"),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(shape = CircleShape, color = Emerald700, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Tanya Chef AI",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Tanya Chef AI (Chatbot Kuliner)",
              style = MaterialTheme.typography.titleSmall,
              color = Emerald950,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Tanya resep masakan, teknik masak, atau info nutrisi & cara simpan bahan.",
              style = MaterialTheme.typography.bodySmall,
              color = Emerald800,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Emerald700,
          )
        }
      }
    }

    // 4. Dynamic Top Recipe Suggestion Banner
    if (featuredRecipe != null && featuredMatch != null) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Slate900, Emerald950)))
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_kitchen_hero),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              alpha = 0.16f,
              modifier = Modifier.matchParentSize(),
            )
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = Emerald500.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.45f)),
              ) {
                Text(
                  text = "REKOMENDASI UTAMA",
                  style = MaterialTheme.typography.labelSmall,
                  color = Emerald200,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }

              Text(
                text = featuredRecipe.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
              )

              Text(
                text = featuredRecipe.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate200,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Emerald200,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${featuredRecipe.durationMinutes} Mins",
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald200,
                  )
                }
                Text(text = "•", color = Emerald200)
                Text(
                  text = featuredRecipe.difficulty,
                  style = MaterialTheme.typography.labelMedium,
                  color = Emerald200,
                )
                Text(text = "•", color = Emerald200)
                Text(
                  text = "${featuredMatch.matchPercent}% Match",
                  style = MaterialTheme.typography.labelMedium,
                  color = Emerald500,
                  fontWeight = FontWeight.Bold,
                )
              }

              Button(
                onClick = { onStartCooking(featuredRecipe) },
                shape = RoundedCornerShape(14.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = Emerald950,
                  ),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_start_cooking_button"),
              ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Mulai Memasak Sekarang",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}
