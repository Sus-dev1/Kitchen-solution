package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MealPlanEntry
import com.example.model.PantryItem
import com.example.model.Recipe
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950

@Composable
fun MealPlannerScreen(
  mealPlans: List<MealPlanEntry>,
  availableRecipes: List<Recipe>,
  pantryItems: List<PantryItem>,
  onBack: () -> Unit,
  onAssignMeal: (dayOfWeek: String, mealSlot: String, recipe: Recipe) -> Unit,
  onDeleteMeal: (planId: String) -> Unit,
  onSyncWeeklyShopping: () -> Unit,
  onStartCooking: (Recipe) -> Unit,
) {
  var selectedDay by remember { mutableStateOf(MealPlanEntry.WEEK_DAYS.first()) }
  var slotPendingPick by remember { mutableStateOf<String?>(null) }

  val plansForSelectedDay =
    remember(mealPlans, selectedDay) {
      mealPlans.filter { it.dayOfWeek == selectedDay }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("meal_plan_back_button"),
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        Column {
          Text(
            text = stringResource(R.string.meal_planner_title),
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = stringResource(R.string.meal_planner_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Emerald50,
        border = BorderStroke(1.dp, Emerald100),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Total Menu Terjadwal: ${mealPlans.size} Jadwal",
                style = MaterialTheme.typography.titleSmall,
                color = Emerald950,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Sinkronkan semua bahan yang kurang langsung ke Daftar Belanja dalam satu ketukan.",
                style = MaterialTheme.typography.bodySmall,
                color = Emerald800,
              )
            }
          }
          Button(
            onClick = onSyncWeeklyShopping,
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_weekly_shopping_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.ShoppingCartCheckout,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tambahkan Bahan Kurang ke Belanja", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MealPlanEntry.WEEK_DAYS) { day ->
          val countForDay = mealPlans.count { it.dayOfWeek == day }
          FilterChip(
            selected = selectedDay == day,
            onClick = { selectedDay = day },
            label = {
              Text(if (countForDay > 0) "$day ($countForDay)" else day)
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }
    }

    items(MealPlanEntry.MEAL_SLOTS) { slot ->
      val entry = plansForSelectedDay.firstOrNull { it.mealSlot == slot }
      val matchedRecipe =
        remember(entry, availableRecipes) {
          if (entry == null) null
          else
            availableRecipes.firstOrNull {
              it.id == entry.recipeId || it.title.equals(entry.recipeTitle, ignoreCase = true)
            }
        }
      val liveMatch =
        remember(matchedRecipe, pantryItems) { matchedRecipe?.evaluateMatch(pantryItems) }

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.RestaurantMenu,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "$selectedDay • $slot",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            if (entry != null) {
              IconButton(
                onClick = { onDeleteMeal(entry.id.ifBlank { MealPlanEntry.docId(selectedDay, slot) }) },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription = "Hapus jadwal $slot",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (entry == null) {
            OutlinedButton(
              onClick = { slotPendingPick = slot },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_meal_${slot}"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pilih Resep untuk $slot")
            }
          } else {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = entry.recipeTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                val statusLabel =
                  when {
                    liveMatch != null && liveMatch.matchPercent == 100 ->
                      "Semua bahan tersedia 100% di Smart Pantry"
                    liveMatch != null ->
                      "Kecocokan bahan ${liveMatch.matchPercent}% • Kurang: ${
                        liveMatch.ingredientStates
                          .filter { !it.availableInPantry }
                          .joinToString(", ") { it.ingredient.name }
                      }"
                    entry.missingItemsSummary.isNotBlank() ->
                      "Perlu beli: ${entry.missingItemsSummary}"
                    else -> "${entry.servings} porsi siap dimasak"
                  }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (liveMatch?.matchPercent == 100) Emerald100 else Amber100,
                ) {
                  Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (liveMatch?.matchPercent == 100) Emerald800 else Amber800,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { slotPendingPick = slot },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Text("Ganti Menu")
              }
              Button(
                onClick = { matchedRecipe?.let(onStartCooking) },
                enabled = matchedRecipe != null,
                shape = RoundedCornerShape(12.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Emerald700,
                    contentColor = Color.White,
                  ),
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Masak")
              }
            }
          }
        }
      }
    }
  }

  val pickingSlot = slotPendingPick
  if (pickingSlot != null) {
    PickRecipeForSlotDialog(
      dayOfWeek = selectedDay,
      mealSlot = pickingSlot,
      recipes = availableRecipes,
      pantryItems = pantryItems,
      onDismiss = { slotPendingPick = null },
      onPick = { recipe ->
        onAssignMeal(selectedDay, pickingSlot, recipe)
        slotPendingPick = null
      },
    )
  }
}

@Composable
private fun PickRecipeForSlotDialog(
  dayOfWeek: String,
  mealSlot: String,
  recipes: List<Recipe>,
  pantryItems: List<PantryItem>,
  onDismiss: () -> Unit,
  onPick: (Recipe) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Pilih Menu • $dayOfWeek ($mealSlot)", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        recipes.forEach { recipe ->
          val matchPercent = remember(recipe, pantryItems) { recipe.evaluateMatch(pantryItems).matchPercent }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onPick(recipe) },
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${recipe.durationMinutes} mnt • ${recipe.servings} porsi",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (matchPercent == 100) Emerald100 else Amber100,
              ) {
                Text(
                  text = "$matchPercent%",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (matchPercent == 100) Emerald800 else Amber800,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
fun AssignRecipeToMealPlanDialog(
  recipe: Recipe,
  onDismiss: () -> Unit,
  onConfirm: (dayOfWeek: String, mealSlot: String) -> Unit,
) {
  var selectedDay by remember { mutableStateOf(MealPlanEntry.WEEK_DAYS.first()) }
  var selectedSlot by remember { mutableStateOf(MealPlanEntry.MEAL_SLOTS[1]) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Emerald700)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Jadwalkan Menu", style = MaterialTheme.typography.titleMedium)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Masukkan \"${recipe.title}\" ke jadwal makan mingguan:",
          style = MaterialTheme.typography.bodySmall,
        )
        Text("Pilih Hari:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(MealPlanEntry.WEEK_DAYS) { day ->
            FilterChip(
              selected = selectedDay == day,
              onClick = { selectedDay = day },
              label = { Text(day) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Emerald700,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
        Text("Waktu Makan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(MealPlanEntry.MEAL_SLOTS) { slot ->
            FilterChip(
              selected = selectedSlot == slot,
              onClick = { selectedSlot = slot },
              label = { Text(slot) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Emerald700,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedDay, selectedSlot) },
        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
      ) {
        Text("Jadwalkan")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}
