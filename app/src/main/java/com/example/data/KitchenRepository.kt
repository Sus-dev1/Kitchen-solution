package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.JournalNote
import com.example.model.MealPlanEntry
import com.example.model.PantryDeduction
import com.example.model.PantryItem
import com.example.model.ReceiptScanItem
import com.example.model.Recipe
import com.example.model.SavedRecipe
import com.example.model.ShoppingItem
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KitchenRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireVerifiedUser(expectedUid: String): String {
    val uid =
      auth.currentUser?.uid
        ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    check(uid == expectedUid) { "Sesi akun berubah; menolak akses lintas pengguna." }
    return uid
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  private fun pantryCollection(uid: String) = userDoc(uid).collection("pantry_items")

  private fun shoppingCollection(uid: String) = userDoc(uid).collection("shopping_items")

  private fun journalCollection(uid: String) = userDoc(uid).collection("journal_notes")

  private fun savedRecipesCollection(uid: String) = userDoc(uid).collection("saved_recipes")

  private fun mealPlansCollection(uid: String) = userDoc(uid).collection("meal_plans")

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    userDoc(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.GET, "users/$userId")
        }
        throw error
      }

  fun observePantryItems(userId: String): Flow<List<PantryItem>> =
    pantryCollection(userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            PantryItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedWith(compareBy<PantryItem> { it.daysUntilExpiry }.thenBy { it.name })
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/pantry_items")
        }
        throw error
      }

  fun observeShoppingItems(userId: String): Flow<List<ShoppingItem>> =
    shoppingCollection(userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            ShoppingItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedBy { it.checked }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/shopping_items")
        }
        throw error
      }

  fun observeJournalNotes(userId: String): Flow<List<JournalNote>> =
    journalCollection(userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            JournalNote::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/journal_notes")
        }
        throw error
      }

  fun observeSavedRecipes(userId: String): Flow<List<SavedRecipe>> =
    savedRecipesCollection(userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            SavedRecipe::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/saved_recipes")
        }
        throw error
      }

  fun observeMealPlans(userId: String): Flow<List<MealPlanEntry>> =
    mealPlansCollection(userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          MealPlanEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/meal_plans")
        }
        throw error
      }

  suspend fun ensureInitialUserData(
    userId: String,
    displayName: String,
    email: String,
  ): Result<Unit> {
    val profilePath = "users/$userId"
    return try {
      val uid = requireVerifiedUser(userId)
      val snap = userDoc(uid).get().await()
      if (!snap.exists()) {
        val cleanName = displayName.trim().take(60).ifBlank { "Chef Alex" }
        val cleanEmail = email.trim().take(120)
        val batch = db.batch()
        batch.set(
          userDoc(uid),
          mapOf(
            "userId" to uid,
            "displayName" to cleanName,
            "email" to cleanEmail,
            "dietType" to "Halal / General",
            "allergies" to "Kacang Tanah",
            "defaultServings" to 2,
            "seededDefaults" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        val starterPantry =
          listOf(
            "dada_ayam" to
              mapOf(
                "name" to "Dada Ayam",
                "quantity" to 300,
                "unit" to "g",
                "stepDelta" to 50,
                "location" to "Kulkas (Fridge)",
                "daysUntilExpiry" to 2,
                "category" to "protein",
                "lowThreshold" to 100,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            "telur_ayam" to
              mapOf(
                "name" to "Telur Ayam",
                "quantity" to 4,
                "unit" to "pcs",
                "stepDelta" to 1,
                "location" to "Suhu Ruang",
                "daysUntilExpiry" to 12,
                "category" to "egg",
                "lowThreshold" to 2,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            "bawang_putih" to
              mapOf(
                "name" to "Bawang Putih",
                "quantity" to 5,
                "unit" to "siung",
                "stepDelta" to 1,
                "location" to "Suhu Ruang",
                "daysUntilExpiry" to 20,
                "category" to "spice",
                "lowThreshold" to 2,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            "kecap_manis" to
              mapOf(
                "name" to "Kecap Manis",
                "quantity" to 8,
                "unit" to "sdm",
                "stepDelta" to 1,
                "location" to "Rak Dapur",
                "daysUntilExpiry" to 90,
                "category" to "sauce",
                "lowThreshold" to 2,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            "mentega" to
              mapOf(
                "name" to "Mentega",
                "quantity" to 4,
                "unit" to "sdm",
                "stepDelta" to 1,
                "location" to "Kulkas (Fridge)",
                "daysUntilExpiry" to 30,
                "category" to "dairy",
                "lowThreshold" to 1,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
          )

        starterPantry.forEach { (docId, payload) ->
          batch.set(pantryCollection(uid).document(docId), payload)
        }

        batch.set(
          shoppingCollection(uid).document("starter_daun_bawang"),
          mapOf(
            "name" to "Daun Bawang (1 batang)",
            "checked" to false,
            "sourceTag" to "Auto AI",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        batch.set(
          journalCollection(uid).document("starter_ayam_kecap"),
          mapOf(
            "recipeTitle" to "Ayam Kecap Mentega Racikan V1",
            "note" to "Tambahkan ekstra 1/2 sdt merica hitam saat menumis bawang putih.",
            "rating" to 4.8,
            "dateLabel" to "14 Nov 2026",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, profilePath)
      Result.failure(e)
    }
  }

  suspend fun updateProfilePreferences(
    userId: String,
    displayName: String,
    dietType: String,
    allergies: String,
    defaultServings: Int,
  ): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireVerifiedUser(userId)
      val cleanName = displayName.trim().take(60).ifBlank { "Chef Alex" }
      val cleanDiet = dietType.trim().take(40).ifBlank { "Halal / General" }
      val cleanAllergies = allergies.trim().take(120)
      val clampedServings = defaultServings.coerceIn(1, 20)

      userDoc(uid)
        .update(
          mapOf(
            "displayName" to cleanName,
            "dietType" to cleanDiet,
            "allergies" to cleanAllergies,
            "defaultServings" to clampedServings,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adjustPantryQuantity(userId: String, item: PantryItem, delta: Int): Result<Unit> {
    val path = "users/$userId/pantry_items/${item.id}"
    return try {
      val uid = requireVerifiedUser(userId)
      require(item.id.isNotBlank()) { "ID bahan tidak boleh kosong." }
      val newQty = (item.quantity + delta).coerceIn(0, 100000)
      pantryCollection(uid)
        .document(item.id)
        .update(
          mapOf(
            "quantity" to newQty,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun addOrMergePantryItems(
    userId: String,
    newItems: List<ReceiptScanItem>,
    existingItems: List<PantryItem>,
  ): Result<Unit> {
    val path = "users/$userId/pantry_items"
    return try {
      val uid = requireVerifiedUser(userId)
      if (newItems.isEmpty()) return Result.success(Unit)

      val batch = db.batch()
      newItems.forEach { entry ->
        val cleanName = entry.name.trim().take(60)
        if (cleanName.isBlank()) return@forEach
        val cleanQty = entry.quantity.coerceIn(1, 100000)
        val cleanUnit = entry.unit.trim().take(20).ifBlank { "pcs" }
        val cleanLocation = entry.location.trim().take(40).ifBlank { "Rak Dapur" }
        val cleanDays = entry.daysUntilExpiry.coerceIn(0, 3650)
        val cleanCategory = entry.category.trim().take(30).ifBlank { "pantry" }
        val step =
          when {
            cleanUnit.equals("g", ignoreCase = true) || cleanUnit.equals("ml", ignoreCase = true) ->
              50
            else -> 1
          }
        val lowThresh = if (step == 50) 100 else 1

        val match = existingItems.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        if (match != null && match.id.isNotBlank()) {
          val updatedQty = (match.quantity + cleanQty).coerceIn(0, 100000)
          batch.update(
            pantryCollection(uid).document(match.id),
            mapOf(
              "quantity" to updatedQty,
              "daysUntilExpiry" to cleanDays,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          val docRef = pantryCollection(uid).document()
          batch.set(
            docRef,
            mapOf(
              "name" to cleanName,
              "quantity" to cleanQty,
              "unit" to cleanUnit,
              "stepDelta" to step,
              "location" to cleanLocation,
              "daysUntilExpiry" to cleanDays,
              "category" to cleanCategory,
              "lowThreshold" to lowThresh,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun deductIngredients(
    userId: String,
    deductions: List<PantryDeduction>,
    currentPantry: List<PantryItem>,
  ): Result<Unit> {
    val path = "users/$userId/pantry_items"
    return try {
      val uid = requireVerifiedUser(userId)
      if (deductions.isEmpty()) return Result.success(Unit)

      val batch = db.batch()
      var hasWrites = false
      deductions.forEach { deduction ->
        val target =
          currentPantry.firstOrNull { item ->
            item.name.contains(deduction.matchKeyword, ignoreCase = true) ||
              deduction.ingredientName.contains(item.name, ignoreCase = true)
          }
        if (target != null && target.id.isNotBlank()) {
          val updatedQty = (target.quantity - deduction.deductAmount).coerceIn(0, 100000)
          batch.update(
            pantryCollection(uid).document(target.id),
            mapOf(
              "quantity" to updatedQty,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          hasWrites = true
        }
      }
      if (hasWrites) {
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deletePantryItem(userId: String, itemId: String): Result<Unit> {
    val path = "users/$userId/pantry_items/$itemId"
    return try {
      val uid = requireVerifiedUser(userId)
      require(itemId.isNotBlank())
      pantryCollection(uid).document(itemId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun addShoppingItem(
    userId: String,
    name: String,
    sourceTag: String = "Manual",
  ): Result<Unit> {
    val path = "users/$userId/shopping_items"
    return try {
      val uid = requireVerifiedUser(userId)
      val cleanName = name.trim().take(80)
      require(cleanName.isNotBlank()) { "Nama belanjaan tidak boleh kosong." }
      val cleanTag = sourceTag.trim().take(20).ifBlank { "Manual" }

      shoppingCollection(uid)
        .document()
        .set(
          mapOf(
            "name" to cleanName,
            "checked" to false,
            "sourceTag" to cleanTag,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun toggleShoppingItem(userId: String, itemId: String, checked: Boolean): Result<Unit> {
    val path = "users/$userId/shopping_items/$itemId"
    return try {
      val uid = requireVerifiedUser(userId)
      require(itemId.isNotBlank())
      shoppingCollection(uid)
        .document(itemId)
        .update(
          mapOf(
            "checked" to checked,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteShoppingItem(userId: String, itemId: String): Result<Unit> {
    val path = "users/$userId/shopping_items/$itemId"
    return try {
      val uid = requireVerifiedUser(userId)
      require(itemId.isNotBlank())
      shoppingCollection(uid).document(itemId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun addJournalNote(
    userId: String,
    recipeTitle: String,
    note: String,
    rating: Double,
    dateLabel: String,
  ): Result<Unit> {
    val path = "users/$userId/journal_notes"
    return try {
      val uid = requireVerifiedUser(userId)
      val cleanTitle = recipeTitle.trim().take(80)
      val cleanNote = note.trim().take(500)
      val cleanDate = dateLabel.trim().take(30).ifBlank { "Hari ini" }
      require(cleanTitle.isNotBlank() && cleanNote.isNotBlank()) {
        "Judul resep dan catatan tidak boleh kosong."
      }
      val clampedRating = rating.coerceIn(1.0, 5.0)

      journalCollection(uid)
        .document()
        .set(
          mapOf(
            "recipeTitle" to cleanTitle,
            "note" to cleanNote,
            "rating" to clampedRating,
            "dateLabel" to cleanDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteJournalNote(userId: String, noteId: String): Result<Unit> {
    val path = "users/$userId/journal_notes/$noteId"
    return try {
      val uid = requireVerifiedUser(userId)
      require(noteId.isNotBlank())
      journalCollection(uid).document(noteId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun saveRecipe(
    userId: String,
    recipe: Recipe,
    isCustom: Boolean = recipe.isCustom,
  ): Result<Unit> {
    val docId = SavedRecipe.buildDocId(recipe.copy(isCustom = isCustom))
    val path = "users/$userId/saved_recipes/$docId"
    return try {
      val uid = requireVerifiedUser(userId)
      val nutrition = recipe.resolvedNutrition()
      val baseFields =
        mapOf(
          "title" to recipe.title.trim().take(100).ifBlank { "Resep Dapur" },
          "description" to recipe.description.trim().take(400),
          "durationMinutes" to recipe.durationMinutes.coerceIn(1, 360),
          "servings" to recipe.servings.coerceIn(1, 30),
          "difficulty" to recipe.difficulty.trim().take(30).ifBlank { "Mudah" },
          "categoryName" to recipe.category.name.take(30),
          "isCustom" to isCustom,
          "caloriesKcal" to nutrition.caloriesKcal.coerceIn(0, 5000),
          "proteinGrams" to nutrition.proteinGrams.coerceIn(0, 500),
          "carbsGrams" to nutrition.carbsGrams.coerceIn(0, 500),
          "fatGrams" to nutrition.fatGrams.coerceIn(0, 500),
          "ingredientsSerialized" to SavedRecipe.serializeIngredients(recipe.ingredients),
          "stepsSerialized" to SavedRecipe.serializeSteps(recipe.steps),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      val docRef = savedRecipesCollection(uid).document(docId)
      val exists = runCatching { docRef.get().await().exists() }.getOrDefault(false)
      if (exists) {
        docRef.update(baseFields).await()
      } else {
        docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteSavedRecipe(userId: String, recipeDocId: String): Result<Unit> {
    val path = "users/$userId/saved_recipes/$recipeDocId"
    return try {
      val uid = requireVerifiedUser(userId)
      require(recipeDocId.isNotBlank())
      savedRecipesCollection(uid).document(recipeDocId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun saveMealPlan(
    userId: String,
    dayOfWeek: String,
    mealSlot: String,
    recipe: Recipe,
    missingSummary: String,
  ): Result<Unit> {
    val docId = MealPlanEntry.docId(dayOfWeek, mealSlot)
    val path = "users/$userId/meal_plans/$docId"
    return try {
      val uid = requireVerifiedUser(userId)
      val baseFields =
        mapOf(
          "dayOfWeek" to dayOfWeek.trim().take(20),
          "mealSlot" to mealSlot.trim().take(30),
          "recipeId" to recipe.id.take(128).ifBlank { docId },
          "recipeTitle" to recipe.title.trim().take(100).ifBlank { "Menu Masakan" },
          "servings" to recipe.servings.coerceIn(1, 30),
          "missingItemsSummary" to missingSummary.trim().take(400),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      val docRef = mealPlansCollection(uid).document(docId)
      val exists = runCatching { docRef.get().await().exists() }.getOrDefault(false)
      if (exists) {
        docRef.update(baseFields).await()
      } else {
        docRef.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteMealPlan(userId: String, planId: String): Result<Unit> {
    val path = "users/$userId/meal_plans/$planId"
    return try {
      val uid = requireVerifiedUser(userId)
      require(planId.isNotBlank())
      mealPlansCollection(uid).document(planId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun addBatchShoppingItems(
    userId: String,
    itemNames: List<String>,
    existingItems: List<ShoppingItem>,
    sourceTag: String = "Meal Plan",
  ): Result<Int> {
    val path = "users/$userId/shopping_items"
    return try {
      val uid = requireVerifiedUser(userId)
      val cleanTag = sourceTag.trim().take(20).ifBlank { "Meal Plan" }
      val uniqueToAdd =
        itemNames
          .map { it.trim().take(80) }
          .filter { it.isNotBlank() }
          .distinctBy { it.lowercase() }
          .filter { candidate ->
            existingItems.none { existing -> existing.name.equals(candidate, ignoreCase = true) }
          }
      if (uniqueToAdd.isEmpty()) return Result.success(0)

      val batch = db.batch()
      uniqueToAdd.forEach { cleanName ->
        batch.set(
          shoppingCollection(uid).document(),
          mapOf(
            "name" to cleanName,
            "checked" to false,
            "sourceTag" to cleanTag,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(uniqueToAdd.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }
}
