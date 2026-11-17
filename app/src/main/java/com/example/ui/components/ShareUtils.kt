package com.example.ui.components

import android.content.Context
import android.content.Intent
import com.example.model.Recipe
import com.example.model.ShoppingItem

fun buildShoppingShareText(items: List<ShoppingItem>): String {
  if (items.isEmpty()) return "Daftar Belanja Kitchen Solution masih kosong."
  val pending = items.filter { !it.checked }
  val bought = items.filter { it.checked }
  return buildString {
    appendLine("🛒 *Daftar Belanja Kitchen Solution*")
    appendLine()
    if (pending.isNotEmpty()) {
      appendLine("Perlu Dibeli (${pending.size}):")
      pending.forEach { appendLine("⬜ ${it.name}") }
    }
    if (bought.isNotEmpty()) {
      appendLine()
      appendLine("Sudah Dibeli (${bought.size}):")
      bought.forEach { appendLine("✅ ${it.name}") }
    }
  }.trim()
}

fun shareShoppingList(context: Context, items: List<ShoppingItem>) {
  val text = buildShoppingShareText(items)
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Daftar Belanja Kitchen Solution")
      putExtra(Intent.EXTRA_TEXT, text)
    }
  context.startActivity(Intent.createChooser(intent, "Bagikan Daftar Belanja via"))
}

fun buildRecipeShareText(recipe: Recipe): String {
  val nutrition = recipe.resolvedNutrition()
  return buildString {
    appendLine("🍳 *${recipe.title}*")
    appendLine(recipe.description)
    appendLine(
      "⏱ ${recipe.durationMinutes} mnt • 👥 ${recipe.servings} porsi • 🔥 ${nutrition.caloriesKcal} kkal/porsi"
    )
    appendLine()
    appendLine("*Bahan:*")
    recipe.ingredients.forEach { appendLine("• ${it.name}") }
    appendLine()
    appendLine("*Cara Membuat:*")
    recipe.steps.forEachIndexed { idx, step ->
      appendLine("${idx + 1}. ${step.instruction}")
    }
  }.trim()
}

fun shareRecipe(context: Context, recipe: Recipe) {
  val text = buildRecipeShareText(recipe)
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Resep: ${recipe.title}")
      putExtra(Intent.EXTRA_TEXT, text)
    }
  context.startActivity(Intent.createChooser(intent, "Bagikan Resep via"))
}
