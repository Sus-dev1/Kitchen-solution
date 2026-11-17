package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.shareRecipe
import com.example.R
import com.example.model.IngredientAvailability
import com.example.model.PantryItem
import com.example.model.Recipe
import com.example.model.RecipeFilter
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose800

@Composable
fun AiMatcherScreen(
  recipes: List<Recipe>,
  pantryItems: List<PantryItem>,
  savedRecipeTitles: Set<String>,
  activeFilter: RecipeFilter,
  isRefreshing: Boolean,
  aiErrorMessage: String?,
  onSelectFilter: (RecipeFilter) -> Unit,
  onRefreshAi: () -> Unit,
  onAddShoppingItem: (String) -> Unit,
  onStartCooking: (Recipe) -> Unit,
  onAskSubstitution: (String, String) -> Unit,
  onToggleBookmark: (Recipe) -> Unit,
  onScheduleMeal: (Recipe) -> Unit,
) {
  // STRICT RULE: Only show recipes with 100% pantry match in AI Recipe Matcher
  val evaluatedFullMatch =
    remember(recipes, pantryItems) {
      recipes
        .map { recipe -> recipe to recipe.evaluateMatch(pantryItems) }
        .filter { (_, status) -> status.matchPercent == 100 }
        .sortedByDescending { (_, status) -> if (status.isPreventWaste) 1 else 0 }
    }

  val countAll100 = evaluatedFullMatch.size
  val countWaste = evaluatedFullMatch.count { it.second.isPreventWaste }

  val filteredRecipes =
    remember(evaluatedFullMatch, activeFilter) {
      when (activeFilter) {
        RecipeFilter.ALL_100 -> evaluatedFullMatch
        RecipeFilter.PREVENT_WASTE -> evaluatedFullMatch.filter { it.second.isPreventWaste }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = stringResource(R.string.ai_matcher_title),
              style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = Emerald600,
              modifier = Modifier.size(18.dp),
            )
          }
          Text(
            text = stringResource(R.string.ai_matcher_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(shape = CircleShape, color = Emerald100) {
          IconButton(
            onClick = onRefreshAi,
            enabled = !isRefreshing,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("ai_refresh_button"),
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Emerald700,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Cari Menu 100% Cocok Lainnya",
                tint = Emerald700,
              )
            }
          }
        }
      }
    }

    if (aiErrorMessage != null) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = aiErrorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = activeFilter == RecipeFilter.ALL_100,
            onClick = { onSelectFilter(RecipeFilter.ALL_100) },
            label = { Text("100% Match ($countAll100)") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
            modifier = Modifier.testTag("filter_100_all"),
          )
        }
        item {
          FilterChip(
            selected = activeFilter == RecipeFilter.PREVENT_WASTE,
            onClick = { onSelectFilter(RecipeFilter.PREVENT_WASTE) },
            label = { Text("Cegah Waste 100% ($countWaste)") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
            modifier = Modifier.testTag("filter_waste"),
          )
        }
      }
    }

    if (filteredRecipes.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = Emerald600,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text =
                if (isRefreshing) "Gemini AI sedang meracik menu 100% cocok..."
                else "Belum ada menu 100% cocok di filter ini",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text =
                if (pantryItems.none { it.quantity > 0 })
                  "Stok Smart Pantry Anda masih kosong. Tambahkan bahan dulu agar AI bisa menyarankan menu yang 100% siap masak."
                else
                  "Ketuk tombol di bawah agar Gemini AI membuatkan variasi menu baru yang 100% sesuai stok Anda saat ini.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pantryItems.any { it.quantity > 0 }) {
              Button(
                onClick = onRefreshAi,
                enabled = !isRefreshing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
              ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cari Menu 100% Cocok")
              }
            }
          }
        }
      }
    } else {
      items(filteredRecipes, key = { it.first.id }) { (recipe, match) ->
        RecipeMatcherCard(
          recipe = recipe,
          pantryItems = pantryItems,
          matchPercent = match.matchPercent,
          isPreventWaste = match.isPreventWaste,
          ingredientStates = match.ingredientStates,
          isBookmarked = recipe.title.trim().lowercase() in savedRecipeTitles,
          onAddShoppingItem = onAddShoppingItem,
          onStartCooking = onStartCooking,
          onAskSubstitution = { ingredient -> onAskSubstitution(ingredient, recipe.title) },
          onToggleBookmark = { onToggleBookmark(recipe) },
          onScheduleMeal = { onScheduleMeal(recipe) },
          modifier = Modifier.animateItem(),
        )
      }
    }
  }
}

@Composable
fun RecipeMatcherCard(
  recipe: Recipe,
  pantryItems: List<PantryItem> = emptyList(),
  matchPercent: Int,
  isPreventWaste: Boolean,
  ingredientStates: List<IngredientAvailability>,
  isBookmarked: Boolean = false,
  onAddShoppingItem: (String) -> Unit,
  onStartCooking: (Recipe) -> Unit,
  onAskSubstitution: (String) -> Unit,
  onToggleBookmark: (() -> Unit)? = null,
  onScheduleMeal: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedServings by remember(recipe.id, recipe.servings) { mutableIntStateOf(recipe.servings) }
  val scaledRecipe =
    remember(recipe, selectedServings) { recipe.scaledForServings(selectedServings) }
  val activeIngredientStates =
    remember(scaledRecipe, pantryItems, ingredientStates) {
      if (selectedServings == recipe.servings || pantryItems.isEmpty()) ingredientStates
      else scaledRecipe.evaluateMatch(pantryItems).ingredientStates
    }
  val nutrition = remember(recipe) { recipe.resolvedNutrition() }
  val isFullMatch = matchPercent == 100

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = CircleShape,
            color = if (isFullMatch) Emerald100 else Amber100,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (isFullMatch) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isFullMatch) Emerald700 else Amber800,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$matchPercent% MATCH",
                style = MaterialTheme.typography.labelSmall,
                color = if (isFullMatch) Emerald800 else Amber800,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          if (isPreventWaste) {
            Surface(shape = CircleShape, color = Rose100) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Eco,
                  contentDescription = null,
                  tint = Rose500,
                  modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "CEGAH WASTE",
                  style = MaterialTheme.typography.labelSmall,
                  color = Rose800,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (recipe.isAiGenerated) Icons.Filled.AutoAwesome else Icons.Filled.Public,
            contentDescription = null,
            tint = if (recipe.isAiGenerated) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text =
              when {
                recipe.isCustom -> "Resep Saya"
                recipe.isAiGenerated -> "Gemini AI"
                else -> recipe.category.label
              },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = recipe.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (onToggleBookmark != null) {
              IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector =
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                  contentDescription = if (isBookmarked) "Hapus dari Favorit" else "Simpan ke Favorit",
                  tint = if (isBookmarked) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            if (onScheduleMeal != null) {
              IconButton(
                onClick = onScheduleMeal,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Filled.CalendarMonth,
                  contentDescription = "Jadwalkan ke Meal Plan",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(19.dp),
                )
              }
            }
            IconButton(
              onClick = { shareRecipe(context, scaledRecipe) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Bagikan Resep",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        Text(
          text = recipe.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Emerald600, modifier = Modifier.size(14.dp))
            Text("${recipe.durationMinutes} Mins", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(recipe.difficulty, style = MaterialTheme.typography.labelMedium, color = Emerald700)
          }

          // Inline Dynamic Portion Scaler (- N Porsi +)
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
              IconButton(
                onClick = { if (selectedServings > 1) selectedServings-- },
                modifier = Modifier.size(28.dp),
              ) {
                Icon(Icons.Filled.Remove, contentDescription = "Kurangi porsi", modifier = Modifier.size(14.dp))
              }
              Icon(Icons.Filled.Group, contentDescription = null, tint = Emerald600, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "$selectedServings Porsi",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
              IconButton(
                onClick = { if (selectedServings < 20) selectedServings++ },
                modifier = Modifier.size(28.dp),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah porsi", modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        // Nutrition Summary Badge Strip
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Nutrisi/porsi: ~${nutrition.caloriesKcal} kkal • ${nutrition.proteinGrams}g Protein • ${nutrition.carbsGrams}g Karbo • ${nutrition.fatGrams}g Lemak",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          val missingColor = if (isSystemInDarkTheme()) Amber500 else Amber800
          Text(
            text = if (isFullMatch) "Bahan Siap Masak:" else "Bahan Terpakai & Kurang:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          activeIngredientStates.forEach { state ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                  imageVector = if (state.availableInPantry) Icons.Filled.Check else Icons.Filled.Close,
                  contentDescription = null,
                  tint = if (state.availableInPantry) Emerald600 else missingColor,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = state.ingredient.name,
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (state.availableInPantry) MaterialTheme.colorScheme.onSurface
                    else missingColor,
                )
              }
              val pantry = state.pantryItem
              if (pantry != null && pantry.expiringSoon) {
                Surface(shape = RoundedCornerShape(6.dp), color = Rose100) {
                  Text(
                    text = "H-${pantry.daysUntilExpiry} Expired!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Rose800,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              } else if (state.isStapleFallback) {
                Surface(shape = RoundedCornerShape(6.dp), color = Emerald100) {
                  Text(
                    text = "Bumbu Dasar",
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald800,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
          }
        }
      }

      if (!recipe.substitutionTip.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Emerald50,
          border = BorderStroke(1.dp, Emerald100),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Filled.Lightbulb,
              contentDescription = null,
              tint = Emerald600,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Tips Chef AI:",
                style = MaterialTheme.typography.labelMedium,
                color = Emerald800,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = recipe.substitutionTip,
                style = MaterialTheme.typography.bodySmall,
                color = Emerald950,
              )
            }
          }
        }
      }

      val missingItem =
        recipe.missingItemToShop
          ?: ingredientStates.firstOrNull { !it.availableInPantry }?.ingredient?.name

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (missingItem != null && !isFullMatch) {
          OutlinedButton(
            onClick = { onAddShoppingItem(missingItem) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AddShoppingCart,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Belanja", style = MaterialTheme.typography.labelMedium)
          }
        } else {
          OutlinedButton(
            onClick = { onAskSubstitution(recipe.ingredients.firstOrNull()?.name ?: recipe.title) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Substitusi", style = MaterialTheme.typography.labelMedium)
          }
        }

        Button(
          onClick = { onStartCooking(scaledRecipe) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
          modifier = Modifier.weight(1.2f).height(48.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFullMatch) "Mulai Masak" else "Tetap Masak",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
