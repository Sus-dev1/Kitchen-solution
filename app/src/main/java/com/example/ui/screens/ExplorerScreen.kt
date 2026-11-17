package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExplorerCategory
import com.example.model.PantryItem
import com.example.model.Recipe
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald950

@Composable
fun ExplorerScreen(
  recipes: List<Recipe>,
  pantryItems: List<PantryItem>,
  savedRecipeTitles: Set<String>,
  selectedCategory: ExplorerCategory,
  searchQuery: String,
  aiMatchedIds: Set<String>,
  isAiSearching: Boolean,
  aiErrorMessage: String?,
  onSelectCategory: (ExplorerCategory) -> Unit,
  onQueryChange: (String) -> Unit,
  onSearchWithAi: () -> Unit,
  onBack: () -> Unit,
  onAddShoppingItem: (String) -> Unit,
  onStartCooking: (Recipe) -> Unit,
  onAskSubstitution: (String, String) -> Unit,
  onToggleBookmark: (Recipe) -> Unit,
  onScheduleMeal: (Recipe) -> Unit,
) {
  val filtered =
    remember(recipes, selectedCategory, searchQuery, aiMatchedIds) {
      recipes.filter { recipe ->
        if (recipe.id in aiMatchedIds) {
          selectedCategory == ExplorerCategory.ALL || recipe.category == selectedCategory
        } else {
          val matchesCategory =
            selectedCategory == ExplorerCategory.ALL || recipe.category == selectedCategory
          val matchesQuery =
            searchQuery.isBlank() ||
              recipe.title.contains(searchQuery, ignoreCase = true) ||
              recipe.description.contains(searchQuery, ignoreCase = true) ||
              recipe.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }
          matchesCategory && matchesQuery
        }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Kembali",
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
          Text(
            text = stringResource(R.string.explorer_title),
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = stringResource(R.string.explorer_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onQueryChange,
          placeholder = { Text("Cari resep apa saja (Rendang, Ramen, Carbonara...)") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSearchWithAi() }),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("explorer_search_input"),
        )

        Button(
          onClick = onSearchWithAi,
          enabled = !isAiSearching,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("explorer_ai_search_button"),
        ) {
          if (isAiSearching) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mencari Resep Dunia dengan Gemini AI...")
          } else {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (searchQuery.isBlank()) "Jelajahi Resep Dunia Baru dengan AI"
              else "Cari \"$searchQuery\" dari Resep Global AI"
            )
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
        items(ExplorerCategory.entries) { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { onSelectCategory(category) },
            label = { Text(category.label) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Emerald50,
          border = BorderStroke(1.dp, Emerald100),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Filled.Public, contentDescription = null, tint = Emerald700, modifier = Modifier.size(32.dp))
            Text(
              text = "Resep \"$searchQuery\" belum ada di daftar lokal.",
              style = MaterialTheme.typography.titleSmall,
              color = Emerald950,
            )
            Text(
              text = "Tekan tombol di bawah untuk mengambil resep lengkap langsung dari Gemini AI.",
              style = MaterialTheme.typography.bodySmall,
              color = Emerald950,
            )
            OutlinedButton(
              onClick = onSearchWithAi,
              enabled = !isAiSearching,
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Cari \"$searchQuery\" Sekarang")
            }
          }
        }
      }
    } else {
      items(filtered, key = { it.id }) { recipe ->
        val match = remember(recipe, pantryItems) { recipe.evaluateMatch(pantryItems) }
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

      item {
        OutlinedButton(
          onClick = onSearchWithAi,
          enabled = !isAiSearching,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Muat Lebih Banyak Resep ${selectedCategory.label} via AI")
        }
      }
    }
  }
}
