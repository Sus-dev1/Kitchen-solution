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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExplorerCategory
import com.example.model.PantryItem
import com.example.model.Recipe
import com.example.model.SavedRecipe
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald950

private enum class SavedRecipeFilter(val label: String) {
  ALL("Semua"),
  BOOKMARKS("Disimpan"),
  CUSTOM("Resep Sendiri"),
}

@Composable
fun SavedRecipesScreen(
  savedRecipes: List<SavedRecipe>,
  pantryItems: List<PantryItem>,
  onBack: () -> Unit,
  onAddShoppingItem: (String) -> Unit,
  onStartCooking: (Recipe) -> Unit,
  onAskSubstitution: (String, String) -> Unit,
  onToggleBookmark: (Recipe) -> Unit,
  onScheduleMeal: (Recipe) -> Unit,
  onCreateCustomRecipe:
    (
      title: String,
      description: String,
      durationMinutes: Int,
      servings: Int,
      category: ExplorerCategory,
      caloriesKcal: Int,
      ingredientsText: String,
      stepsText: String,
    ) -> Unit,
) {
  var activeFilter by remember { mutableStateOf(SavedRecipeFilter.ALL) }
  var showCreateDialog by remember { mutableStateOf(false) }

  val filteredModels =
    remember(savedRecipes, activeFilter) {
      val base =
        when (activeFilter) {
          SavedRecipeFilter.ALL -> savedRecipes
          SavedRecipeFilter.BOOKMARKS -> savedRecipes.filter { !it.isCustom }
          SavedRecipeFilter.CUSTOM -> savedRecipes.filter { it.isCustom }
        }
      base.map { it.toRecipe() }
    }

  val countAll = savedRecipes.size
  val countBookmarks = savedRecipes.count { !it.isCustom }
  val countCustom = savedRecipes.count { it.isCustom }

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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("saved_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Kembali",
            )
          }
          Column {
            Text(
              text = stringResource(R.string.saved_recipes_title),
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = stringResource(R.string.saved_recipes_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Emerald50,
        border = BorderStroke(1.dp, Emerald100),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Punya resep andalan keluarga?",
              style = MaterialTheme.typography.titleSmall,
              color = Emerald950,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Tulis resep pribadi Anda lengkap dengan bahan & langkah untuk Mode Masak suara.",
              style = MaterialTheme.typography.bodySmall,
              color = Emerald700,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { showCreateDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
            modifier = Modifier.height(48.dp).testTag("create_custom_recipe_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Buat Resep")
          }
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = activeFilter == SavedRecipeFilter.ALL,
            onClick = { activeFilter = SavedRecipeFilter.ALL },
            label = { Text("Semua ($countAll)") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
          )
        }
        item {
          FilterChip(
            selected = activeFilter == SavedRecipeFilter.BOOKMARKS,
            onClick = { activeFilter = SavedRecipeFilter.BOOKMARKS },
            label = { Text("Disimpan ($countBookmarks)") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
          )
        }
        item {
          FilterChip(
            selected = activeFilter == SavedRecipeFilter.CUSTOM,
            onClick = { activeFilter = SavedRecipeFilter.CUSTOM },
            label = { Text("Resep Sendiri ($countCustom)") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = Emerald700,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }
    }

    if (filteredModels.isEmpty()) {
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
              imageVector =
                if (activeFilter == SavedRecipeFilter.CUSTOM) Icons.Filled.MenuBook
                else Icons.Filled.Bookmark,
              contentDescription = null,
              tint = Emerald700,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text =
                if (activeFilter == SavedRecipeFilter.CUSTOM) "Belum ada resep buatan sendiri"
                else "Belum ada resep favorit tersimpan",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text =
                "Ketuk ikon bookmark di kartu resep AI Matcher / Explorer atau buat resep pribadi baru.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = { showCreateDialog = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Buat Resep Sendiri")
            }
          }
        }
      }
    } else {
      items(filteredModels, key = { it.id }) { recipe ->
        val match = remember(recipe, pantryItems) { recipe.evaluateMatch(pantryItems) }
        RecipeMatcherCard(
          recipe = recipe,
          pantryItems = pantryItems,
          matchPercent = match.matchPercent,
          isPreventWaste = match.isPreventWaste,
          ingredientStates = match.ingredientStates,
          isBookmarked = true,
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

  if (showCreateDialog) {
    CreateCustomRecipeDialog(
      onDismiss = { showCreateDialog = false },
      onCreate = { title, desc, durationMins, servings, category, calories, ingredientsText, stepsText ->
        onCreateCustomRecipe(
          title,
          desc,
          durationMins,
          servings,
          category,
          calories,
          ingredientsText,
          stepsText,
        )
        showCreateDialog = false
      },
    )
  }
}

@Composable
fun CreateCustomRecipeDialog(
  onDismiss: () -> Unit,
  onCreate:
    (
      title: String,
      description: String,
      durationMinutes: Int,
      servings: Int,
      category: ExplorerCategory,
      caloriesKcal: Int,
      ingredientsText: String,
      stepsText: String,
    ) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var durationText by remember { mutableStateOf("25") }
  var servingsText by remember { mutableStateOf("2") }
  var caloriesText by remember { mutableStateOf("350") }
  var selectedCategory by remember { mutableStateOf(ExplorerCategory.NUSANTARA) }
  var ingredientsText by remember {
    mutableStateOf("Dada Ayam (300g)\nBawang Putih (3 siung)\nKecap Manis (2 sdm)")
  }
  var stepsText by remember {
    mutableStateOf(
      "Potong dadu dada ayam dan cincang bawang putih.\nTumis bawang putih hingga harum keemasan.\nMasukkan ayam dan kecap manis, masak hingga matang sempurna."
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Buat Resep Keluarga", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Judul Masakan *") },
          placeholder = { Text("Contoh: Sayur Sop Nenek") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("custom_recipe_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Deskripsi Singkat") },
          placeholder = { Text("Kuah kaldu hangat gurih cocok untuk makan malam...") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Durasi (mnt)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = servingsText,
            onValueChange = { servingsText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Porsi") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = caloriesText,
            onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Kkal/Porsi") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val categories = ExplorerCategory.entries.filter { it != ExplorerCategory.ALL }
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Emerald700,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
        OutlinedTextField(
          value = ingredientsText,
          onValueChange = { ingredientsText = it },
          label = { Text("Bahan (1 bahan per baris)") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("custom_recipe_ingredients_input"),
        )
        OutlinedTextField(
          value = stepsText,
          onValueChange = { stepsText = it },
          label = { Text("Langkah Masak (1 langkah per baris)") },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("custom_recipe_steps_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(
            title,
            description,
            durationText.toIntOrNull() ?: 25,
            servingsText.toIntOrNull() ?: 2,
            selectedCategory,
            caloriesText.toIntOrNull() ?: 350,
            ingredientsText,
            stepsText,
          )
        },
        enabled = title.isNotBlank() && ingredientsText.isNotBlank() && stepsText.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
        modifier = Modifier.testTag("custom_recipe_submit_button"),
      ) {
        Text("Simpan Resep")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}
