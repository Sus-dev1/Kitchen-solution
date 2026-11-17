package com.example

import com.example.model.PantryItem
import com.example.model.RecipeIngredient
import com.example.model.RecipeStep
import com.example.model.Recipe
import com.example.model.ExplorerCategory
import com.example.model.ShoppingItem
import com.example.model.StarterCatalog
import com.example.ui.components.CookingActionType
import com.example.ui.components.CookingVoiceCommand
import com.example.ui.components.buildHumanoidCookingSegments
import com.example.ui.components.buildRecipeShareText
import com.example.ui.components.buildShoppingShareText
import com.example.ui.components.detectCookingAction
import com.example.ui.components.formatStepForNaturalSpeech
import com.example.ui.components.parseCookingVoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private val samplePantry =
    listOf(
      PantryItem(
        id = "1",
        name = "Dada Ayam",
        quantity = 300,
        unit = "g",
        daysUntilExpiry = 2,
      ),
      PantryItem(
        id = "2",
        name = "Telur Ayam",
        quantity = 4,
        unit = "pcs",
        daysUntilExpiry = 12,
      ),
      PantryItem(
        id = "3",
        name = "Bawang Putih",
        quantity = 5,
        unit = "siung",
        daysUntilExpiry = 20,
      ),
      PantryItem(
        id = "4",
        name = "Kecap Manis",
        quantity = 8,
        unit = "sdm",
        daysUntilExpiry = 90,
      ),
      PantryItem(
        id = "5",
        name = "Mentega",
        quantity = 4,
        unit = "sdm",
        daysUntilExpiry = 30,
      ),
    )

  @Test
  fun starterCatalog_alwaysProvidesMultiple100PercentMatches() {
    val fullMatches =
      StarterCatalog.matcherStarterRecipes.count {
        it.evaluateMatch(samplePantry).matchPercent == 100
      }
    assertTrue("At least 2 starter recipes must match 100%", fullMatches >= 2)
  }

  @Test
  fun ayamTumis_matches100PercentAndPreventsWaste() {
    val ayamTumis = StarterCatalog.matcherStarterRecipes.first { it.id == "ayam_tumis_kecap" }
    val status = ayamTumis.evaluateMatch(samplePantry)

    assertEquals(100, status.matchPercent)
    assertTrue(status.isPreventWaste)
    assertEquals(3, status.deductions.size)
  }

  @Test
  fun universalStapleFallback_doesNotBreak100PercentMatch() {
    val recipeWithStaple =
      Recipe(
        id = "test_staple",
        title = "Telur Dadar Bawang",
        description = "Test recipe with garam and minyak",
        durationMinutes = 5,
        servings = 1,
        difficulty = "Sangat Mudah",
        category = ExplorerCategory.QUICK,
        ingredients =
          listOf(
            RecipeIngredient("Telur Ayam (2 pcs)", "2 pcs", "Telur Ayam", 2),
            RecipeIngredient("Garam (1/4 sdt)", "1/4 sdt", "Garam", 0, isBasicStaple = true),
            RecipeIngredient("Minyak Goreng (1 sdm)", "1 sdm", "Minyak Goreng", 0),
          ),
        steps = listOf(RecipeStep("Kocok telur dan goreng.", 120)),
      )

    val status = recipeWithStaple.evaluateMatch(samplePantry)
    assertEquals(100, status.matchPercent)
    assertEquals(1, status.deductions.size)
  }

  @Test
  fun worldExplorer_missingIngredientsDropBelow100Percent() {
    val rendang = StarterCatalog.worldExplorerRecipes.first { it.id == "exp_rendang_ayam" }
    val status = rendang.evaluateMatch(samplePantry)

    assertTrue(status.matchPercent < 100)
    assertFalse(status.deductions.any { it.ingredientName == "Santan" })
  }

  @Test
  fun detectCookingAction_classifiesStepsAccurately() {
    assertEquals(CookingActionType.CHOPPING, detectCookingAction("Iris tipis bawang putih dan potong dadu ayam"))
    assertEquals(CookingActionType.FRYING, detectCookingAction("Panaskan minyak di wajan lalu tumis bumbu"))
    assertEquals(CookingActionType.BOILING, detectCookingAction("Rebus kaldu ayam di panci hingga mendidih"))
    assertEquals(CookingActionType.MIXING, detectCookingAction("Kocok telur dan aduk rata bersama tepung"))
    assertEquals(CookingActionType.BAKING, detectCookingAction("Panggang adonan di oven bersuhu 180°C"))
    assertEquals(CookingActionType.SERVING, detectCookingAction("Sajikan hangat bersama nasi putih"))
  }

  @Test
  fun buildHumanoidCookingSegments_appliesFillersPausesIntonationAndReminder() {
    val segments =
      buildHumanoidCookingSegments(
        stepIndex = 0,
        totalSteps = 3,
        rawInstruction = "Panaskan 2 sdm minyak lalu tumis 300g ayam.",
      )

    assertTrue("Should produce intro + action clauses + reminder", segments.size >= 3)
    assertTrue(segments.first().text.lowercase().contains("oke"))
    assertTrue(segments.first().text.lowercase().contains("langkah pertama"))
    assertTrue(segments.first().pauseAfterMs >= 200L)

    val distinctPitches = segments.map { it.pitchDelta }.distinct()
    assertTrue("Intonation should vary across clauses to avoid monotone", distinctPitches.size >= 2)

    val joined = formatStepForNaturalSpeech(0, 3, "Panaskan 2 sdm minyak lalu tumis 300g ayam.")
    assertTrue(joined.contains("2 sendok makan minyak"))
    assertTrue(joined.contains("300 gram ayam"))
    assertTrue(joined.lowercase().contains("terus"))
    assertTrue(joined.lowercase().contains("nah,"))

    val lastStep = formatStepForNaturalSpeech(2, 3, "Panggang suhu 180°C.")
    assertTrue(lastStep.lowercase().contains("langkah terakhir"))
    assertTrue(lastStep.contains("180 derajat celcius"))
  }

  @Test
  fun scaledForServings_multipliesQuantitiesAndDeductAmountCleanly() {
    val ayamTumis = StarterCatalog.matcherStarterRecipes.first { it.id == "ayam_tumis_kecap" }
    val scaled = ayamTumis.scaledForServings(4) // doubled from 2 servings
    assertEquals(4, scaled.servings)
    assertEquals(600, scaled.ingredients.first().deductAmount)
    assertTrue(scaled.ingredients.first().amountLabel.contains("600"))
    val nutrition = scaled.resolvedNutrition()
    assertTrue(nutrition.caloriesKcal > 0)
    assertTrue(nutrition.proteinGrams > 0)
  }

  @Test
  fun parseCookingVoiceCommand_recognizesHandsFreeKeywords() {
    assertEquals(CookingVoiceCommand.NEXT_STEP, parseCookingVoiceCommand("lanjut ke langkah berikutnya"))
    assertEquals(CookingVoiceCommand.PREV_STEP, parseCookingVoiceCommand("kembali dulu"))
    assertEquals(CookingVoiceCommand.TOGGLE_TIMER, parseCookingVoiceCommand("mulai timer"))
    assertEquals(CookingVoiceCommand.RESET_TIMER, parseCookingVoiceCommand("ulang timer ya"))
    assertEquals(CookingVoiceCommand.REPLAY_VOICE, parseCookingVoiceCommand("ulangi instruksinya"))
  }

  @Test
  fun shareBuilders_formatShoppingAndRecipeCleanly() {
    val shoppingText =
      buildShoppingShareText(
        listOf(
          ShoppingItem(id = "1", name = "Santan 200ml", checked = false),
          ShoppingItem(id = "2", name = "Bawang Merah", checked = true),
        )
      )
    assertTrue(shoppingText.contains("Santan 200ml"))
    assertTrue(shoppingText.contains("Sudah Dibeli"))

    val ayamTumis = StarterCatalog.matcherStarterRecipes.first { it.id == "ayam_tumis_kecap" }
    val recipeText = buildRecipeShareText(ayamTumis)
    assertTrue(recipeText.contains("Ayam Tumis Kecap Bawang"))
    assertTrue(recipeText.contains("kkal/porsi"))
  }
}
