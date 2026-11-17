package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.ExplorerCategory
import com.example.model.PantryItem
import com.example.model.ReceiptScanItem
import com.example.model.Recipe
import com.example.model.RecipeIngredient
import com.example.model.RecipeNutrition
import com.example.model.RecipeStep
import com.example.model.SubstitutionAnalysis
import com.example.model.SubstitutionOption
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "KitchenAiService"
private const val PRIMARY_MODEL = "gemini-3.8-flash"
private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
private const val AI_TIMEOUT_MS = 45_000L

class KitchenAiService {

  private val ai by lazy { Firebase.ai(backend = GenerativeBackend.googleAI()) }

  private val recipeSchema =
    Schema.obj(
      properties =
        mapOf(
          "recipes" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "title" to Schema.string("Indonesian recipe title"),
                      "description" to Schema.string("Short appetizing description in Indonesian"),
                      "durationMinutes" to Schema.integer("Estimated cook time in minutes"),
                      "servings" to Schema.integer("Portions count"),
                      "difficulty" to Schema.string("Sangat Mudah, Mudah, or Sedang"),
                      "category" to
                        Schema.enumeration(
                          values = listOf("NUSANTARA", "ASIAN", "WESTERN", "QUICK", "FRUGAL", "HEALTHY"),
                          description = "Best matching category for this dish",
                        ),
                      "substitutionTip" to
                        Schema.string(
                          "Helpful substitution or cooking tip in Indonesian",
                          nullable = true,
                        ),
                      "missingItemToShop" to
                        Schema.string(
                          "Missing non-staple ingredient name if any, else null",
                          nullable = true,
                        ),
                      "caloriesKcal" to Schema.integer("Estimated calories per serving (kcal)"),
                      "proteinGrams" to Schema.integer("Estimated protein per serving (g)"),
                      "carbsGrams" to Schema.integer("Estimated carbohydrates per serving (g)"),
                      "fatGrams" to Schema.integer("Estimated fat per serving (g)"),
                      "ingredients" to
                        Schema.array(
                          items =
                            Schema.obj(
                              properties =
                                mapOf(
                                  "name" to Schema.string("Ingredient display name with quantity"),
                                  "amountLabel" to Schema.string("Quantity label e.g. 200 g"),
                                  "pantryMatchKeyword" to
                                    Schema.string("Exact core ingredient name matching user pantry"),
                                  "deductAmount" to
                                    Schema.integer("Numeric quantity matching user pantry unit"),
                                  "isBasicStaple" to
                                    Schema.boolean(
                                      "True only for universal kitchen staples like Air, Garam, Gula, Lada, Merica, Minyak Goreng"
                                    ),
                                )
                            )
                        ),
                      "steps" to
                        Schema.array(
                          items =
                            Schema.obj(
                              properties =
                                mapOf(
                                  "instruction" to Schema.string("Clear Indonesian cooking step"),
                                  "durationSeconds" to Schema.integer("Step timer in seconds"),
                                )
                            )
                        ),
                    ),
                  optionalProperties =
                    listOf(
                      "substitutionTip",
                      "missingItemToShop",
                      "caloriesKcal",
                      "proteinGrams",
                      "carbsGrams",
                      "fatGrams",
                    ),
                )
            )
        )
    )

  private val substitutionSchema =
    Schema.obj(
      properties =
        mapOf(
          "summary" to
            Schema.string(
              "Concise 1-2 sentence Indonesian culinary guidance explaining the best substitution strategy"
            ),
          "options" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "substituteName" to Schema.string("Replacement ingredient name in Indonesian"),
                      "conversionRatio" to
                        Schema.string("Exact measurement conversion ratio e.g. 1 sdm -> 1 sdm"),
                      "matchScore" to
                        Schema.integer("Accuracy and culinary similarity score from 50 to 100"),
                      "inPantry" to
                        Schema.boolean("True if the user already has this substitute in Smart Pantry"),
                      "flavorImpact" to
                        Schema.string("Short explanation of taste, aroma, or texture difference"),
                    )
                )
            ),
        )
    )

  private val receiptSchema =
    Schema.obj(
      properties =
        mapOf(
          "items" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "name" to Schema.string("Clean ingredient name in Indonesian"),
                      "quantity" to Schema.integer("Numeric quantity"),
                      "unit" to Schema.string("Unit such as g, kg, pcs, siung, L, sdm"),
                      "location" to
                        Schema.string("Kulkas (Fridge), Freezer, Suhu Ruang, or Rak Dapur"),
                      "daysUntilExpiry" to Schema.integer("Estimated days before expiry"),
                      "category" to
                        Schema.string("protein, egg, spice, dairy, sauce, grain, vegetable, or pantry"),
                    )
                )
            )
        )
    )

  private fun buildRecipeModel(modelName: String): GenerativeModel =
    ai.generativeModel(
      modelName = modelName,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = recipeSchema
        },
    )

  private fun buildSubstitutionModel(modelName: String): GenerativeModel =
    ai.generativeModel(
      modelName = modelName,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = substitutionSchema
        },
    )

  private fun buildReceiptModel(modelName: String): GenerativeModel =
    ai.generativeModel(
      modelName = modelName,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = receiptSchema
        },
    )

  private fun buildChatModel(modelName: String): GenerativeModel =
    ai.generativeModel(
      modelName = modelName,
      systemInstruction =
        content {
          text(
            "Kamu adalah Chef AI Kitchen Solution, asisten kuliner profesional, ahli gizi, dan pengetahuan bahan makanan. " +
              "Jawab dalam bahasa Indonesia yang ramah, terstruktur, praktis, dan akurat. " +
              "Bantu pengguna mengenai ide masakan dari seluruh dunia, teknik memasak, penyimpanan bahan makanan, nutrisi/kalori bahan, " +
              "keamanan pangan, dan solusi pengganti bahan dapur."
          )
        },
    )

  private val recipeModel by lazy { buildRecipeModel(PRIMARY_MODEL) }
  private val recipeFallbackModel by lazy { buildRecipeModel(FALLBACK_MODEL) }

  private val substitutionModel by lazy { buildSubstitutionModel(PRIMARY_MODEL) }
  private val substitutionFallbackModel by lazy { buildSubstitutionModel(FALLBACK_MODEL) }

  private val receiptModel by lazy { buildReceiptModel(PRIMARY_MODEL) }
  private val receiptFallbackModel by lazy { buildReceiptModel(FALLBACK_MODEL) }

  private val chatModel by lazy { buildChatModel(PRIMARY_MODEL) }
  private val chatFallbackModel by lazy { buildChatModel(FALLBACK_MODEL) }

  private suspend fun generateTextWithFallback(
    primary: GenerativeModel,
    fallback: GenerativeModel,
    prompt: String,
  ): GenerateContentResponse {
    return try {
      withTimeout(AI_TIMEOUT_MS) { primary.generateContent(prompt) }
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "Primary model ($PRIMARY_MODEL) timed out, retrying with $FALLBACK_MODEL", e)
      withTimeout(AI_TIMEOUT_MS) { fallback.generateContent(prompt) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Primary model ($PRIMARY_MODEL) failed (${e.message}), retrying with $FALLBACK_MODEL", e)
      withTimeout(AI_TIMEOUT_MS) { fallback.generateContent(prompt) }
    }
  }

  private suspend fun generateContentWithFallback(
    primary: GenerativeModel,
    fallback: GenerativeModel,
    promptContent: Content,
  ): GenerateContentResponse {
    return try {
      withTimeout(AI_TIMEOUT_MS) { primary.generateContent(promptContent) }
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "Primary multimodal model timed out, retrying with $FALLBACK_MODEL", e)
      withTimeout(AI_TIMEOUT_MS) { fallback.generateContent(promptContent) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Primary multimodal model failed (${e.message}), retrying with $FALLBACK_MODEL", e)
      withTimeout(AI_TIMEOUT_MS) { fallback.generateContent(promptContent) }
    }
  }

  suspend fun generateMatchedRecipes(
    pantryItems: List<PantryItem>,
    profile: UserProfile?,
  ): Result<List<Recipe>> {
    return try {
      val activeItems = pantryItems.filter { it.quantity > 0 }
      if (activeItems.isEmpty()) {
        return Result.failure(
          Exception("Stok Smart Pantry masih kosong. Tambahkan bahan dulu untuk mendapat menu 100% cocok.")
        )
      }

      val stockSummary =
        activeItems.joinToString("\n") {
          "- ${it.name} (qty: ${it.quantity} ${it.unit}, kedaluwarsa H-${it.daysUntilExpiry})"
        }
      val allowedNames = activeItems.joinToString(", ") { it.name }

      val prompt =
        """
        Kamu adalah asisten koki AI Indonesia untuk aplikasi Kitchen Solution.
        Stok bahan yang TERSEDIA di Smart Pantry pengguna saat ini:
        $stockSummary

        Daftar nama bahan yang tersedia: $allowedNames

        Preferensi pengguna:
        - Diet: ${profile?.dietType ?: "Halal / General"}
        - Alergi (WAJIB HINDARI): ${profile?.allergies ?: "Tidak ada"}
        - Porsi default: ${profile?.defaultServings ?: 2} orang

        ATURAN KETAT 100% MATCH:
        1. Buat 4 resep masakan berbeda dan kreatif (variasikan gaya Nusantara, Asia, Western, dan Cepat) yang BISA LANGSUNG DIMASAK 100% menggunakan bahan dari stok di atas.
        2. JANGAN menyertakan bahan utama atau bumbu khusus apa pun yang TIDAK ada di daftar stok pengguna! Hanya bahan dari daftar [$allowedNames] plus bumbu dapur universal (Air, Garam, Gula, Lada/Merica, Minyak Goreng) yang boleh dipakai.
        3. Untuk setiap bahan dari stok pengguna, isi "pantryMatchKeyword" PERSIS seperti nama bahan di stok pengguna ($allowedNames) dan set "isBasicStaple": false.
        4. Untuk bumbu dapur universal (Air, Garam, Gula, Lada, Merica, Minyak Goreng), set "isBasicStaple": true dan "deductAmount": 0.
        5. Prioritaskan bahan dengan H-kedaluwarsa paling kecil agar tidak terbuang.
        6. Set "missingItemToShop": null karena semua resep harus 100% siap masak.
        7. Setiap resep memiliki 3-4 langkah memasak yang jelas, lengkap dengan durasi detik realistis.
        """
          .trimIndent()

      val response = generateTextWithFallback(recipeModel, recipeFallbackModel, prompt)
      val parsed =
        parseRecipesJson(
          jsonText = response.text,
          isAiGenerated = true,
          defaultServings = profile?.defaultServings ?: 2,
        )
      Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI matched recipe generation timed out", e)
      Result.failure(Exception("Koneksi ke server AI melebihi batas waktu. Periksa koneksi internet Anda."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate AI matched recipes", e)
      Result.failure(Exception(formatAiError("Gagal membuat rekomendasi menu AI", e), e))
    }
  }

  suspend fun searchWorldRecipes(
    query: String,
    category: ExplorerCategory,
    profile: UserProfile?,
  ): Result<List<Recipe>> {
    return try {
      val cleanQuery = query.trim()
      val categoryHint =
        if (category == ExplorerCategory.ALL) "Beragam masakan populer Nusantara, Asia, Western, Timur Tengah, dan Internasional"
        else "Kategori ${category.label} (gunakan nilai category=${category.name})"

      val topicLine =
        if (cleanQuery.isNotBlank()) "Pencarian khusus pengguna: \"$cleanQuery\"."
        else "Berikan resep-resep populer terbaik dari berbagai tradisi kuliner internet dan dunia."

      val prompt =
        """
        Kamu adalah kurator resep global untuk aplikasi Kitchen Solution (Explorer Resep Dunia).
        $topicLine
        Fokus kategori: $categoryHint.
        Preferensi pengguna:
        - Diet: ${profile?.dietType ?: "Halal / General"}
        - Alergi (WAJIB HINDARI): ${profile?.allergies ?: "Tidak ada"}
        - Porsi default: ${profile?.defaultServings ?: 2} orang

        Buat 4 resep masakan autentik, lengkap, dan populer yang sesuai dengan permintaan di atas dalam Bahasa Indonesia.
        Beri variasi hidangan yang menarik dan jangan mengulang resep yang sama.
        Isi semua bahan dengan takaran jelas, "pantryMatchKeyword" sederhana (contoh: "Dada Ayam", "Bawang Putih", "Telur Ayam", "Santan", "Pasta"), dan "isBasicStaple": true untuk Air/Garam/Gula/Lada/Minyak Goreng.
        Sertakan 3-5 langkah memasak yang jelas dan informatif.
        """
          .trimIndent()

      val response = generateTextWithFallback(recipeModel, recipeFallbackModel, prompt)
      val forcedCategory = if (category != ExplorerCategory.ALL) category else null
      val parsed =
        parseRecipesJson(
          jsonText = response.text,
          isAiGenerated = true,
          defaultServings = profile?.defaultServings ?: 2,
          overrideCategory = forcedCategory,
        )
      Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI world recipe search timed out", e)
      Result.failure(Exception("Pencarian resep AI melebihi batas waktu. Periksa koneksi internet Anda."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to search world recipes", e)
      Result.failure(Exception(formatAiError("Gagal mencari resep di Explorer AI", e), e))
    }
  }

  suspend fun suggestSubstitution(
    missingIngredient: String,
    recipeContext: String,
    pantryItems: List<PantryItem>,
  ): Result<SubstitutionAnalysis> {
    return try {
      val activePantry = pantryItems.filter { it.quantity > 0 }
      val stockList =
        activePantry
          .joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }
          .ifBlank { "Tidak ada stok di Smart Pantry" }
      val contextLine =
        if (recipeContext.isNotBlank()) "Untuk hidangan/resep: $recipeContext."
        else "Konteks masakan umum rumahan."

      val prompt =
        """
        Kamu adalah ahli kuliner dan substitusi bahan makanan Indonesia.
        Pengguna kekurangan / ingin mengganti bahan: "$missingIngredient".
        $contextLine
        Stok Smart Pantry pengguna saat ini: [$stockList].

        Berikan 3 alternatif bahan substitusi paling AKURAT dan realistis dalam kuliner Indonesia/global:
        1. Prioritaskan bahan yang memang tersedia di Smart Pantry pengguna jika cocok secara rasa/fungsi, dan set "inPantry": true hanya jika benar-benar ada dalam daftar [$stockList].
        2. Berikan "conversionRatio" yang presisi (contoh: "1 sdm santan kental = 1 sdm susu evaporasi + 1/4 sdt mentega").
        3. Berikan "matchScore" (50-100) yang jujur berdasarkan kemiripan fungsi kimiawi/kuliner (pengental, asam, umami, aroma, lemak).
        4. Jelaskan "flavorImpact" secara spesifik dan jujur.
        """
          .trimIndent()

      val response = generateTextWithFallback(substitutionModel, substitutionFallbackModel, prompt)
      val jsonText = response.text ?: throw IllegalStateException("AI tidak mengembalikan analisis substitusi.")
      val root = JSONObject(jsonText)
      val summary = root.optString("summary", "Berikut rekomendasi bahan pengganti yang paling mendekati.")
      val optionsArr = root.getJSONArray("options")
      val options = mutableListOf<SubstitutionOption>()
      for (i in 0 until optionsArr.length()) {
        val opt = optionsArr.getJSONObject(i)
        val subName = opt.optString("substituteName", "").trim()
        if (subName.isBlank()) continue
        val pantryMatched =
          activePantry.any {
            it.name.contains(subName, ignoreCase = true) || subName.contains(it.name, ignoreCase = true)
          }
        options.add(
          SubstitutionOption(
            substituteName = subName,
            conversionRatio = opt.optString("conversionRatio", "1 : 1"),
            matchScore = opt.optInt("matchScore", 75).coerceIn(10, 100),
            inPantry = pantryMatched,
            flavorImpact = opt.optString("flavorImpact", "Karakter rasa mendekati bahan asli."),
          )
        )
      }
      if (options.isEmpty()) throw IllegalStateException("Tidak ada opsi substitusi yang ditemukan.")
      Result.success(
        SubstitutionAnalysis(
          originalIngredient = missingIngredient,
          recipeContext = recipeContext,
          summary = summary,
          options = options.sortedByDescending { (if (it.inPantry) 15 else 0) + it.matchScore },
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI substitution timed out", e)
      Result.failure(Exception("Permintaan substitusi AI melebihi batas waktu. Periksa koneksi internet Anda."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to suggest substitution", e)
      Result.failure(Exception(formatAiError("Gagal mencari substitusi AI", e), e))
    }
  }

  fun sendChatMessageStream(
    history: List<ChatMessage>,
    userMessage: String,
    pantryItems: List<PantryItem>,
    profile: UserProfile?,
  ): Flow<String> = flow {
    val activeStock =
      pantryItems
        .filter { it.quantity > 0 }
        .joinToString(", ") { "${it.name} (${it.quantity} ${it.unit}, H-${it.daysUntilExpiry})" }
        .ifBlank { "Belum ada bahan tercatat di Smart Pantry" }

    val contextIntro =
      "Info Dapur Pengguna -> Nama: ${profile?.displayName ?: "Chef"}, Diet: ${profile?.dietType ?: "Halal / General"}, " +
        "Alergi: ${profile?.allergies ?: "Tidak ada"}, Porsi: ${profile?.defaultServings ?: 2} orang. " +
        "Stok Smart Pantry saat ini: [$activeStock]."

    val sdkHistory =
      mutableListOf(
        content("user") { text(contextIntro) },
        content("model") {
          text(
            "Siap! Saya Chef AI yang sudah memahami profil diet, alergi, dan stok Smart Pantry Anda. " +
              "Silakan tanya apa saja seputar masakan, resep, atau informasi bahan makanan."
          )
        },
      )

    // Ensure strict alternating user -> model turns so failed turns never cause HTTP 400
    var expectedRole = "user"
    history
      .filter { !it.isStreaming && it.text.isNotBlank() }
      .takeLast(12)
      .forEach { msg ->
        val role = if (msg.isFromUser) "user" else "model"
        if (role == expectedRole) {
          sdkHistory.add(content(role) { text(msg.text) })
          expectedRole = if (role == "user") "model" else "user"
        }
      }
    if (expectedRole == "model" && sdkHistory.size > 2) {
      // Drop trailing unpaired user turn before sending the new userMessage
      sdkHistory.removeAt(sdkHistory.lastIndex)
    }

    var emittedAny = false
    try {
      withTimeout(AI_TIMEOUT_MS) {
        val chat = chatModel.startChat(history = sdkHistory)
        chat.sendMessageStream(userMessage).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            emittedAny = true
            emit(part)
          }
        }
      }
    } catch (e: TimeoutCancellationException) {
      if (emittedAny) throw e
      Log.w(TAG, "Primary chat model timed out, retrying with $FALLBACK_MODEL", e)
      withTimeout(AI_TIMEOUT_MS) {
        val fallbackChat = chatFallbackModel.startChat(history = sdkHistory)
        fallbackChat.sendMessageStream(userMessage).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            emit(part)
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (emittedAny) throw e
      Log.w(TAG, "Primary chat model failed (${e.message}), retrying with $FALLBACK_MODEL", e)
      withTimeout(AI_TIMEOUT_MS) {
        val fallbackChat = chatFallbackModel.startChat(history = sdkHistory)
        fallbackChat.sendMessageStream(userMessage).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            emit(part)
          }
        }
      }
    }
  }

  suspend fun scanReceiptImage(bitmap: Bitmap): Result<List<ReceiptScanItem>> {
    return try {
      val promptContent = content {
        image(bitmap)
        text(
          "Ekstrak daftar bahan makanan/dapur dari foto struk belanjaan atau bahan makanan ini dalam bahasa Indonesia. " +
            "Sertakan jumlah perkiraan, satuan (g/kg/pcs/L/siung/sdm), lokasi simpan, dan estimasi hari kedaluwarsa."
        )
      }
      val response = generateContentWithFallback(receiptModel, receiptFallbackModel, promptContent)
      parseReceiptJson(response.text)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Receipt image scan timed out", e)
      Result.failure(Exception("Pemindaian foto struk melebihi batas waktu."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Receipt image scan failed", e)
      Result.failure(Exception(formatAiError("Gagal memindai foto struk", e), e))
    }
  }

  suspend fun parseReceiptText(rawText: String): Result<List<ReceiptScanItem>> {
    return try {
      val response =
        generateTextWithFallback(
          receiptModel,
          receiptFallbackModel,
          "Ekstrak bahan makanan dari catatan/struk belanja berikut ke JSON terstruktur dalam bahasa Indonesia:\n$rawText",
        )
      parseReceiptJson(response.text)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Receipt text parse timed out", e)
      Result.failure(Exception("Analisis teks struk melebihi batas waktu."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Receipt text parse failed", e)
      Result.failure(Exception(formatAiError("Gagal menganalisis teks struk", e), e))
    }
  }

  private fun parseRecipesJson(
    jsonText: String?,
    isAiGenerated: Boolean,
    defaultServings: Int,
    overrideCategory: ExplorerCategory? = null,
  ): List<Recipe> {
    if (jsonText.isNullOrBlank()) throw IllegalStateException("AI tidak mengembalikan respons.")
    val root = JSONObject(jsonText)
    val recipesArr = root.getJSONArray("recipes")
    val parsed = mutableListOf<Recipe>()
    val now = System.currentTimeMillis()
    for (i in 0 until recipesArr.length()) {
      val obj = recipesArr.getJSONObject(i)
      val title = obj.optString("title", "").trim()
      if (title.isBlank()) continue
      val ingredientsArr = obj.optJSONArray("ingredients")
      val ingredients =
        if (ingredientsArr != null) {
          (0 until ingredientsArr.length()).map { idx ->
            val ing = ingredientsArr.getJSONObject(idx)
            val ingName = ing.optString("name", "Bahan")
            RecipeIngredient(
              name = ingName,
              amountLabel = ing.optString("amountLabel", ""),
              pantryMatchKeyword = ing.optString("pantryMatchKeyword", ingName),
              deductAmount = ing.optInt("deductAmount", 1).coerceAtLeast(0),
              isBasicStaple = ing.optBoolean("isBasicStaple", false),
            )
          }
        } else {
          emptyList()
        }
      val stepsArr = obj.optJSONArray("steps")
      val steps =
        if (stepsArr != null) {
          (0 until stepsArr.length()).mapNotNull { idx ->
            val step = stepsArr.getJSONObject(idx)
            val instruction = step.optString("instruction", "").trim()
            if (instruction.isBlank()) null
            else RecipeStep(
              instruction = instruction,
              durationSeconds = step.optInt("durationSeconds", 180).coerceIn(30, 1800),
            )
          }
        } else {
          emptyList()
        }
      val tip = obj.optString("substitutionTip").takeIf { it.isNotBlank() && it != "null" }
      val missing = obj.optString("missingItemToShop").takeIf { it.isNotBlank() && it != "null" }
      val catString = obj.optString("category", "NUSANTARA")
      val parsedCategory =
        ExplorerCategory.entries.firstOrNull { it.name.equals(catString, ignoreCase = true) }
          ?: ExplorerCategory.NUSANTARA

      val calories = obj.optInt("caloriesKcal", 0)
      val nutrition =
        if (calories > 0) {
          RecipeNutrition(
            caloriesKcal = calories.coerceIn(50, 3000),
            proteinGrams = obj.optInt("proteinGrams", 18).coerceIn(0, 300),
            carbsGrams = obj.optInt("carbsGrams", 25).coerceIn(0, 400),
            fatGrams = obj.optInt("fatGrams", 12).coerceIn(0, 300),
          )
        } else {
          null
        }

      parsed.add(
        Recipe(
          id = "ai_${now}_$i",
          title = title,
          description = obj.optString("description", "Resep spesial rekomendasi Chef AI."),
          durationMinutes = obj.optInt("durationMinutes", 20).coerceIn(5, 180),
          servings = obj.optInt("servings", defaultServings).coerceIn(1, 20),
          difficulty = obj.optString("difficulty", "Mudah"),
          category = overrideCategory ?: parsedCategory,
          isWebVerified = false,
          isAiGenerated = isAiGenerated,
          nutrition = nutrition,
          ingredients = ingredients,
          substitutionTip = tip,
          missingItemToShop = missing,
          steps = steps.ifEmpty { listOf(RecipeStep("Siapkan bahan dan masak hingga matang.", 180)) },
        )
      )
    }
    if (parsed.isEmpty()) throw IllegalStateException("Daftar resep AI kosong.")
    return parsed
  }

  private fun parseReceiptJson(jsonText: String?): Result<List<ReceiptScanItem>> {
    if (jsonText.isNullOrBlank()) {
      return Result.failure(Exception("AI tidak mendeteksi bahan dari struk."))
    }
    val root = JSONObject(jsonText)
    val itemsArr = root.getJSONArray("items")
    val items = mutableListOf<ReceiptScanItem>()
    for (i in 0 until itemsArr.length()) {
      val obj = itemsArr.getJSONObject(i)
      val name = obj.optString("name", "").trim()
      if (name.isBlank()) continue
      items.add(
        ReceiptScanItem(
          name = name,
          quantity = obj.optInt("quantity", 1).coerceAtLeast(1),
          unit = obj.optString("unit", "pcs"),
          location = obj.optString("location", "Rak Dapur"),
          daysUntilExpiry = obj.optInt("daysUntilExpiry", 14).coerceIn(1, 3650),
          category = obj.optString("category", "pantry"),
        )
      )
    }
    return if (items.isEmpty()) {
      Result.failure(Exception("Tidak ada item bahan yang terdeteksi."))
    } else {
      Result.success(items)
    }
  }

  private fun formatAiError(prefix: String, e: Exception): String {
    val raw = e.localizedMessage ?: e.message ?: e::class.java.simpleName
    val lower = raw.lowercase()
    return when {
      lower.contains("app check") || lower.contains("403") || lower.contains("attestation") ->
        "$prefix: Verifikasi App Check gagal ($raw). Pastikan aplikasi dibuka lewat preview/emulator resmi atau koneksi aman."
      lower.contains("unable to resolve host") || lower.contains("network") || lower.contains("connect") ->
        "$prefix: Koneksi internet bermasalah ($raw)."
      else -> "$prefix: $raw"
    }
  }
}
