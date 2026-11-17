package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Chef Alex",
  val email: String = "",
  val dietType: String = "Halal / General",
  val allergies: String = "Kacang Tanah",
  val defaultServings: Int = 2,
  val seededDefaults: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PantryItem(
  @DocumentId val id: String = "",
  val name: String = "",
  val quantity: Int = 0,
  val unit: String = "pcs",
  val stepDelta: Int = 1,
  val location: String = "Kulkas (Fridge)",
  val daysUntilExpiry: Int = 7,
  val category: String = "pantry",
  val lowThreshold: Int = 1,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val expiringSoon: Boolean
    get() = quantity > 0 && daysUntilExpiry <= 3

  val lowStock: Boolean
    get() = quantity <= lowThreshold
}

@Keep
@IgnoreExtraProperties
data class ShoppingItem(
  @DocumentId val id: String = "",
  val name: String = "",
  val checked: Boolean = false,
  val sourceTag: String = "Manual",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class JournalNote(
  @DocumentId val id: String = "",
  val recipeTitle: String = "",
  val note: String = "",
  val rating: Double = 5.0,
  val dateLabel: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedRecipe(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val durationMinutes: Int = 20,
  val servings: Int = 2,
  val difficulty: String = "Mudah",
  val categoryName: String = "NUSANTARA",
  val isCustom: Boolean = false,
  val caloriesKcal: Int = 320,
  val proteinGrams: Int = 22,
  val carbsGrams: Int = 24,
  val fatGrams: Int = 14,
  val ingredientsSerialized: String = "[]",
  val stepsSerialized: String = "[]",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toRecipe(): Recipe {
    val parsedIngredients = mutableListOf<RecipeIngredient>()
    try {
      val arr = org.json.JSONArray(ingredientsSerialized)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        parsedIngredients.add(
          RecipeIngredient(
            name = obj.optString("name", "Bahan"),
            amountLabel = obj.optString("amountLabel", ""),
            pantryMatchKeyword = obj.optString("pantryMatchKeyword", obj.optString("name", "")),
            deductAmount = obj.optInt("deductAmount", 0).coerceAtLeast(0),
            isBasicStaple = obj.optBoolean("isBasicStaple", false),
          )
        )
      }
    } catch (_: Exception) {}

    val parsedSteps = mutableListOf<RecipeStep>()
    try {
      val arr = org.json.JSONArray(stepsSerialized)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val instruction = obj.optString("instruction", "").trim()
        if (instruction.isNotBlank()) {
          parsedSteps.add(
            RecipeStep(
              instruction = instruction,
              durationSeconds = obj.optInt("durationSeconds", 180).coerceIn(30, 3600),
            )
          )
        }
      }
    } catch (_: Exception) {}

    val category =
      ExplorerCategory.entries.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        ?: ExplorerCategory.NUSANTARA

    return Recipe(
      id = id.ifBlank { "saved_${title.hashCode()}" },
      title = title,
      description = description,
      durationMinutes = durationMinutes.coerceIn(1, 360),
      servings = servings.coerceIn(1, 30),
      difficulty = difficulty.ifBlank { "Mudah" },
      category = category,
      isWebVerified = !isCustom,
      isAiGenerated = false,
      isCustom = isCustom,
      nutrition =
        RecipeNutrition(
          caloriesKcal = caloriesKcal,
          proteinGrams = proteinGrams,
          carbsGrams = carbsGrams,
          fatGrams = fatGrams,
        ),
      ingredients = parsedIngredients,
      steps =
        parsedSteps.ifEmpty {
          listOf(RecipeStep("Siapkan semua bahan dan masak hingga matang sempurna.", 180))
        },
    )
  }

  companion object {
    fun buildDocId(recipe: Recipe): String {
      val slug =
        recipe.title
          .lowercase()
          .replace(Regex("[^a-z0-9]+"), "_")
          .trim('_')
          .take(64)
          .ifBlank { "resep" }
      val prefix = if (recipe.isCustom) "custom" else "fav"
      return "${prefix}_${slug}".take(120)
    }

    fun serializeIngredients(ingredients: List<RecipeIngredient>): String {
      val arr = org.json.JSONArray()
      ingredients.forEach { ing ->
        arr.put(
          org.json.JSONObject().apply {
            put("name", ing.name)
            put("amountLabel", ing.amountLabel)
            put("pantryMatchKeyword", ing.pantryMatchKeyword)
            put("deductAmount", ing.deductAmount)
            put("isBasicStaple", ing.isBasicStaple)
          }
        )
      }
      return arr.toString().take(7800)
    }

    fun serializeSteps(steps: List<RecipeStep>): String {
      val arr = org.json.JSONArray()
      steps.forEach { step ->
        arr.put(
          org.json.JSONObject().apply {
            put("instruction", step.instruction)
            put("durationSeconds", step.durationSeconds)
          }
        )
      }
      return arr.toString().take(7800)
    }
  }
}

@Keep
@IgnoreExtraProperties
data class MealPlanEntry(
  @DocumentId val id: String = "",
  val dayOfWeek: String = "Senin",
  val mealSlot: String = "Makan Siang",
  val recipeId: String = "",
  val recipeTitle: String = "",
  val servings: Int = 2,
  val missingItemsSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    val WEEK_DAYS = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val MEAL_SLOTS = listOf("Sarapan", "Makan Siang", "Makan Malam")

    fun docId(dayOfWeek: String, mealSlot: String): String =
      "${dayOfWeek}_${mealSlot}".replace(Regex("[^a-zA-Z0-9]+"), "_")
  }
}

data class RecipeNutrition(
  val caloriesKcal: Int,
  val proteinGrams: Int,
  val carbsGrams: Int,
  val fatGrams: Int,
)

data class RecipeIngredient(
  val name: String,
  val amountLabel: String,
  val pantryMatchKeyword: String,
  val deductAmount: Int,
  val isBasicStaple: Boolean = false,
)

data class RecipeStep(
  val instruction: String,
  val durationSeconds: Int = 180,
)

data class PantryDeduction(
  val ingredientName: String,
  val matchKeyword: String,
  val deductAmount: Int,
  val unit: String,
)

data class Recipe(
  val id: String,
  val title: String,
  val description: String,
  val durationMinutes: Int,
  val servings: Int,
  val difficulty: String,
  val category: ExplorerCategory,
  val isWebVerified: Boolean = true,
  val isAiGenerated: Boolean = false,
  val isCustom: Boolean = false,
  val nutrition: RecipeNutrition? = null,
  val ingredients: List<RecipeIngredient>,
  val substitutionTip: String? = null,
  val missingItemToShop: String? = null,
  val steps: List<RecipeStep>,
) {
  fun resolvedNutrition(): RecipeNutrition {
    if (nutrition != null && nutrition.caloriesKcal > 0) return nutrition
    val proteinRich =
      ingredients.any {
        val kw = it.pantryMatchKeyword.lowercase()
        kw.contains("ayam") || kw.contains("daging") || kw.contains("ikan") || kw.contains("telur")
      }
    val baseCalories =
      when (category) {
        ExplorerCategory.HEALTHY -> 240
        ExplorerCategory.QUICK -> 285
        ExplorerCategory.FRUGAL -> 295
        ExplorerCategory.WESTERN -> 410
        ExplorerCategory.ASIAN -> 340
        ExplorerCategory.NUSANTARA -> 375
        ExplorerCategory.ALL -> 330
      }
    val protein = if (proteinRich) 26 else 12
    val carbs = if (category == ExplorerCategory.HEALTHY) 16 else 32
    val fat = if (category == ExplorerCategory.HEALTHY) 9 else 16
    return RecipeNutrition(
      caloriesKcal = baseCalories,
      proteinGrams = protein,
      carbsGrams = carbs,
      fatGrams = fat,
    )
  }

  fun scaledForServings(targetServings: Int): Recipe {
    val safeTarget = targetServings.coerceIn(1, 20)
    if (safeTarget == servings || servings <= 0) return this
    val factor = safeTarget.toDouble() / servings.toDouble()
    val scaledIngredients =
      ingredients.map { ing ->
        ing.copy(
          name = scaleNumericAmounts(ing.name, factor),
          amountLabel = scaleNumericAmounts(ing.amountLabel, factor),
          deductAmount =
            if (ing.deductAmount > 0) {
              kotlin.math.round(ing.deductAmount * factor).toInt().coerceAtLeast(1)
            } else {
              0
            },
        )
      }
    return copy(servings = safeTarget, ingredients = scaledIngredients)
  }
  fun evaluateMatch(pantryItems: List<PantryItem>): RecipeMatchStatus {
    val activePantry = pantryItems.filter { it.quantity > 0 }
    var requiredCount = 0
    var availableRequiredCount = 0
    var usesExpiringItem = false

    val ingredientStates =
      ingredients.map { ing ->
        val matched =
          activePantry.firstOrNull { item ->
            item.name.contains(ing.pantryMatchKeyword, ignoreCase = true) ||
              ing.pantryMatchKeyword.contains(item.name, ignoreCase = true) ||
              ing.name.contains(item.name, ignoreCase = true)
          }
        val isStaple = ing.isBasicStaple || isUniversalStaple(ing.pantryMatchKeyword)
        if (!isStaple) {
          requiredCount++
          if (matched != null) {
            availableRequiredCount++
          }
        }
        if (matched != null && matched.daysUntilExpiry <= 3) {
          usesExpiringItem = true
        }
        IngredientAvailability(
          ingredient = ing,
          availableInPantry = matched != null || isStaple,
          matchedPantry = matched != null,
          isStapleFallback = matched == null && isStaple,
          pantryItem = matched,
        )
      }

    val percent =
      if (requiredCount == 0) {
        if (activePantry.isEmpty()) 0 else 100
      } else {
        ((availableRequiredCount * 100.0) / requiredCount).toInt().coerceIn(0, 100)
      }

    val deductions =
      ingredientStates
        .filter { it.matchedPantry && it.pantryItem != null && it.ingredient.deductAmount > 0 }
        .map {
          PantryDeduction(
            ingredientName = it.pantryItem!!.name,
            matchKeyword = it.ingredient.pantryMatchKeyword,
            deductAmount = it.ingredient.deductAmount,
            unit = it.pantryItem.unit,
          )
        }

    return RecipeMatchStatus(
      matchPercent = percent,
      isPreventWaste = usesExpiringItem,
      ingredientStates = ingredientStates,
      deductions = deductions,
    )
  }

  private fun isUniversalStaple(keyword: String): Boolean {
    val lower = keyword.trim().lowercase()
    return lower == "air" ||
      lower == "garam" ||
      lower == "gula" ||
      lower == "lada" ||
      lower == "merica" ||
      lower == "minyak goreng" ||
      lower == "minyak"
  }
  private fun scaleNumericAmounts(input: String, factor: Double): String {
    if (input.isBlank() || factor == 1.0) return input
    return Regex("\\b(\\d+(?:\\.\\d+)?)\\b").replace(input) { match ->
      val num = match.groupValues[1].toDoubleOrNull() ?: return@replace match.value
      val scaled = num * factor
      val rounded = kotlin.math.round(scaled * 10.0) / 10.0
      if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
      } else {
        rounded.toString()
      }
    }
  }
}

data class IngredientAvailability(
  val ingredient: RecipeIngredient,
  val availableInPantry: Boolean,
  val matchedPantry: Boolean,
  val isStapleFallback: Boolean,
  val pantryItem: PantryItem?,
)

data class RecipeMatchStatus(
  val matchPercent: Int,
  val isPreventWaste: Boolean,
  val ingredientStates: List<IngredientAvailability>,
  val deductions: List<PantryDeduction>,
)

data class SubstitutionOption(
  val substituteName: String,
  val conversionRatio: String,
  val matchScore: Int,
  val inPantry: Boolean,
  val flavorImpact: String,
)

data class SubstitutionAnalysis(
  val originalIngredient: String,
  val recipeContext: String,
  val summary: String,
  val options: List<SubstitutionOption>,
)

data class ChatMessage(
  val id: String,
  val isFromUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
)

data class ReceiptScanItem(
  val name: String,
  val quantity: Int,
  val unit: String,
  val location: String,
  val daysUntilExpiry: Int,
  val category: String,
)

enum class MainDestination {
  HOME,
  AI_MATCHER,
  EXPLORER,
  SAVED_RECIPES,
  MEAL_PLANNER,
  CHAT,
  PANTRY,
  NOTES,
  PROFILE,
}

enum class NotesTab {
  SHOPPING,
  JOURNAL,
}

enum class RecipeFilter {
  ALL_100,
  PREVENT_WASTE,
}

enum class ExplorerCategory(val label: String) {
  ALL("Semua"),
  NUSANTARA("Nusantara"),
  ASIAN("Asia"),
  WESTERN("Western"),
  QUICK("Cepat <15m"),
  FRUGAL("Hemat"),
  HEALTHY("Sehat"),
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

object StarterCatalog {
  // Guaranteed 100% match recipes with starter pantry (Dada Ayam, Telur Ayam, Bawang Putih, Kecap Manis, Mentega)
  val matcherStarterRecipes: List<Recipe> =
    listOf(
      Recipe(
        id = "ayam_tumis_kecap",
        title = "Ayam Tumis Kecap Bawang",
        description =
          "Mengolah Dada Ayam sebelum kedaluwarsa dengan tumisan bawang putih harum dan kecap manis.",
        durationMinutes = 20,
        servings = 2,
        difficulty = "Mudah",
        category = ExplorerCategory.NUSANTARA,
        ingredients =
          listOf(
            RecipeIngredient("Dada Ayam (300g)", "300 g", "Dada Ayam", 300),
            RecipeIngredient("Bawang Putih (3 siung)", "3 siung", "Bawang Putih", 3),
            RecipeIngredient("Kecap Manis (2 sdm)", "2 sdm", "Kecap Manis", 2),
          ),
        steps =
          listOf(
            RecipeStep(
              "Iris halus 3 siung bawang putih dan potong dada ayam (300g) menjadi dadu sedang.",
              durationSeconds = 180,
            ),
            RecipeStep(
              "Panaskan 2 sdm minyak di wajan, tumis bawang putih hingga harum keemasan.",
              durationSeconds = 120,
            ),
            RecipeStep(
              "Masukkan ayam dan 2 sdm kecap manis. Masak hingga bumbu meresap dan ayam matang sempurna.",
              durationSeconds = 240,
            ),
          ),
      ),
      Recipe(
        id = "ayam_mentega_bawang",
        title = "Ayam Panggang Mentega Bawang",
        description =
          "Olahan dada ayam gurih beraroma mentega dan bawang putih tanpa bahan tambahan lain.",
        durationMinutes = 18,
        servings = 2,
        difficulty = "Mudah",
        category = ExplorerCategory.WESTERN,
        ingredients =
          listOf(
            RecipeIngredient("Dada Ayam (250g)", "250 g", "Dada Ayam", 250),
            RecipeIngredient("Mentega (2 sdm)", "2 sdm", "Mentega", 2),
            RecipeIngredient("Bawang Putih (3 siung)", "3 siung", "Bawang Putih", 3),
          ),
        steps =
          listOf(
            RecipeStep(
              "Iris tipis dada ayam agar cepat matang merata, lalu geprek dan cincang 3 siung bawang putih.",
              durationSeconds = 150,
            ),
            RecipeStep(
              "Lelehkan 2 sdm mentega di wajan api sedang, tumis bawang putih sebentar hingga wangi.",
              durationSeconds = 90,
            ),
            RecipeStep(
              "Masukkan dada ayam, masak 4 menit per sisi sambil disiram lelehan mentega hingga kecokelatan.",
              durationSeconds = 300,
            ),
          ),
      ),
      Recipe(
        id = "omelet_bawang_mentega",
        title = "Omelet Mentega Bawang Harum",
        description =
          "Sarapan lembut super cepat hanya menggunakan telur ayam, mentega, dan bawang putih.",
        durationMinutes = 8,
        servings = 2,
        difficulty = "Sangat Mudah",
        category = ExplorerCategory.QUICK,
        ingredients =
          listOf(
            RecipeIngredient("Telur Ayam (2 pcs)", "2 pcs", "Telur Ayam", 2),
            RecipeIngredient("Mentega (1 sdm)", "1 sdm", "Mentega", 1),
            RecipeIngredient("Bawang Putih (1 siung)", "1 siung", "Bawang Putih", 1),
          ),
        steps =
          listOf(
            RecipeStep(
              "Kocok lepas 2 butir telur ayam dengan sejumput garam hingga berbusa ringan.",
              durationSeconds = 90,
            ),
            RecipeStep(
              "Lelehkan 1 sdm mentega dengan api kecil, tumis 1 siung bawang putih cincang sebentar.",
              durationSeconds = 60,
            ),
            RecipeStep(
              "Tuang kocokan telur, lipat perlahan saat bagian tepi set, lalu angkat selagi lembut.",
              durationSeconds = 120,
            ),
          ),
      ),
      Recipe(
        id = "nasi_telur_kecap",
        title = "Telur Ceplok Kecap Bawang",
        description =
          "Menu hemat favorit hanya 10 menit dengan telur ayam, bawang putih, dan kecap manis.",
        durationMinutes = 10,
        servings = 1,
        difficulty = "Sangat Mudah",
        category = ExplorerCategory.FRUGAL,
        ingredients =
          listOf(
            RecipeIngredient("Telur Ayam (2 pcs)", "2 pcs", "Telur Ayam", 2),
            RecipeIngredient("Bawang Putih (2 siung)", "2 siung", "Bawang Putih", 2),
            RecipeIngredient("Kecap Manis (2 sdm)", "2 sdm", "Kecap Manis", 2),
          ),
        steps =
          listOf(
            RecipeStep(
              "Goreng ceplok 2 butir telur ayam sesuai tingkat kematangan favorit, lalu sisihkan.",
              durationSeconds = 150,
            ),
            RecipeStep(
              "Tumis 2 siung bawang putih cincang dengan sedikit minyak hingga wangi.",
              durationSeconds = 90,
            ),
            RecipeStep(
              "Tambahkan 2 sdm kecap manis dan 3 sdm air, aduk bersama telur hingga saus mengental.",
              durationSeconds = 120,
            ),
          ),
      ),
    )

  // Broader global / internet-inspired Explorer catalog
  val worldExplorerRecipes: List<Recipe> =
    matcherStarterRecipes +
      listOf(
        Recipe(
          id = "exp_rendang_ayam",
          title = "Rendang Ayam Rempah Padang",
          description = "Hidangan klasik Nusantara dengan santan gurih, serai, lengkuas, dan bumbu rendang kaya rasa.",
          durationMinutes = 45,
          servings = 4,
          difficulty = "Sedang",
          category = ExplorerCategory.NUSANTARA,
          ingredients =
            listOf(
              RecipeIngredient("Dada Ayam (400g)", "400 g", "Dada Ayam", 400),
              RecipeIngredient("Bawang Putih (4 siung)", "4 siung", "Bawang Putih", 4),
              RecipeIngredient("Santan Kental (200ml)", "200 ml", "Santan", 200),
              RecipeIngredient("Serai & Daun Jeruk", "2 batang", "Serai", 2),
            ),
          missingItemToShop = "Santan Kental & Serai",
          steps =
            listOf(
              RecipeStep("Haluskan bawang putih, bawang merah, cabai, kunyit, dan jahe lalu tumis bersama serai.", 240),
              RecipeStep("Masukkan potongan ayam, aduk hingga berubah warna dan bumbu meresap.", 300),
              RecipeStep("Tuang santan, masak dengan api kecil sambil diaduk hingga kuah mengental berminyak.", 900),
            ),
        ),
        Recipe(
          id = "exp_teriyaki_chicken",
          title = "Chicken Teriyaki Ala Jepang",
          description = "Irisan dada ayam glazed kecap manis, bawang putih, dan jahe ala bento Jepang.",
          durationMinutes = 20,
          servings = 2,
          difficulty = "Mudah",
          category = ExplorerCategory.ASIAN,
          ingredients =
            listOf(
              RecipeIngredient("Dada Ayam (300g)", "300 g", "Dada Ayam", 300),
              RecipeIngredient("Kecap Manis (3 sdm)", "3 sdm", "Kecap Manis", 3),
              RecipeIngredient("Bawang Putih (2 siung)", "2 siung", "Bawang Putih", 2),
              RecipeIngredient("Jahe Parut (1 sdt)", "1 sdt", "Jahe", 1),
            ),
          missingItemToShop = "Jahe Segar",
          steps =
            listOf(
              RecipeStep("Lumuri potongan dada ayam dengan kecap manis, bawang putih parut, dan jahe selama 10 menit.", 180),
              RecipeStep("Panaskan wajan dengan sedikit minyak, panggang ayam hingga sisi bawah kecokelatan.", 240),
              RecipeStep("Tuang sisa saus marinasi, masak hingga saus mengental mengkilap menyelimuti ayam.", 180),
            ),
        ),
        Recipe(
          id = "exp_korean_egg_roll",
          title = "Gyeran Mari (Telur Gulung Korea)",
          description = "Telur dadar gulung tebal lembut ala Korea dengan irisan sayuran warna-warni.",
          durationMinutes = 12,
          servings = 2,
          difficulty = "Mudah",
          category = ExplorerCategory.ASIAN,
          ingredients =
            listOf(
              RecipeIngredient("Telur Ayam (4 pcs)", "4 pcs", "Telur Ayam", 4),
              RecipeIngredient("Daun Bawang (1 batang)", "1 batang", "Daun Bawang", 1),
              RecipeIngredient("Wortel Cincang (2 sdm)", "2 sdm", "Wortel", 2),
            ),
          missingItemToShop = "Daun Bawang & Wortel",
          steps =
            listOf(
              RecipeStep("Kocok 4 butir telur bersama wortel cincang halus, daun bawang, dan sejumput garam.", 120),
              RecipeStep("Tuang 1/3 adonan ke teflon api kecil, gulung dari satu sisi sebelum atasnya kering.", 150),
              RecipeStep("Tuang sisa adonan bertahap sambil menggulung hingga tebal, lalu potong-potong rapi.", 180),
            ),
        ),
        Recipe(
          id = "exp_garlic_butter_pasta",
          title = "Aglio Olio Mentega Bawang",
          description = "Pasta Italia klasik cepat saji dengan aroma bawang putih tumis mentega dan cabai.",
          durationMinutes = 15,
          servings = 2,
          difficulty = "Mudah",
          category = ExplorerCategory.WESTERN,
          ingredients =
            listOf(
              RecipeIngredient("Spaghetti (200g)", "200 g", "Spaghetti", 200),
              RecipeIngredient("Bawang Putih (4 siung)", "4 siung", "Bawang Putih", 4),
              RecipeIngredient("Mentega (2 sdm)", "2 sdm", "Mentega", 2),
            ),
          missingItemToShop = "Spaghetti (200g)",
          steps =
            listOf(
              RecipeStep("Rebus spaghetti dalam air mendidih bergaram hingga al dente, sisihkan sedikit air rebusan.", 480),
              RecipeStep("Tumis irisan tipis 4 siung bawang putih dengan 2 sdm mentega di api kecil hingga harum.", 120),
              RecipeStep("Masukkan pasta dan 3 sdm air rebusan pasta, aduk cepat hingga saus menyatu.", 120),
            ),
        ),
        Recipe(
          id = "exp_sup_ayam_jahe",
          title = "Sup Ayam Jahe Bening Sehat",
          description = "Kuah kaldu ringan rendah lemak dengan bawang putih geprek dan kehangatan jahe.",
          durationMinutes = 25,
          servings = 2,
          difficulty = "Mudah",
          category = ExplorerCategory.HEALTHY,
          ingredients =
            listOf(
              RecipeIngredient("Dada Ayam (200g)", "200 g", "Dada Ayam", 200),
              RecipeIngredient("Bawang Putih (2 siung)", "2 siung", "Bawang Putih", 2),
              RecipeIngredient("Jahe Segar (2 cm)", "2 cm", "Jahe", 1),
            ),
          missingItemToShop = "Jahe Segar",
          steps =
            listOf(
              RecipeStep("Potong dadu 200g dada ayam dan geprek 2 siung bawang putih serta jahe.", 150),
              RecipeStep("Didihkan 600 ml air, masukkan ayam, bawang putih, dan jahe dengan api kecil.", 420),
              RecipeStep("Bumbui garam dan lada secukupnya, masak 5 menit lalu sajikan hangat.", 300),
            ),
        ),
        Recipe(
          id = "exp_french_toast",
          title = "Roti Panggang Telur Mentega",
          description = "French toast klasik keemasan dengan campuran telur kocok dan mentega wangi.",
          durationMinutes = 10,
          servings = 2,
          difficulty = "Sangat Mudah",
          category = ExplorerCategory.QUICK,
          ingredients =
            listOf(
              RecipeIngredient("Roti Tawar (4 lembar)", "4 lembar", "Roti", 4),
              RecipeIngredient("Telur Ayam (2 pcs)", "2 pcs", "Telur Ayam", 2),
              RecipeIngredient("Mentega (1 sdm)", "1 sdm", "Mentega", 1),
            ),
          missingItemToShop = "Roti Tawar",
          steps =
            listOf(
              RecipeStep("Kocok 2 telur bersama 2 sdm susu atau air dan sedikit gula atau garam.", 90),
              RecipeStep("Celupkan roti tawar ke adonan telur hingga merata kedua sisinya.", 60),
              RecipeStep("Panggang di atas teflon beroles 1 sdm mentega hingga kedua sisi cokelat keemasan.", 180),
            ),
        ),
        Recipe(
          id = "exp_soto_ayam_rumahan",
          title = "Soto Ayam Kuah Kuning Rumahan",
          description = "Soto ayam gurih segar dengan kunyit, bawang putih, dan suwiran dada ayam.",
          durationMinutes = 35,
          servings = 4,
          difficulty = "Sedang",
          category = ExplorerCategory.NUSANTARA,
          ingredients =
            listOf(
              RecipeIngredient("Dada Ayam (300g)", "300 g", "Dada Ayam", 300),
              RecipeIngredient("Bawang Putih (4 siung)", "4 siung", "Bawang Putih", 4),
              RecipeIngredient("Kunyit & Serai", "1 paket", "Kunyit", 1),
            ),
          missingItemToShop = "Kunyit & Serai",
          steps =
            listOf(
              RecipeStep("Rebus dada ayam hingga empuk untuk membuat kaldu, angkat ayam lalu suwir-suwir.", 600),
              RecipeStep("Tumis bawang putih halus, kunyit, dan serai hingga harum lalu masukkan ke panci kaldu.", 240),
              RecipeStep("Sajikan kuah soto panas dengan suwiran ayam dan perasan jeruk nipis.", 120),
            ),
        ),
        Recipe(
          id = "exp_omelet_keju_bayam",
          title = "Omelet Bayam Tinggi Protein",
          description = "Menu sarapan sehat rendah karbohidrat memadukan telur, bayam segar, dan bawang.",
          durationMinutes = 10,
          servings = 1,
          difficulty = "Sangat Mudah",
          category = ExplorerCategory.HEALTHY,
          ingredients =
            listOf(
              RecipeIngredient("Telur Ayam (2 pcs)", "2 pcs", "Telur Ayam", 2),
              RecipeIngredient("Bayam Segar (1 genggam)", "1 genggam", "Bayam", 1),
              RecipeIngredient("Mentega (1 sdm)", "1 sdm", "Mentega", 1),
            ),
          missingItemToShop = "Bayam Segar",
          steps =
            listOf(
              RecipeStep("Tumis sebentar daun bayam dengan 1 sdm mentega hingga layu.", 90),
              RecipeStep("Tuang kocokan 2 telur ayam ke atas bayam dengan api kecil.", 120),
              RecipeStep("Lipat omelet dan sajikan selagi hangat.", 60),
            ),
        ),
      )
}
